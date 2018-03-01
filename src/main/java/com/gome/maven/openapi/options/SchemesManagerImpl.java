/*
 * Copyright 2000-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gome.maven.openapi.options;

import com.gome.maven.openapi.application.AccessToken;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.application.ex.DecodeDefaultsUtil;
import com.gome.maven.openapi.components.RoamingType;
import com.gome.maven.openapi.components.ServiceManager;
import com.gome.maven.openapi.components.impl.stores.DirectoryBasedStorage;
import com.gome.maven.openapi.components.impl.stores.DirectoryStorageData;
import com.gome.maven.openapi.components.impl.stores.StorageUtil;
import com.gome.maven.openapi.components.impl.stores.StreamProvider;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.editor.DocumentRunnable;
import com.gome.maven.openapi.extensions.AbstractExtensionPointBean;
import com.gome.maven.openapi.ui.Messages;
import com.gome.maven.openapi.util.Comparing;
import com.gome.maven.openapi.util.InvalidDataException;
import com.gome.maven.openapi.util.JDOMUtil;
import com.gome.maven.openapi.util.WriteExternalException;
import com.gome.maven.openapi.util.io.BufferExposingByteArrayOutputStream;
import com.gome.maven.openapi.util.io.FileUtil;
import com.gome.maven.openapi.util.text.StringUtilRt;
import com.gome.maven.openapi.vfs.LocalFileSystem;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.openapi.vfs.VirtualFileAdapter;
import com.gome.maven.openapi.vfs.VirtualFileEvent;
import com.gome.maven.openapi.vfs.tracker.VirtualFileTracker;
import com.gome.maven.util.SmartList;
import com.gome.maven.util.ThrowableConvertor;
import com.gome.maven.util.containers.ContainerUtilRt;
import com.gome.maven.util.io.URLUtil;
import com.gome.maven.util.lang.CompoundRuntimeException;
import com.gome.maven.util.text.UniqueNameGenerator;
import gnu.trove.THashSet;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Parent;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.*;

public class SchemesManagerImpl<T extends Scheme, E extends ExternalizableScheme> extends AbstractSchemesManager<T, E> {
    private static final Logger LOG = Logger.getInstance(SchemesManagerFactoryImpl.class);

    private final String myFileSpec;
    private final SchemeProcessor<E> myProcessor;
    private final RoamingType myRoamingType;

    private final StreamProvider myProvider;
    private final File myIoDir;
    private VirtualFile myDir;

    private String mySchemeExtension = DirectoryStorageData.DEFAULT_EXT;
    private boolean myUpdateExtension;

    private final Set<String> myFilesToDelete = new THashSet<String>();

    public SchemesManagerImpl( String fileSpec,
                               SchemeProcessor<E> processor,
                               RoamingType roamingType,
                               StreamProvider provider,
                               File baseDir) {
        myFileSpec = fileSpec;
        myProcessor = processor;
        myRoamingType = roamingType;
        myProvider = provider;
        myIoDir = baseDir;
        if (processor instanceof SchemeExtensionProvider) {
            mySchemeExtension = ((SchemeExtensionProvider)processor).getSchemeExtension();
            myUpdateExtension = ((SchemeExtensionProvider)processor).isUpgradeNeeded();
        }

        VirtualFileTracker virtualFileTracker = ServiceManager.getService(VirtualFileTracker.class);
        if (virtualFileTracker != null) {
            final String baseDirPath = myIoDir.getAbsolutePath().replace(File.separatorChar, '/');
            virtualFileTracker.addTracker(LocalFileSystem.PROTOCOL_PREFIX + baseDirPath, new VirtualFileAdapter() {
                @Override
                public void contentsChanged( VirtualFileEvent event) {
                    if (event.getRequestor() != null || !isMy(event)) {
                        return;
                    }

                    E scheme = findSchemeFor(event.getFile().getName());
                    T oldCurrentScheme = null;
                    if (scheme != null) {
                        oldCurrentScheme = getCurrentScheme();
                        //noinspection unchecked
                        removeScheme((T)scheme);
                        myProcessor.onSchemeDeleted(scheme);
                    }

                    E readScheme = readSchemeFromFile(event.getFile(), true, false);
                    if (readScheme != null) {
                        myProcessor.initScheme(readScheme);
                        myProcessor.onSchemeAdded(readScheme);

                        T newCurrentScheme = getCurrentScheme();
                        if (oldCurrentScheme != null && newCurrentScheme == null) {
                            setCurrentSchemeName(readScheme.getName());
                            newCurrentScheme = getCurrentScheme();
                        }

                        if (oldCurrentScheme != newCurrentScheme) {
                            myProcessor.onCurrentSchemeChanged(oldCurrentScheme);
                        }
                    }
                }

                @Override
                public void fileCreated( VirtualFileEvent event) {
                    if (event.getRequestor() == null) {
                        if (event.getFile().isDirectory()) {
                            VirtualFile dir = getVirtualDir();
                            if (event.getFile().equals(dir)) {
                                for (VirtualFile file : dir.getChildren()) {
                                    if (isMy(file)) {
                                        schemeCreatedExternally(file);
                                    }
                                }
                            }
                        }
                        else if (isMy(event)) {
                            schemeCreatedExternally(event.getFile());
                        }
                    }
                }

                private void schemeCreatedExternally( VirtualFile file) {
                    E readScheme = readSchemeFromFile(file, true, false);
                    if (readScheme != null) {
                        myProcessor.initScheme(readScheme);
                        myProcessor.onSchemeAdded(readScheme);
                    }
                }

                @Override
                public void fileDeleted( VirtualFileEvent event) {
                    if (event.getRequestor() == null) {
                        if (event.getFile().isDirectory()) {
                            VirtualFile dir = myDir;
                            if (event.getFile().equals(dir)) {
                                myDir = null;
                                for (VirtualFile file : dir.getChildren()) {
                                    if (isMy(file)) {
                                        schemeDeletedExternally(file);
                                    }
                                }
                            }
                        }
                        else if (isMy(event)) {
                            schemeDeletedExternally(event.getFile());
                        }
                    }
                }

                private void schemeDeletedExternally( VirtualFile file) {
                    E scheme = findSchemeFor(file.getName());
                    T oldCurrentScheme = null;
                    if (scheme != null) {
                        oldCurrentScheme = getCurrentScheme();
                        //noinspection unchecked
                        removeScheme((T)scheme);
                        myProcessor.onSchemeDeleted(scheme);
                    }

                    T newCurrentScheme = getCurrentScheme();
                    if (oldCurrentScheme != null && newCurrentScheme == null) {
                        if (!mySchemes.isEmpty()) {
                            setCurrentSchemeName(mySchemes.get(0).getName());
                            newCurrentScheme = getCurrentScheme();
                        }
                    }

                    if (oldCurrentScheme != newCurrentScheme) {
                        myProcessor.onCurrentSchemeChanged(oldCurrentScheme);
                    }
                }
            }, false, ApplicationManager.getApplication());
        }
    }

    public void loadBundledScheme( String resourceName,  Object requestor,  ThrowableConvertor<Element, T, Throwable> convertor) {
        try {
            URL url = requestor instanceof AbstractExtensionPointBean
                    ? (((AbstractExtensionPointBean)requestor).getLoaderForClass().getResource(resourceName))
                    : DecodeDefaultsUtil.getDefaults(requestor, resourceName);
            if (url == null) {
                // Error shouldn't occur during this operation thus we report error instead of info
                LOG.error("Cannot read scheme from " + resourceName);
                return;
            }
            addNewScheme(convertor.convert(JDOMUtil.load(URLUtil.openStream(url))), false);
        }
        catch (Throwable e) {
            LOG.error("Cannot read scheme from " + resourceName, e);
        }
    }

    private boolean isMy( VirtualFileEvent event) {
        return isMy(event.getFile());
    }

    private boolean isMy( VirtualFile file) {
        return StringUtilRt.endsWithIgnoreCase(file.getNameSequence(), mySchemeExtension);
    }

    @Override
    
    public Collection<E> loadSchemes() {
        Map<String, E> result = new LinkedHashMap<String, E>();
        if (myProvider != null && myProvider.isEnabled()) {
            readSchemesFromProviders(result);
        }
        else {
            VirtualFile dir = getVirtualDir();
            VirtualFile[] files = dir == null ? null : dir.getChildren();
            if (files != null) {
                for (VirtualFile file : files) {
                    E scheme = readSchemeFromFile(file, false, true);
                    if (scheme != null) {
                        result.put(scheme.getName(), scheme);
                    }
                }
            }
        }

        Collection<E> list = result.values();
        for (E scheme : list) {
            myProcessor.initScheme(scheme);
            checkCurrentScheme(scheme);
        }
        return list;
    }

    private E findSchemeFor( String ioFileName) {
        for (T scheme : mySchemes) {
            if (scheme instanceof ExternalizableScheme) {
                if (ioFileName.equals(((ExternalizableScheme)scheme).getExternalInfo().getCurrentFileName() + mySchemeExtension)) {
                    //noinspection CastConflictsWithInstanceof,unchecked
                    return (E)scheme;
                }
            }
        }
        return null;
    }

    
    private static Element loadElementOrNull( InputStream stream) {
        try {
            return JDOMUtil.load(stream);
        }
        catch (JDOMException e) {
            LOG.warn(e);
            return null;
        }
        catch (IOException e) {
            LOG.warn(e);
            return null;
        }
    }

    private void readSchemesFromProviders( Map<String, E> result) {
        assert myProvider != null;
        for (String subPath : myProvider.listSubFiles(myFileSpec, myRoamingType)) {
            try {
                Element element = loadElementOrNull(myProvider.loadContent(getFileFullPath(subPath), myRoamingType));
                if (element == null) {
                    return;
                }

                E scheme = readScheme(element, true);
                boolean fileRenamed = false;
                assert scheme != null;
                T existing = findSchemeByName(scheme.getName());
                if (existing instanceof ExternalizableScheme) {
                    String currentFileName = ((ExternalizableScheme)existing).getExternalInfo().getCurrentFileName();
                    if (currentFileName != null && !currentFileName.equals(subPath)) {
                        deleteServerFile(subPath);
                        subPath = currentFileName;
                        fileRenamed = true;
                    }
                }
                String fileName = checkFileNameIsFree(subPath, scheme.getName());
                if (!fileRenamed && !fileName.equals(subPath)) {
                    deleteServerFile(subPath);
                }

                loadScheme(scheme, false, fileName);
                scheme.getExternalInfo().markRemote();
                result.put(scheme.getName(), scheme);
            }
            catch (Exception e) {
                LOG.info("Cannot load data from stream provider: " + e.getMessage());
            }
        }
    }

    
    private String checkFileNameIsFree( String subPath,  String schemeName) {
        for (Scheme scheme : mySchemes) {
            if (scheme instanceof ExternalizableScheme) {
                String name = ((ExternalizableScheme)scheme).getExternalInfo().getCurrentFileName();
                if (name != null &&
                        !schemeName.equals(scheme.getName()) &&
                        subPath.length() == (name.length() + mySchemeExtension.length()) &&
                        subPath.startsWith(name) &&
                        subPath.endsWith(mySchemeExtension)) {
                    return UniqueNameGenerator.generateUniqueName(FileUtil.sanitizeName(schemeName), collectAllFileNames());
                }
            }
        }
        return subPath;
    }

    
    private Collection<String> collectAllFileNames() {
        Set<String> result = new THashSet<String>();
        for (T scheme : mySchemes) {
            if (scheme instanceof ExternalizableScheme) {
                ExternalInfo externalInfo = ((ExternalizableScheme)scheme).getExternalInfo();
                if (externalInfo.getCurrentFileName() != null) {
                    result.add(externalInfo.getCurrentFileName());
                }
            }
        }
        return result;
    }

    private void loadScheme( E scheme, boolean forceAdd,  CharSequence fileName) {
        String fileNameWithoutExtension = createFileName(fileName);
        if (!forceAdd && myFilesToDelete.contains(fileNameWithoutExtension)) {
            return;
        }

        T existing = findSchemeByName(scheme.getName());
        if (existing != null) {
            if (!Comparing.equal(existing.getClass(), scheme.getClass())) {
                LOG.warn("'" + scheme.getName() + "' " + existing.getClass().getSimpleName() + " replaced with " + scheme.getClass().getSimpleName());
            }

            mySchemes.remove(existing);
            if (existing instanceof ExternalizableScheme) {
                //noinspection unchecked,CastConflictsWithInstanceof
                myProcessor.onSchemeDeleted((E)existing);
            }
        }

        //noinspection unchecked
        addNewScheme((T)scheme, true);
        scheme.getExternalInfo().setPreviouslySavedName(scheme.getName());
        scheme.getExternalInfo().setCurrentFileName(fileNameWithoutExtension);
    }

    private boolean canRead( VirtualFile file) {
        if (file.isDirectory()) {
            return false;
        }

        if (myUpdateExtension && !DirectoryStorageData.DEFAULT_EXT.equals(mySchemeExtension) && DirectoryStorageData.isStorageFile(file)) {
            // read file.DEFAULT_EXT only if file.CUSTOM_EXT doesn't exists
            return myDir.findChild(file.getNameSequence() + mySchemeExtension) == null;
        }
        else {
            return StringUtilRt.endsWithIgnoreCase(file.getNameSequence(), mySchemeExtension);
        }
    }

    
    private E readSchemeFromFile( final VirtualFile file, boolean forceAdd, boolean duringLoad) {
        if (!canRead(file)) {
            return null;
        }

        try {
            Element element;
            try {
                element = JDOMUtil.load(file.getInputStream());
            }
            catch (JDOMException e) {
                try {
                    File initialIoFile = new File(myIoDir, file.getName());
                    if (initialIoFile.isFile()) {
                        FileUtil.copy(initialIoFile, new File(myIoDir, file.getName() + ".copy"));
                    }
                }
                catch (IOException e1) {
                    LOG.error(e1);
                }
                LOG.error("Error reading file " + file.getPath() + ": " + e.getMessage());
                return null;
            }

            E scheme = readScheme(element, duringLoad);
            if (scheme != null) {
                loadScheme(scheme, forceAdd, file.getNameSequence());
            }
            return scheme;
        }
        catch (final Exception e) {
            ApplicationManager.getApplication().invokeLater(new Runnable() {
                                                                @Override
                                                                public void run() {
                                                                    String msg = "Cannot read scheme " + file.getName() + "  from '" + myFileSpec + "': " + e.getMessage();
                                                                    LOG.info(msg, e);
                                                                    Messages.showErrorDialog(msg, "Load Settings");
                                                                }
                                                            }
            );
            return null;
        }
    }

    
    private E readScheme( Element element, boolean duringLoad) throws InvalidDataException, IOException, JDOMException {
        E scheme;
        if (myProcessor instanceof BaseSchemeProcessor) {
            scheme = ((BaseSchemeProcessor<E>)myProcessor).readScheme(element, duringLoad);
        }
        else {
            //noinspection deprecation
            scheme = myProcessor.readScheme(new Document((Element)element.detach()));
        }
        if (scheme != null) {
            scheme.getExternalInfo().setHash(JDOMUtil.getTreeHash(element, true));
        }
        return scheme;
    }

    
    private String createFileName( CharSequence fileName) {
        if (StringUtilRt.endsWithIgnoreCase(fileName, mySchemeExtension)) {
            fileName = fileName.subSequence(0, fileName.length() - mySchemeExtension.length());
        }
        else if (StringUtilRt.endsWithIgnoreCase(fileName, DirectoryStorageData.DEFAULT_EXT)) {
            fileName = fileName.subSequence(0, fileName.length() - DirectoryStorageData.DEFAULT_EXT.length());
        }
        return fileName.toString();
    }

    public void updateConfigFilesFromStreamProviders() {
        // todo
    }

    private String getFileFullPath( String subPath) {
        return myFileSpec + '/' + subPath;
    }

    @Override
    public void save() {
        boolean hasSchemes = false;
        UniqueNameGenerator nameGenerator = new UniqueNameGenerator();
        List<E> schemesToSave = new SmartList<E>();
        for (T scheme : mySchemes) {
            if (scheme instanceof ExternalizableScheme) {
                //noinspection CastConflictsWithInstanceof,unchecked
                E eScheme = (E)scheme;
                BaseSchemeProcessor.State state;
                if (myProcessor instanceof BaseSchemeProcessor) {
                    state = ((BaseSchemeProcessor<E>)myProcessor).getState(eScheme);
                }
                else {
                    //noinspection deprecation
                    state = myProcessor.shouldBeSaved(eScheme) ? BaseSchemeProcessor.State.POSSIBLY_CHANGED : BaseSchemeProcessor.State.NON_PERSISTENT;
                }

                if (state == BaseSchemeProcessor.State.NON_PERSISTENT) {
                    continue;
                }

                hasSchemes = true;

                if (state != BaseSchemeProcessor.State.UNCHANGED) {
                    schemesToSave.add(eScheme);
                }

                String fileName = eScheme.getExternalInfo().getCurrentFileName();
                if (fileName != null && !isRenamed(eScheme)) {
                    nameGenerator.addExistingName(fileName);
                }
            }
        }

        List<Throwable> errors = null;

        for (E scheme : schemesToSave) {
            try {
                saveScheme(scheme, nameGenerator);
            }
            catch (Throwable e) {
                if (errors == null) {
                    errors = new SmartList<Throwable>();
                }
                errors.add(e);
            }
        }

        VirtualFile dir = getVirtualDir();
        errors = deleteFiles(dir, errors);

        if (!hasSchemes && dir != null) {
            LOG.info("No schemes to save, directory " + dir.getName() + " will be removed");

            AccessToken token = ApplicationManager.getApplication().acquireWriteActionLock(DocumentRunnable.IgnoreDocumentRunnable.class);
            try {
                boolean remove = true;
                for (VirtualFile file : dir.getChildren()) {
                    if (StringUtilRt.endsWithIgnoreCase(file.getNameSequence(), mySchemeExtension)) {
                        LOG.info("Directory " + dir.getName() + " cannot be removed - scheme " + file.getName() + " exists");
                        remove = false;
                        break;
                    }
                }

                if (remove) {
                    LOG.info("Remove schemes directory " + dir.getName());
                    try {
                        StorageUtil.deleteFile(this, dir);
                        myDir = null;
                    }
                    catch (Throwable e) {
                        if (errors == null) {
                            errors = new SmartList<Throwable>();
                        }
                        errors.add(e);
                    }
                }
            }
            finally {
                token.finish();
            }
        }

        CompoundRuntimeException.doThrow(errors);
    }

    private void saveScheme( E scheme,  UniqueNameGenerator nameGenerator) throws WriteExternalException, IOException {
        ExternalInfo externalInfo = scheme.getExternalInfo();
        String currentFileNameWithoutExtension = externalInfo.getCurrentFileName();
        Parent parent = myProcessor.writeScheme(scheme);
        Element element = parent == null || parent instanceof Element ? (Element)parent : ((Document)parent).detachRootElement();
        if (JDOMUtil.isEmpty(element)) {
            ContainerUtilRt.addIfNotNull(myFilesToDelete, currentFileNameWithoutExtension);
            return;
        }

        String fileNameWithoutExtension = currentFileNameWithoutExtension;
        if (fileNameWithoutExtension == null || isRenamed(scheme)) {
            fileNameWithoutExtension = nameGenerator.generateUniqueName(FileUtil.sanitizeName(scheme.getName()));
        }
        String fileName = fileNameWithoutExtension + mySchemeExtension;

        int newHash = JDOMUtil.getTreeHash(element, true);
        if (currentFileNameWithoutExtension == fileNameWithoutExtension && newHash == externalInfo.getHash()) {
            return;
        }

        // file will be overwritten, so, we don't need to delete it
        myFilesToDelete.remove(fileNameWithoutExtension);

        // stream provider always use LF separator
        final BufferExposingByteArrayOutputStream byteOut = StorageUtil.writeToBytes(element, "\n");

        // if another new scheme uses old name of this scheme, so, we must not delete it (as part of rename operation)
        boolean renamed = currentFileNameWithoutExtension != null && fileNameWithoutExtension != currentFileNameWithoutExtension && nameGenerator.value(currentFileNameWithoutExtension);
        if (!externalInfo.isRemote()) {
            VirtualFile file = null;
            if (renamed) {
                file = myDir.findChild(currentFileNameWithoutExtension + mySchemeExtension);
                if (file != null) {
                    AccessToken token = ApplicationManager.getApplication().acquireWriteActionLock(DocumentRunnable.IgnoreDocumentRunnable.class);
                    try {
                        file.rename(this, fileName);
                    }
                    finally {
                        token.finish();
                    }
                }
            }

            if (file == null) {
                if (myDir == null || !myDir.isValid()) {
                    myDir = DirectoryBasedStorage.createDir(myIoDir, this);
                }
                file = DirectoryBasedStorage.getFile(fileName, myDir, this);
            }

            AccessToken token = ApplicationManager.getApplication().acquireWriteActionLock(DocumentRunnable.IgnoreDocumentRunnable.class);
            try {
                OutputStream out = file.getOutputStream(this);
                try {
                    byteOut.writeTo(out);
                }
                finally {
                    out.close();
                }
            }
            finally {
                token.finish();
            }
        }
        else if (renamed) {
            myFilesToDelete.add(currentFileNameWithoutExtension);
        }

        externalInfo.setHash(newHash);
        externalInfo.setPreviouslySavedName(scheme.getName());
        externalInfo.setCurrentFileName(createFileName(fileName));

        if (myProvider != null && myProvider.isEnabled()) {
            String fileSpec = getFileFullPath(fileName);
            if (myProvider.isApplicable(fileSpec, myRoamingType)) {
                myProvider.saveContent(fileSpec, byteOut.getInternalBuffer(), byteOut.size(), myRoamingType, true);
            }
        }
    }

    private static boolean isRenamed( ExternalizableScheme scheme) {
        return !scheme.getName().equals(scheme.getExternalInfo().getPreviouslySavedName());
    }

    
    private List<Throwable> deleteFiles( VirtualFile dir, List<Throwable> errors) {
        if (myFilesToDelete.isEmpty()) {
            return errors;
        }

        if (myProvider != null && myProvider.isEnabled()) {
            for (String nameWithoutExtension : myFilesToDelete) {
                deleteServerFile(nameWithoutExtension + mySchemeExtension);
                if (!DirectoryStorageData.DEFAULT_EXT.equals(mySchemeExtension)) {
                    deleteServerFile(nameWithoutExtension + DirectoryStorageData.DEFAULT_EXT);
                }
            }
        }

        if (dir != null) {
            AccessToken token = ApplicationManager.getApplication().acquireWriteActionLock(DocumentRunnable.IgnoreDocumentRunnable.class);
            try {
                for (VirtualFile file : dir.getChildren()) {
                    if (myFilesToDelete.contains(file.getNameWithoutExtension())) {
                        try {
                            file.delete(this);
                        }
                        catch (IOException e) {
                            if (errors == null) {
                                errors = new SmartList<Throwable>();
                            }
                            errors.add(e);
                        }
                    }
                }
                myFilesToDelete.clear();
            }
            finally {
                token.finish();
            }
        }
        return errors;
    }

    
    private VirtualFile getVirtualDir() {
        VirtualFile virtualFile = myDir;
        if (virtualFile == null) {
            myDir = virtualFile = LocalFileSystem.getInstance().findFileByIoFile(myIoDir);
        }
        return virtualFile;
    }

    @Override
    public File getRootDirectory() {
        return myIoDir;
    }

    private void deleteServerFile( String path) {
        if (myProvider != null && myProvider.isEnabled()) {
            StorageUtil.delete(myProvider, getFileFullPath(path), myRoamingType);
        }
    }

    @Override
    protected void schemeDeleted( Scheme scheme) {
        super.schemeDeleted(scheme);

        if (scheme instanceof ExternalizableScheme) {
            ContainerUtilRt.addIfNotNull(myFilesToDelete, ((ExternalizableScheme)scheme).getExternalInfo().getCurrentFileName());
        }
    }

    @Override
    protected void schemeAdded( T scheme) {
        if (!(scheme instanceof ExternalizableScheme)) {
            return;
        }

        ExternalInfo externalInfo = ((ExternalizableScheme)scheme).getExternalInfo();
        String fileName = externalInfo.getCurrentFileName();
        if (fileName != null) {
            myFilesToDelete.remove(fileName);
        }
        if (myProvider != null && myProvider.isEnabled()) {
            // do not save locally
            externalInfo.markRemote();
        }
    }
}
