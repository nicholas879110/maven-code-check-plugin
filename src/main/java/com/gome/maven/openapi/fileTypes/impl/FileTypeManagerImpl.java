/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.gome.maven.openapi.fileTypes.impl;

import com.google.common.annotations.VisibleForTesting;
import com.gome.maven.ide.highlighter.custom.SyntaxTable;
import com.gome.maven.ide.plugins.PluginManager;
import com.gome.maven.ide.util.PropertiesComponent;
import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.application.impl.TransferToPooledThreadQueue;
import com.gome.maven.openapi.components.*;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.extensions.Extensions;
import com.gome.maven.openapi.fileEditor.impl.LoadTextUtil;
import com.gome.maven.openapi.fileTypes.*;
import com.gome.maven.openapi.fileTypes.ex.*;
import com.gome.maven.openapi.options.BaseSchemeProcessor;
import com.gome.maven.openapi.options.SchemesManager;
import com.gome.maven.openapi.options.SchemesManagerFactory;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.*;
import com.gome.maven.openapi.util.io.ByteSequence;
import com.gome.maven.openapi.util.io.FileUtil;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.openapi.util.text.StringUtilRt;
import com.gome.maven.openapi.vfs.*;
import com.gome.maven.openapi.vfs.newvfs.BulkFileListener;
import com.gome.maven.openapi.vfs.newvfs.FileAttribute;
import com.gome.maven.openapi.vfs.newvfs.FileSystemInterface;
import com.gome.maven.openapi.vfs.newvfs.events.VFileCreateEvent;
import com.gome.maven.openapi.vfs.newvfs.events.VFileEvent;
import com.gome.maven.openapi.vfs.newvfs.impl.StubVirtualFile;
import com.gome.maven.psi.SingleRootFileViewProvider;
import com.gome.maven.testFramework.LightVirtualFile;
import com.gome.maven.util.*;
import com.gome.maven.util.containers.ConcurrentPackedBitsArray;
import com.gome.maven.util.containers.ContainerUtil;
import com.gome.maven.util.io.URLUtil;
import com.gome.maven.util.messages.MessageBus;
import com.gome.maven.util.messages.MessageBusConnection;
import gnu.trove.THashMap;
import gnu.trove.THashSet;
import org.jdom.Element;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@State(
        name = "FileTypeManager",
        storages = @Storage(file = StoragePathMacros.APP_CONFIG + "/filetypes.xml"),
        additionalExportFile = FileTypeManagerImpl.FILE_SPEC
)
public class FileTypeManagerImpl extends FileTypeManagerEx implements PersistentStateComponent<Element>, ApplicationComponent, Disposable {
    private static final Logger LOG = Logger.getInstance(FileTypeManagerImpl.class);

    // You must update all existing default configurations accordingly
    private static final int VERSION = 14;
    private static final Key<FileType> FILE_TYPE_KEY = Key.create("FILE_TYPE_KEY");
    // cached auto-detected file type. If the file was auto-detected as plain text or binary
    // then the value is null and autoDetectedAsText, autoDetectedAsBinary and autoDetectWasRun sets are used instead.
    private static final Key<FileType> DETECTED_FROM_CONTENT_FILE_TYPE_KEY = Key.create("DETECTED_FROM_CONTENT_FILE_TYPE_KEY");
    private static final int DETECT_BUFFER_SIZE = 8192; // the number of bytes to read from the file to feed to the file type detector

    
    private static final String DEFAULT_IGNORED =
            "*.hprof;*.pyc;*.pyo;*.rbc;*~;.DS_Store;.bundle;.git;.hg;.svn;CVS;RCS;SCCS;__pycache__;.tox;_svn;rcs;vssver.scc;vssver2.scc;";

    private static boolean RE_DETECT_ASYNC = !ApplicationManager.getApplication().isUnitTestMode();
    private final Set<FileType> myDefaultTypes = new THashSet<FileType>();
    private final List<FileTypeIdentifiableByVirtualFile> mySpecialFileTypes = new ArrayList<FileTypeIdentifiableByVirtualFile>();

    private FileTypeAssocTable<FileType> myPatternsTable = new FileTypeAssocTable<FileType>();
    private final IgnoredPatternSet myIgnoredPatterns = new IgnoredPatternSet();
    private final IgnoredFileCache myIgnoredFileCache = new IgnoredFileCache(myIgnoredPatterns);

    private final FileTypeAssocTable<FileType> myInitialAssociations = new FileTypeAssocTable<FileType>();
    private final Map<FileNameMatcher, String> myUnresolvedMappings = new THashMap<FileNameMatcher, String>();
    private final Map<FileNameMatcher, Trinity<String, String, Boolean>> myUnresolvedRemovedMappings = new THashMap<FileNameMatcher, Trinity<String, String, Boolean>>();
    /** This will contain removed mappings with "approved" states */
    private final Map<FileNameMatcher, Pair<FileType, Boolean>> myRemovedMappings = new THashMap<FileNameMatcher, Pair<FileType, Boolean>>();

     private static final String ELEMENT_FILETYPE = "filetype";
     private static final String ELEMENT_IGNORE_FILES = "ignoreFiles";
     private static final String ATTRIBUTE_LIST = "list";

     private static final String ATTRIBUTE_VERSION = "version";
     private static final String ATTRIBUTE_NAME = "name";
     private static final String ATTRIBUTE_DESCRIPTION = "description";

    private static class StandardFileType {
         private final FileType fileType;
         private final List<FileNameMatcher> matchers;

        private StandardFileType( FileType fileType,  List<FileNameMatcher> matchers) {
            this.fileType = fileType;
            this.matchers = matchers;
        }
    }

    private final MessageBus myMessageBus;
    private final Map<String, StandardFileType> myStandardFileTypes = new LinkedHashMap<String, StandardFileType>();
    
    private static final String[] FILE_TYPES_WITH_PREDEFINED_EXTENSIONS = {"JSP", "JSPX", "DTD", "HTML", "Properties", "XHTML"};
    private final SchemesManager<FileType, AbstractFileType> mySchemesManager;
    
    static final String FILE_SPEC = StoragePathMacros.ROOT_CONFIG + "/filetypes";

    // these flags are stored in 'packedFlags' as chunks of four bits
    private static final int AUTO_DETECTED_AS_TEXT_MASK = 1;
    private static final int AUTO_DETECTED_AS_BINARY_MASK = 2;
    private static final int AUTO_DETECT_WAS_RUN_MASK = 4;
    private static final int ATTRIBUTES_WERE_LOADED_MASK = 8;
    private final ConcurrentPackedBitsArray packedFlags = new ConcurrentPackedBitsArray(4);

    private final AtomicInteger counterAutoDetect = new AtomicInteger();
    private final AtomicLong elapsedAutoDetect = new AtomicLong();

    public FileTypeManagerImpl(MessageBus bus, SchemesManagerFactory schemesManagerFactory, PropertiesComponent propertiesComponent) {
        int fileTypeChangedCounter = StringUtilRt.parseInt(propertiesComponent.getValue("fileTypeChangedCounter"), 0);
        fileTypeChangedCount = new AtomicInteger(fileTypeChangedCounter);
        autoDetectedAttribute = new FileAttribute("AUTO_DETECTION_CACHE_ATTRIBUTE", fileTypeChangedCounter, true);

        myMessageBus = bus;
        mySchemesManager = schemesManagerFactory.createSchemesManager(FILE_SPEC, new BaseSchemeProcessor<AbstractFileType>() {
            
            @Override
            public AbstractFileType readScheme( Element element, boolean duringLoad) {
                if (!duringLoad) {
                    fireBeforeFileTypesChanged();
                }
                AbstractFileType type = (AbstractFileType)loadFileType(element, false);
                if (!duringLoad) {
                    fireFileTypesChanged();
                }
                return type;
            }

            
            @Override
            public State getState( AbstractFileType fileType) {
                if (!shouldSave(fileType)) {
                    return State.NON_PERSISTENT;
                }
                if (!myDefaultTypes.contains(fileType)) {
                    return State.POSSIBLY_CHANGED;
                }
                return fileType.isModified() ? State.POSSIBLY_CHANGED : State.NON_PERSISTENT;
            }

            @Override
            public Element writeScheme( AbstractFileType fileType) {
                Element root = new Element(ELEMENT_FILETYPE);

                root.setAttribute("binary", String.valueOf(fileType.isBinary()));
                if (!StringUtil.isEmpty(fileType.getDefaultExtension())) {
                    root.setAttribute("default_extension", fileType.getDefaultExtension());
                }
                root.setAttribute(ATTRIBUTE_DESCRIPTION, fileType.getDescription());
                root.setAttribute(ATTRIBUTE_NAME, fileType.getName());

                fileType.writeExternal(root);

                Element map = new Element(AbstractFileType.ELEMENT_EXTENSION_MAP);
                writeExtensionsMap(map, fileType, false);
                if (!map.getChildren().isEmpty()) {
                    root.addContent(map);
                }
                return root;
            }

            @Override
            public void onSchemeDeleted( AbstractFileType scheme) {
                fireBeforeFileTypesChanged();
                myPatternsTable.removeAllAssociations(scheme);
                fireFileTypesChanged();
            }
        }, RoamingType.PER_USER);
        bus.connect().subscribe(VirtualFileManager.VFS_CHANGES, new BulkFileListener.Adapter() {
            @Override
            public void after( List<? extends VFileEvent> events) {
                Collection<VirtualFile> files = ContainerUtil.map2Set(events, new Function<VFileEvent, VirtualFile>() {
                    @Override
                    public VirtualFile fun(VFileEvent event) {
                        VirtualFile file = event instanceof VFileCreateEvent ? null : event.getFile();
                        return file != null && wasAutoDetectedBefore(file) && isDetectable(file) ? file : null;
                    }
                });
                files.remove(null);
                if (toLog()) {
                    log("F: VFS events: " + events);
                }
                if (!files.isEmpty() && RE_DETECT_ASYNC) {
                    if (toLog()) {
                        log("F: queued to redetect: " + files);
                    }
                    reDetectQueue.offerIfAbsent(files);
                }
            }
        });

        //noinspection SpellCheckingInspection
        myIgnoredPatterns.setIgnoreMasks(DEFAULT_IGNORED);

        // this should be done BEFORE reading state
        initStandardFileTypes();
    }

    @VisibleForTesting
    void initStandardFileTypes() {
        FileTypeConsumer consumer = new FileTypeConsumer() {
            @Override
            public void consume( FileType fileType) {
                register(fileType, parse(fileType.getDefaultExtension()));
            }

            @Override
            public void consume( final FileType fileType, String extensions) {
                register(fileType, parse(extensions));
            }

            @Override
            public void consume( final FileType fileType,  final FileNameMatcher... matchers) {
                register(fileType, new ArrayList<FileNameMatcher>(Arrays.asList(matchers)));
            }

            @Override
            public FileType getStandardFileTypeByName( final String name) {
                final StandardFileType type = myStandardFileTypes.get(name);
                return type != null ? type.fileType : null;
            }

            private void register( FileType fileType,  List<FileNameMatcher> fileNameMatchers) {
                final StandardFileType type = myStandardFileTypes.get(fileType.getName());
                if (type != null) {
                    type.matchers.addAll(fileNameMatchers);
                }
                else {
                    myStandardFileTypes.put(fileType.getName(), new StandardFileType(fileType, fileNameMatchers));
                }
            }
        };

        for (FileTypeFactory factory : FileTypeFactory.FILE_TYPE_FACTORY_EP.getExtensions()) {
            try {
                factory.createFileTypes(consumer);
            }
            catch (Throwable e) {
                PluginManager.handleComponentError(e, factory.getClass().getName(), null);
            }
        }
        for (StandardFileType pair : myStandardFileTypes.values()) {
            registerFileTypeWithoutNotification(pair.fileType, pair.matchers, true);
        }

        if (PlatformUtils.isDatabaseIDE() || PlatformUtils.isCidr()) {
            // build scripts are correct, but it is required to run from sources
            return;
        }

        try {
            URL defaultFileTypesUrl = FileTypeManagerImpl.class.getResource("/defaultFileTypes.xml");
            if (defaultFileTypesUrl != null) {
                Element defaultFileTypesElement = JDOMUtil.load(URLUtil.openStream(defaultFileTypesUrl));
                for (Element e : (List<Element>)defaultFileTypesElement.getChildren()) {
                    //noinspection SpellCheckingInspection
                    if ("filetypes".equals(e.getName())) {
                        for (Element element : (List<Element>)e.getChildren(ELEMENT_FILETYPE)) {
                            loadFileType(element, true);
                        }
                    }
                    else if (AbstractFileType.ELEMENT_EXTENSION_MAP.equals(e.getName())) {
                        readGlobalMappings(e);
                    }
                }
            }
        }
        catch (Exception e) {
            LOG.error(e);
        }
    }

    private static boolean toLog() {
        return RE_DETECT_ASYNC && ApplicationManager.getApplication().isUnitTestMode();
    }

    private static void log(@SuppressWarnings("UnusedParameters") String message) {
        //System.out.println(message);
    }

    private final TransferToPooledThreadQueue<Collection<VirtualFile>> reDetectQueue = new TransferToPooledThreadQueue<Collection<VirtualFile>>("File type re-detect", Conditions.alwaysFalse(), -1, new Processor<Collection<VirtualFile>>() {
        @Override
        public boolean process(Collection<VirtualFile> files) {
            reDetect(files);
            return true;
        }
    });

    
    public void drainReDetectQueue() {
        reDetectQueue.waitFor();
    }

    
    
    Collection<VirtualFile> dumpReDetectQueue() {
        return ContainerUtil.flatten(reDetectQueue.dump());
    }

    
    static void reDetectAsync(boolean enable) {
        RE_DETECT_ASYNC = enable;
    }

    private void reDetect( Collection<VirtualFile> files) {
        final List<VirtualFile> changed = new ArrayList<VirtualFile>();
        for (VirtualFile file : files) {
            boolean shouldRedetect = wasAutoDetectedBefore(file) && isDetectable(file);
            if (toLog()) {
                log("F: Redetect file: " + file.getName() + "; shouldRedetect: " + shouldRedetect);
            }
            if (shouldRedetect) {
                int id = file instanceof VirtualFileWithId ? ((VirtualFileWithId)file).getId() : -1;
                FileType before = getAutoDetectedType(file, id);

                packedFlags.set(id, ATTRIBUTES_WERE_LOADED_MASK);

                file.putUserData(DETECTED_FROM_CONTENT_FILE_TYPE_KEY, null);
                FileType after = getFileTypeByFile(file); // may be back to standard file type
                if (toLog()) {
                    log("F: After redetect file: " + file.getName() + "; before: " + before.getName() + "; after: " + after.getName()+"; now getFileType()="+file.getFileType().getName());
                }

                if (before != after) {
                    changed.add(file);
                    LOG.debug(file+" type was re-detected. Was: "+before.getName()+"; now: "+after.getName());
                }
            }
        }
        if (!changed.isEmpty()) {
            ApplicationManager.getApplication().invokeLater(new Runnable() {
                @Override
                public void run() {
                    FileContentUtilCore.reparseFiles(changed);
                }
            }, ApplicationManager.getApplication().getDisposed());
        }
    }

    private boolean wasAutoDetectedBefore( VirtualFile file) {
        if (file.getUserData(DETECTED_FROM_CONTENT_FILE_TYPE_KEY) != null) return true;
        if (file instanceof VirtualFileWithId) {
            int id = Math.abs(((VirtualFileWithId)file).getId());
            // do not re-detect binary files
            return (packedFlags.get(id) & (AUTO_DETECT_WAS_RUN_MASK | AUTO_DETECTED_AS_BINARY_MASK)) == AUTO_DETECT_WAS_RUN_MASK;
        }
        return false;
    }

    @Override
    
    public FileType getStdFileType(  String name) {
        StandardFileType stdFileType = myStandardFileTypes.get(name);
        return stdFileType != null ? stdFileType.fileType : PlainTextFileType.INSTANCE;
    }

    @Override
    public void disposeComponent() {
    }

    @Override
    public void initComponent() {
        if (!myUnresolvedMappings.isEmpty()) {
            for (StandardFileType pair : myStandardFileTypes.values()) {
                registerReDetectedMappings(pair);
            }
        }
        // Resolve unresolved mappings initialized before certain plugin initialized.
        for (StandardFileType pair : myStandardFileTypes.values()) {
            bindUnresolvedMappings(pair.fileType);
        }

        boolean isAtLeastOneStandardFileTypeHasBeenRead = false;
        for (AbstractFileType fileType : mySchemesManager.loadSchemes()) {
            isAtLeastOneStandardFileTypeHasBeenRead |= myInitialAssociations.hasAssociationsFor(fileType);
        }
        if (isAtLeastOneStandardFileTypeHasBeenRead) {
            restoreStandardFileExtensions();
        }
    }

    @Override
    
    public FileType getFileTypeByFileName( String fileName) {
        return getFileTypeByFileName((CharSequence)fileName);
    }

    
    private FileType getFileTypeByFileName( CharSequence fileName) {
        FileType type = myPatternsTable.findAssociatedFileType(fileName);
        return type == null ? UnknownFileType.INSTANCE : type;
    }

    public static void cacheFileType( VirtualFile file,  FileType fileType) {
        file.putUserData(FILE_TYPE_KEY, fileType);
        if (toLog()) {
            log("F: Cached file type for "+file.getName()+" to "+(fileType == null ? null : fileType.getName()));
        }
    }

    @Override
    
    public FileType getFileTypeByFile( VirtualFile file) {
        FileType fileType = file.getUserData(FILE_TYPE_KEY);
        if (fileType != null) return fileType;

        if (file instanceof LightVirtualFile) {
            fileType = ((LightVirtualFile)file).getAssignedFileType();
            if (fileType != null) return fileType;
        }

        //noinspection ForLoopReplaceableByForEach
        for (int i = 0; i < mySpecialFileTypes.size(); i++) {
            FileTypeIdentifiableByVirtualFile type = mySpecialFileTypes.get(i);
            if (type.isMyFileType(file)) {
                if (toLog()) {
                    log("F: Special file type for "+file.getName()+"; type: "+type.getName());
                }
                return type;
            }
        }

        fileType = getFileTypeByFileName(file.getNameSequence());
        if (fileType != UnknownFileType.INSTANCE) {
            if (toLog()) {
                log("F: By name file type for "+file.getName()+"; type: "+fileType.getName());
            }
            return fileType;
        }

        if (!(file instanceof StubVirtualFile)) {
            fileType = getOrDetectFromContent(file);
        }

        return fileType;
    }

    
    private FileType getOrDetectFromContent( VirtualFile file) {
        if (!isDetectable(file)) return UnknownFileType.INSTANCE;
        if (file instanceof VirtualFileWithId) {
            int id = ((VirtualFileWithId)file).getId();
            if (id < 0) return UnknownFileType.INSTANCE;

            //boolean autoDetectWasRun = this.autoDetectWasRun.get(id);
            long flags = packedFlags.get(id);
            boolean autoDetectWasRun = (flags & AUTO_DETECT_WAS_RUN_MASK) != 0;
            if (autoDetectWasRun) {
                FileType type = getAutoDetectedType(file, id);
                if (toLog()) {
                    log("F: autodetected getFileType("+file.getName()+") = "+type.getName());
                }
                return type;
            }
            boolean wasDetectedAsText = false;
            boolean wasDetectedAsBinary = false;
            boolean wasAutoDetectRun = false;
            if ((flags & ATTRIBUTES_WERE_LOADED_MASK) == 0) {
                DataInputStream stream = autoDetectedAttribute.readAttribute(file);
                try {
                    try {
                        byte status = stream != null ? stream.readByte() : 0;
                        wasAutoDetectRun = stream != null;
                        wasDetectedAsText = BitUtil.isSet(status, AUTO_DETECTED_AS_TEXT_MASK);
                        wasDetectedAsBinary = BitUtil.isSet(status, AUTO_DETECTED_AS_BINARY_MASK);
                    }
                    finally {
                        if (stream != null) {
                            stream.close();
                        }
                    }
                }
                catch (IOException ignored) {
                }
                flags = ATTRIBUTES_WERE_LOADED_MASK;
                flags = BitUtil.set(flags, AUTO_DETECTED_AS_TEXT_MASK, wasDetectedAsText);
                flags = BitUtil.set(flags, AUTO_DETECTED_AS_BINARY_MASK, wasDetectedAsBinary);
                flags = BitUtil.set(flags, AUTO_DETECT_WAS_RUN_MASK, wasAutoDetectRun);

                packedFlags.set(id, flags);
            }
            if (wasAutoDetectRun && (wasDetectedAsText || wasDetectedAsBinary)) {
                return wasDetectedAsText ? FileTypes.PLAIN_TEXT : UnknownFileType.INSTANCE;
            }
        }
        FileType fileType = file.getUserData(DETECTED_FROM_CONTENT_FILE_TYPE_KEY);
        // run autodetection
        if (fileType == null) {
            fileType = detectFromContent(file);
        }

        if (toLog()) {
            log("F: getFileType after detect run("+file.getName()+") = "+fileType.getName());
        }

        return fileType;
    }

    
    private FileType getAutoDetectedType( VirtualFile file, int id) {
        long flags = packedFlags.get(id);
        return BitUtil.isSet(flags, AUTO_DETECTED_AS_TEXT_MASK) ? FileTypes.PLAIN_TEXT :
                BitUtil.isSet(flags, AUTO_DETECTED_AS_BINARY_MASK) ? UnknownFileType.INSTANCE :
                        ObjectUtils.notNull(file.getUserData(DETECTED_FROM_CONTENT_FILE_TYPE_KEY), FileTypes.PLAIN_TEXT);
    }

    
    @Override
    @Deprecated
    public FileType detectFileTypeFromContent( VirtualFile file) {
        return file.getFileType();
    }

    private volatile FileAttribute autoDetectedAttribute;
    private void cacheAutoDetectedFileType( VirtualFile file,  FileType fileType) {
        DataOutputStream stream = autoDetectedAttribute.writeAttribute(file);
        boolean wasAutodetectedAsText = fileType == FileTypes.PLAIN_TEXT;
        boolean wasAutodetectedAsBinary = fileType == FileTypes.UNKNOWN;
        try {
            try {
                int flags = BitUtil.set(0, AUTO_DETECTED_AS_TEXT_MASK, wasAutodetectedAsText);
                flags = BitUtil.set(flags, AUTO_DETECTED_AS_BINARY_MASK, wasAutodetectedAsBinary);
                stream.writeByte(flags);
            }
            finally {
                stream.close();
            }
        }
        catch (IOException e) {
            LOG.error(e);
        }
        if (file instanceof VirtualFileWithId) {
            int id = Math.abs(((VirtualFileWithId)file).getId());
            int flags = AUTO_DETECT_WAS_RUN_MASK | ATTRIBUTES_WERE_LOADED_MASK;
            flags = BitUtil.set(flags, AUTO_DETECTED_AS_TEXT_MASK, wasAutodetectedAsText);
            flags = BitUtil.set(flags, AUTO_DETECTED_AS_BINARY_MASK, wasAutodetectedAsBinary);
            packedFlags.set(id, flags);

            if (wasAutodetectedAsText || wasAutodetectedAsBinary) {
                file.putUserData(DETECTED_FROM_CONTENT_FILE_TYPE_KEY, null);
                return;
            }
        }
        file.putUserData(DETECTED_FROM_CONTENT_FILE_TYPE_KEY, fileType);
    }

    @Override
    public FileType findFileTypeByName(String fileTypeName) {
        FileType type = getStdFileType(fileTypeName);
        // TODO: Abstract file types are not std one, so need to be restored specially,
        // currently there are 6 of them and restoration does not happen very often so just iteration is enough
        if (type == PlainTextFileType.INSTANCE && !fileTypeName.equals(type.getName())) {
            for (FileType fileType: mySchemesManager.getAllSchemes()) {
                if (fileTypeName.equals(fileType.getName())) {
                    return fileType;
                }
            }
        }
        return type;
    }

    private static boolean isDetectable( final VirtualFile file) {
        if (file.isDirectory() || !file.isValid() || file.is(VFileProperty.SPECIAL) || file.getLength() == 0) {
            // for empty file there is still hope its type will change
            return false;
        }
        return file.getFileSystem() instanceof FileSystemInterface && !SingleRootFileViewProvider.isTooLargeForContentLoading(file);
    }

    
    private FileType detectFromContent( final VirtualFile file) {
        long start = System.currentTimeMillis();
        try {
            final InputStream inputStream = ((FileSystemInterface)file.getFileSystem()).getInputStream(file);
            final Ref<FileType> result = new Ref<FileType>(UnknownFileType.INSTANCE);
            try {
                FileUtil.processFirstBytes(inputStream, DETECT_BUFFER_SIZE, new Processor<ByteSequence>() {
                    @Override
                    public boolean process(ByteSequence byteSequence) {
                        boolean isText = guessIfText(file, byteSequence);
                        CharSequence text;
                        if (isText) {
                            byte[] bytes = Arrays.copyOf(byteSequence.getBytes(), byteSequence.getLength());
                            text = LoadTextUtil.getTextByBinaryPresentation(bytes, file, true, true, UnknownFileType.INSTANCE);
                        }
                        else {
                            text = null;
                        }

                        FileType detected = null;
                        for (FileTypeDetector detector : Extensions.getExtensions(FileTypeDetector.EP_NAME)) {
                            try {
                                detected = detector.detect(file, byteSequence, text);
                            }
                            catch (Exception e) {
                                LOG.error("Detector " + detector + " (" + detector.getClass() + ") exception occurred:", e);
                            }
                            if (detected != null) break;
                        }

                        if (detected == null) {
                            detected = isText ? PlainTextFileType.INSTANCE : UnknownFileType.INSTANCE;
                        }
                        result.set(detected);
                        return true;
                    }
                });
            }
            finally {
                inputStream.close();
            }
            FileType fileType = result.get();
            if (toLog()) {
                log("F: Redetect run for file: " + file.getName() + "; result: "+fileType.getName());
            }

            if (LOG.isDebugEnabled()) {
                LOG.debug(file + "; type=" + fileType.getDescription() + "; " + counterAutoDetect);
            }

            cacheAutoDetectedFileType(file, fileType);
            counterAutoDetect.incrementAndGet();
            long elapsed = System.currentTimeMillis() - start;
            elapsedAutoDetect.addAndGet(elapsed);

            return fileType;
        }
        catch (IOException ignored) {
            return UnknownFileType.INSTANCE; // return unknown, do not cache
        }
    }

    private static boolean guessIfText( VirtualFile file,  ByteSequence byteSequence) {
        byte[] bytes = byteSequence.getBytes();
        Trinity<Charset, CharsetToolkit.GuessedEncoding, byte[]> guessed = LoadTextUtil.guessFromContent(file, bytes, byteSequence.getLength());
        if (guessed == null) return false;
        file.setBOM(guessed.third);
        if (guessed.first != null) {
            // charset was detected unambiguously
            return true;
        }
        // use wild guess
        CharsetToolkit.GuessedEncoding guess = guessed.second;
        return guess != null && (guess == CharsetToolkit.GuessedEncoding.VALID_UTF8 || guess == CharsetToolkit.GuessedEncoding.SEVEN_BIT);
    }

    @Override
    public boolean isFileOfType( VirtualFile file,  FileType type) {
        if (type instanceof FileTypeIdentifiableByVirtualFile) {
            return ((FileTypeIdentifiableByVirtualFile)type).isMyFileType(file);
        }

        return getFileTypeByFileName(file.getName()) == type;
    }

    @Override
    
    public FileType getFileTypeByExtension( String extension) {
        return getFileTypeByFileName("IntelliJ_IDEA_RULES." + extension);
    }

    @Override
    public void registerFileType( FileType fileType) {
        //noinspection deprecation
        registerFileType(fileType, ArrayUtil.EMPTY_STRING_ARRAY);
    }

    @Override
    public void registerFileType( final FileType type,  final List<FileNameMatcher> defaultAssociations) {
        ApplicationManager.getApplication().runWriteAction(new Runnable() {
            @Override
            public void run() {
                fireBeforeFileTypesChanged();
                registerFileTypeWithoutNotification(type, defaultAssociations, true);
                fireFileTypesChanged();
            }
        });
    }

    @Override
    public void unregisterFileType( final FileType fileType) {
        ApplicationManager.getApplication().runWriteAction(new Runnable() {
            @Override
            public void run() {
                fireBeforeFileTypesChanged();
                unregisterFileTypeWithoutNotification(fileType);
                fireFileTypesChanged();
            }
        });
    }

    private void unregisterFileTypeWithoutNotification(FileType fileType) {
        myPatternsTable.removeAllAssociations(fileType);
        mySchemesManager.removeScheme(fileType);
        if (fileType instanceof FileTypeIdentifiableByVirtualFile) {
            final FileTypeIdentifiableByVirtualFile fakeFileType = (FileTypeIdentifiableByVirtualFile)fileType;
            mySpecialFileTypes.remove(fakeFileType);
        }
    }

    @Override
    
    public FileType[] getRegisteredFileTypes() {
        Collection<FileType> fileTypes = mySchemesManager.getAllSchemes();
        return fileTypes.toArray(new FileType[fileTypes.size()]);
    }

    @Override
    
    public String getExtension( String fileName) {
        int index = fileName.lastIndexOf('.');
        if (index < 0) return "";
        return fileName.substring(index + 1);
    }

    @Override
    
    public String getIgnoredFilesList() {
        Set<String> masks = myIgnoredPatterns.getIgnoreMasks();
        return masks.isEmpty() ? "" : StringUtil.join(masks, ";") + ";";
    }

    @Override
    public void setIgnoredFilesList( String list) {
        fireBeforeFileTypesChanged();
        myIgnoredFileCache.clearCache();
        myIgnoredPatterns.setIgnoreMasks(list);
        fireFileTypesChanged();
    }

    @Override
    public boolean isIgnoredFilesListEqualToCurrent( String list) {
        Set<String> tempSet = new THashSet<String>();
        StringTokenizer tokenizer = new StringTokenizer(list, ";");
        while (tokenizer.hasMoreTokens()) {
            tempSet.add(tokenizer.nextToken());
        }
        return tempSet.equals(myIgnoredPatterns.getIgnoreMasks());
    }

    @Override
    public boolean isFileIgnored( String name) {
        return myIgnoredPatterns.isIgnored(name);
    }

    @Override
    public boolean isFileIgnored(  VirtualFile file) {
        return myIgnoredFileCache.isFileIgnored(file);
    }

    @Override
    @SuppressWarnings({"deprecation"})
    
    public String[] getAssociatedExtensions( FileType type) {
        return myPatternsTable.getAssociatedExtensions(type);
    }

    @Override
    
    public List<FileNameMatcher> getAssociations( FileType type) {
        return myPatternsTable.getAssociations(type);
    }

    @Override
    public void associate( FileType type,  FileNameMatcher matcher) {
        associate(type, matcher, true);
    }

    @Override
    public void removeAssociation( FileType type,  FileNameMatcher matcher) {
        removeAssociation(type, matcher, true);
    }

    @Override
    public void fireBeforeFileTypesChanged() {
        FileTypeEvent event = new FileTypeEvent(this);
        myMessageBus.syncPublisher(TOPIC).beforeFileTypesChanged(event);
    }

    private final AtomicInteger fileTypeChangedCount;
    @Override
    public void fireFileTypesChanged() {
        clearCaches();
        myMessageBus.syncPublisher(TOPIC).fileTypesChanged(new FileTypeEvent(this));
    }

    private void clearCaches() {
        int count = fileTypeChangedCount.incrementAndGet();
        autoDetectedAttribute = autoDetectedAttribute.newVersion(count);
        PropertiesComponent.getInstance().setValue("fileTypeChangedCounter", Integer.toString(count));
        packedFlags.clear();
    }

    private final Map<FileTypeListener, MessageBusConnection> myAdapters = new HashMap<FileTypeListener, MessageBusConnection>();

    @Override
    public void addFileTypeListener( FileTypeListener listener) {
        final MessageBusConnection connection = myMessageBus.connect();
        connection.subscribe(TOPIC, listener);
        myAdapters.put(listener, connection);
    }

    @Override
    public void removeFileTypeListener( FileTypeListener listener) {
        final MessageBusConnection connection = myAdapters.remove(listener);
        if (connection != null) {
            connection.disconnect();
        }
    }

    @Override
    public void loadState(Element state) {
        int savedVersion = StringUtilRt.parseInt(state.getAttributeValue(ATTRIBUTE_VERSION), 0);

        for (Element element : (List<Element>)state.getChildren()) {
            if (element.getName().equals(ELEMENT_IGNORE_FILES)) {
                myIgnoredPatterns.setIgnoreMasks(element.getAttributeValue(ATTRIBUTE_LIST));
            }
            else if (AbstractFileType.ELEMENT_EXTENSION_MAP.equals(element.getName())) {
                readGlobalMappings(element);
            }
        }

        if (savedVersion < 4) {
            if (savedVersion == 0) {
                addIgnore(".svn");
            }

            if (savedVersion < 2) {
                restoreStandardFileExtensions();
            }

            addIgnore("*.pyc");
            addIgnore("*.pyo");
            addIgnore(".git");
        }

        if (savedVersion < 5) {
            addIgnore("*.hprof");
        }

        if (savedVersion < 6) {
            addIgnore("_svn");
        }

        if (savedVersion < 7) {
            addIgnore(".hg");
        }

        if (savedVersion < 8) {
            addIgnore("*~");
        }

        if (savedVersion < 9) {
            addIgnore("__pycache__");
        }

        if (savedVersion < 10) {
            addIgnore(".bundle");
        }

        if (savedVersion < 11) {
            addIgnore("*.rbc");
        }

        if (savedVersion < 13) {
            // we want *.lib back since it's an important user artifact for CLion, also for IDEA project itself, since we have some libs.
            Set<String> masks = new LinkedHashSet<String>(myIgnoredPatterns.getIgnoreMasks());
            masks.remove("*.lib");

            myIgnoredPatterns.clearPatterns();
            for (String each : masks) {
                myIgnoredPatterns.addIgnoreMask(each);
            }
        }

        if (savedVersion < 14) {
            addIgnore(".tox");
        }

        myIgnoredFileCache.clearCache();

        String counter = JDOMExternalizer.readString(state, "fileTypeChangedCounter");
        if (counter != null) {
            fileTypeChangedCount.set(StringUtilRt.parseInt(counter, 0));
            autoDetectedAttribute = autoDetectedAttribute.newVersion(fileTypeChangedCount.get());
        }
    }

    private void readGlobalMappings( Element e) {
        for (Pair<FileNameMatcher, String> association : AbstractFileType.readAssociations(e)) {
            FileType type = getFileTypeByName(association.getSecond());
            FileNameMatcher matcher = association.getFirst();
            if (type != null) {
                if (PlainTextFileType.INSTANCE == type) {
                    FileType newFileType = myPatternsTable.findAssociatedFileType(matcher);
                    if (newFileType != null && newFileType != PlainTextFileType.INSTANCE && newFileType != UnknownFileType.INSTANCE) {
                        myRemovedMappings.put(matcher, Pair.create(newFileType, false));
                    }
                }
                associate(type, matcher, false);
            }
            else {
                myUnresolvedMappings.put(matcher, association.getSecond());
            }
        }

        List<Trinity<FileNameMatcher, String, Boolean>> removedAssociations = AbstractFileType.readRemovedAssociations(e);
        for (Trinity<FileNameMatcher, String, Boolean> trinity : removedAssociations) {
            FileType type = getFileTypeByName(trinity.getSecond());
            FileNameMatcher matcher = trinity.getFirst();
            if (type != null) {
                removeAssociation(type, matcher, false);
            }
            else {
                myUnresolvedRemovedMappings.put(matcher, Trinity.create(trinity.getSecond(), myUnresolvedMappings.get(matcher), trinity.getThird()));
            }
        }
    }

    private void addIgnore(  String ignoreMask) {
        myIgnoredPatterns.addIgnoreMask(ignoreMask);
    }

    private void restoreStandardFileExtensions() {
        for (final String name : FILE_TYPES_WITH_PREDEFINED_EXTENSIONS) {
            final StandardFileType stdFileType = myStandardFileTypes.get(name);
            if (stdFileType != null) {
                FileType fileType = stdFileType.fileType;
                for (FileNameMatcher matcher : myPatternsTable.getAssociations(fileType)) {
                    FileType defaultFileType = myInitialAssociations.findAssociatedFileType(matcher);
                    if (defaultFileType != null && defaultFileType != fileType) {
                        removeAssociation(fileType, matcher, false);
                        associate(defaultFileType, matcher, false);
                    }
                }

                for (FileNameMatcher matcher : myInitialAssociations.getAssociations(fileType)) {
                    associate(fileType, matcher, false);
                }
            }
        }
    }

    
    @Override
    public Element getState() {
        Element state = new Element("state");

        Set<String> masks = myIgnoredPatterns.getIgnoreMasks();
        String ignoreFiles;
        if (masks.isEmpty()) {
            ignoreFiles = "";
        }
        else {
            String[] strings = ArrayUtil.toStringArray(masks);
            Arrays.sort(strings);
            ignoreFiles = StringUtil.join(strings, ";") + ";";
        }

        if (!ignoreFiles.equalsIgnoreCase(DEFAULT_IGNORED)) {
            // empty means empty list - we need to distinguish null and empty to apply or not to apply default value
            state.addContent(new Element(ELEMENT_IGNORE_FILES).setAttribute(ATTRIBUTE_LIST, ignoreFiles));
        }

        Element map = new Element(AbstractFileType.ELEMENT_EXTENSION_MAP);

        List<FileType> notExternalizableFileTypes = new ArrayList<FileType>();
        for (FileType type : mySchemesManager.getAllSchemes()) {
            if (!(type instanceof AbstractFileType) || myDefaultTypes.contains(type)) {
                notExternalizableFileTypes.add(type);
            }
        }
        if (!notExternalizableFileTypes.isEmpty()) {
            Collections.sort(notExternalizableFileTypes, new Comparator<FileType>() {
                @Override
                public int compare( FileType o1,  FileType o2) {
                    return o1.getName().compareTo(o2.getName());
                }
            });
            for (FileType type : notExternalizableFileTypes) {
                writeExtensionsMap(map, type, true);
            }
        }

        if (!myUnresolvedMappings.isEmpty()) {
            FileNameMatcher[] unresolvedMappingKeys = myUnresolvedMappings.keySet().toArray(new FileNameMatcher[myUnresolvedMappings.size()]);
            Arrays.sort(unresolvedMappingKeys, new Comparator<FileNameMatcher>() {
                @Override
                public int compare(FileNameMatcher o1, FileNameMatcher o2) {
                    return o1.getPresentableString().compareTo(o2.getPresentableString());
                }
            });

            for (FileNameMatcher fileNameMatcher : unresolvedMappingKeys) {
                Element content = AbstractFileType.writeMapping(myUnresolvedMappings.get(fileNameMatcher), fileNameMatcher, true);
                if (content != null) {
                    map.addContent(content);
                }
            }
        }

        if (!map.getChildren().isEmpty()) {
            state.addContent(map);
        }

        if (!state.getChildren().isEmpty()) {
            state.setAttribute(ATTRIBUTE_VERSION, String.valueOf(VERSION));
        }
        return state;
    }

    private void writeExtensionsMap( Element map,  FileType type, boolean specifyTypeName) {
        List<FileNameMatcher> associations = myPatternsTable.getAssociations(type);
        Set<FileNameMatcher> defaultAssociations = new THashSet<FileNameMatcher>(myInitialAssociations.getAssociations(type));

        for (FileNameMatcher matcher : associations) {
            if (defaultAssociations.contains(matcher)) {
                defaultAssociations.remove(matcher);
            }
            else if (shouldSave(type)) {
                Element content = AbstractFileType.writeMapping(type.getName(), matcher, specifyTypeName);
                if (content != null) {
                    map.addContent(content);
                }
            }
        }

        for (FileNameMatcher matcher : defaultAssociations) {
            Element content = AbstractFileType.writeRemovedMapping(type, matcher, specifyTypeName, isApproved(matcher));
            if (content != null) {
                map.addContent(content);
            }
        }
    }

    private boolean isApproved(FileNameMatcher matcher) {
        Pair<FileType, Boolean> pair = myRemovedMappings.get(matcher);
        return pair != null && pair.getSecond();
    }

    // -------------------------------------------------------------------------
    // Helper methods
    // -------------------------------------------------------------------------

    
    private FileType getFileTypeByName( String name) {
        return mySchemesManager.findSchemeByName(name);
    }

    
    private static List<FileNameMatcher> parse( String semicolonDelimited) {
        if (semicolonDelimited == null) {
            return Collections.emptyList();
        }

        StringTokenizer tokenizer = new StringTokenizer(semicolonDelimited, FileTypeConsumer.EXTENSION_DELIMITER, false);
        ArrayList<FileNameMatcher> list = new ArrayList<FileNameMatcher>();
        while (tokenizer.hasMoreTokens()) {
            list.add(new ExtensionFileNameMatcher(tokenizer.nextToken().trim()));
        }
        return list;
    }

    /**
     * Registers a standard file type. Doesn't notifyListeners any change events.
     */
    private void registerFileTypeWithoutNotification( FileType fileType,  List<FileNameMatcher> matchers, boolean addScheme) {
        if (addScheme) {
            mySchemesManager.addNewScheme(fileType, true);
        }
        for (FileNameMatcher matcher : matchers) {
            myPatternsTable.addAssociation(matcher, fileType);
            myInitialAssociations.addAssociation(matcher, fileType);
        }

        if (fileType instanceof FileTypeIdentifiableByVirtualFile) {
            mySpecialFileTypes.add((FileTypeIdentifiableByVirtualFile)fileType);
        }
    }

    private void bindUnresolvedMappings( FileType fileType) {
        for (FileNameMatcher matcher : new THashSet<FileNameMatcher>(myUnresolvedMappings.keySet())) {
            String name = myUnresolvedMappings.get(matcher);
            if (Comparing.equal(name, fileType.getName())) {
                myPatternsTable.addAssociation(matcher, fileType);
                myUnresolvedMappings.remove(matcher);
            }
        }

        for (FileNameMatcher matcher : new THashSet<FileNameMatcher>(myUnresolvedRemovedMappings.keySet())) {
            Trinity<String, String, Boolean> trinity = myUnresolvedRemovedMappings.get(matcher);
            if (Comparing.equal(trinity.getFirst(), fileType.getName())) {
                removeAssociation(fileType, matcher, false);
                myUnresolvedRemovedMappings.remove(matcher);
            }
        }
    }

    
    private FileType loadFileType( Element typeElement, boolean isDefault) {
        String fileTypeName = typeElement.getAttributeValue(ATTRIBUTE_NAME);
        String fileTypeDescr = typeElement.getAttributeValue(ATTRIBUTE_DESCRIPTION);
        String iconPath = typeElement.getAttributeValue("icon");

        String extensionsStr = StringUtil.nullize(typeElement.getAttributeValue("extensions"));
        if (isDefault && extensionsStr != null) {
            // todo support wildcards
            extensionsStr = filterAlreadyRegisteredExtensions(extensionsStr);
        }

        FileType type = isDefault ? getFileTypeByName(fileTypeName) : null;
        if (type != null) {
            return type;
        }

        Element element = typeElement.getChild(AbstractFileType.ELEMENT_HIGHLIGHTING);
        if (element == null) {
            for (CustomFileTypeFactory factory : CustomFileTypeFactory.EP_NAME.getExtensions()) {
                type = factory.createFileType(typeElement);
                if (type != null) {
                    break;
                }
            }

            if (type == null) {
                type = new UserBinaryFileType();
            }
        }
        else {
            SyntaxTable table = AbstractFileType.readSyntaxTable(element);
            type = new AbstractFileType(table);
            ((AbstractFileType)type).initSupport();
        }

        setFileTypeAttributes((UserFileType)type, fileTypeName, fileTypeDescr, iconPath);
        registerFileTypeWithoutNotification(type, parse(extensionsStr), isDefault);

        if (isDefault) {
            myDefaultTypes.add(type);
            if (type instanceof ExternalizableFileType) {
                ((ExternalizableFileType)type).markDefaultSettings();
            }
        }
        else {
            Element extensions = typeElement.getChild(AbstractFileType.ELEMENT_EXTENSION_MAP);
            if (extensions != null) {
                for (Pair<FileNameMatcher, String> association : AbstractFileType.readAssociations(extensions)) {
                    associate(type, association.getFirst(), false);
                }

                for (Trinity<FileNameMatcher, String, Boolean> removedAssociation : AbstractFileType.readRemovedAssociations(extensions)) {
                    removeAssociation(type, removedAssociation.getFirst(), false);
                }
            }
        }
        return type;
    }

    
    private String filterAlreadyRegisteredExtensions( String semicolonDelimited) {
        StringTokenizer tokenizer = new StringTokenizer(semicolonDelimited, FileTypeConsumer.EXTENSION_DELIMITER, false);
        StringBuilder builder = null;
        while (tokenizer.hasMoreTokens()) {
            String extension = tokenizer.nextToken().trim();
            if (getFileTypeByExtension(extension) == UnknownFileType.INSTANCE) {
                if (builder == null) {
                    builder = new StringBuilder();
                }
                else if (builder.length() > 0) {
                    builder.append(FileTypeConsumer.EXTENSION_DELIMITER);
                }
                builder.append(extension);
            }
        }
        return builder == null ? null : builder.toString();
    }

    private static void setFileTypeAttributes( UserFileType fileType,  String name,  String description,  String iconPath) {
        if (!StringUtil.isEmptyOrSpaces(iconPath)) {
            fileType.setIcon(IconLoader.getIcon(iconPath));
        }
        if (description != null) {
            fileType.setDescription(description);
        }
        if (name != null) {
            fileType.setName(name);
        }
    }

    private static boolean shouldSave(FileType fileType) {
        return fileType != FileTypes.UNKNOWN && !fileType.isReadOnly();
    }

    // -------------------------------------------------------------------------
    // Setup
    // -------------------------------------------------------------------------

    @Override
    
    public String getComponentName() {
        return getFileTypeComponentName();
    }

    public static String getFileTypeComponentName() {
        return PlatformUtils.isIdeaCommunity() ? "CommunityFileTypes" : "FileTypeManager";
    }

    
    FileTypeAssocTable getExtensionMap() {
        return myPatternsTable;
    }

    void setPatternsTable( Set<FileType> fileTypes,  FileTypeAssocTable<FileType> assocTable) {
        fireBeforeFileTypesChanged();
        for (FileType existing : getRegisteredFileTypes()) {
            if (!fileTypes.contains(existing)) {
                mySchemesManager.removeScheme(existing);
            }
        }
        for (FileType fileType : fileTypes) {
            mySchemesManager.addNewScheme(fileType, true);
            if (fileType instanceof AbstractFileType) {
                ((AbstractFileType)fileType).initSupport();
            }
        }
        myPatternsTable = assocTable.copy();
        fireFileTypesChanged();
    }

    public void associate(FileType fileType, FileNameMatcher matcher, boolean fireChange) {
        if (!myPatternsTable.isAssociatedWith(fileType, matcher)) {
            if (fireChange) {
                fireBeforeFileTypesChanged();
            }
            myPatternsTable.addAssociation(matcher, fileType);
            if (fireChange) {
                fireFileTypesChanged();
            }
        }
    }

    public void removeAssociation(FileType fileType, FileNameMatcher matcher, boolean fireChange) {
        if (myPatternsTable.isAssociatedWith(fileType, matcher)) {
            if (fireChange) {
                fireBeforeFileTypesChanged();
            }
            myPatternsTable.removeAssociation(matcher, fileType);
            if (fireChange) {
                fireFileTypesChanged();
            }
        }
    }

    @Override
    
    public FileType getKnownFileTypeOrAssociate( VirtualFile file) {
        FileType type = file.getFileType();
        if (type != UnknownFileType.INSTANCE) return type;
        return FileTypeChooser.getKnownFileTypeOrAssociate(file.getName());
    }

    @Override
    public FileType getKnownFileTypeOrAssociate( VirtualFile file,  Project project) {
        return FileTypeChooser.getKnownFileTypeOrAssociate(file, project);
    }

    private void registerReDetectedMappings(StandardFileType pair) {
        FileType fileType = pair.fileType;
        if (fileType == PlainTextFileType.INSTANCE) return;
        for (FileNameMatcher matcher : pair.matchers) {
            registerReDetectedMapping(fileType, matcher);
            if (matcher instanceof ExtensionFileNameMatcher) {
                // also check exact file name matcher
                ExtensionFileNameMatcher extMatcher = (ExtensionFileNameMatcher)matcher;
                registerReDetectedMapping(fileType, new ExactFileNameMatcher("." + extMatcher.getExtension()));
            }
        }
    }

    private void registerReDetectedMapping( FileType fileType,  FileNameMatcher matcher) {
        String typeName = myUnresolvedMappings.get(matcher);
        if (typeName != null && !typeName.equals(fileType.getName())) {
            Trinity<String, String, Boolean> trinity = myUnresolvedRemovedMappings.get(matcher);
            myRemovedMappings.put(matcher, Pair.create(fileType, trinity != null && trinity.third));
            myUnresolvedMappings.remove(matcher);
        }
    }

    Map<FileNameMatcher, Pair<FileType, Boolean>> getRemovedMappings() {
        return myRemovedMappings;
    }

    
    void clearForTests() {
        myStandardFileTypes.clear();
        myUnresolvedMappings.clear();
        mySchemesManager.clearAllSchemes();
    }

    @Override
    public void dispose() {
        LOG.info("FileTypeManager: "+ counterAutoDetect +" auto-detected files\nElapsed time on auto-detect: "+elapsedAutoDetect+" ms");
    }
}
