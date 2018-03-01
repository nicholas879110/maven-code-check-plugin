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

/*
 * @author max
 */
package com.gome.maven.psi.stubs;

import com.gome.maven.lang.Language;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.application.ModalityState;
import com.gome.maven.openapi.components.*;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.extensions.Extensions;
import com.gome.maven.openapi.fileEditor.FileDocumentManager;
import com.gome.maven.openapi.fileTypes.FileType;
import com.gome.maven.openapi.fileTypes.LanguageFileType;
import com.gome.maven.openapi.progress.ProgressManager;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.NotNullComputable;
import com.gome.maven.openapi.util.io.FileUtil;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.openapi.vfs.newvfs.ManagingFS;
import com.gome.maven.openapi.vfs.newvfs.persistent.PersistentFS;
import com.gome.maven.psi.LanguageSubstitutors;
import com.gome.maven.psi.PsiDocumentManager;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.psi.impl.source.PsiFileImpl;
import com.gome.maven.psi.impl.source.PsiFileWithStubSupport;
import com.gome.maven.psi.impl.source.tree.FileElement;
import com.gome.maven.psi.search.GlobalSearchScope;
import com.gome.maven.util.CommonProcessors;
import com.gome.maven.util.Processor;
import com.gome.maven.util.SmartList;
import com.gome.maven.util.containers.ContainerUtil;
import com.gome.maven.util.indexing.*;
import com.gome.maven.util.io.DataExternalizer;
import com.gome.maven.util.io.DataInputOutputUtil;
import gnu.trove.THashMap;
import gnu.trove.TObjectIntHashMap;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;

@State(
        name = "FileBasedIndex",
        storages = {@Storage(file = StoragePathMacros.APP_CONFIG + "/stubIndex.xml", roamingType = RoamingType.DISABLED)}
)
public class StubIndexImpl extends StubIndex implements ApplicationComponent, PersistentStateComponent<StubIndexState> {
    private static final AtomicReference<Boolean> ourForcedClean = new AtomicReference<Boolean>(null);
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.psi.stubs.StubIndexImpl");
    private final Map<StubIndexKey<?,?>, MyIndex<?>> myIndices = new THashMap<StubIndexKey<?,?>, MyIndex<?>>();
    private final TObjectIntHashMap<ID<?, ?>> myIndexIdToVersionMap = new TObjectIntHashMap<ID<?, ?>>();

    private final StubProcessingHelper myStubProcessingHelper;

    private StubIndexState myPreviouslyRegistered;

    public StubIndexImpl(FileBasedIndex fileBasedIndex /* need this to ensure initialization order*/ ) throws IOException {
        final boolean forceClean = Boolean.TRUE == ourForcedClean.getAndSet(Boolean.FALSE);

        final StubIndexExtension<?, ?>[] extensions = Extensions.getExtensions(StubIndexExtension.EP_NAME);
        boolean needRebuild = false;
        for (StubIndexExtension extension : extensions) {
            //noinspection unchecked
            needRebuild |= registerIndexer(extension, forceClean);
        }
        if (needRebuild) {
            if (ApplicationManager.getApplication().isUnitTestMode()) {
                requestRebuild();
            }
            else {
                final Throwable e = new Throwable();
                // avoid direct forceRebuild as it produces dependency cycle (IDEA-105485)
                ApplicationManager.getApplication().invokeLater(
                        new Runnable() {
                            @Override
                            public void run() {
                                forceRebuild(e);
                            }
                        }, ModalityState.NON_MODAL
                );
            }
        }
        dropUnregisteredIndices();

        myStubProcessingHelper = new StubProcessingHelper(fileBasedIndex);
    }

    
    public static StubIndexImpl getInstanceOrInvalidate() {
        if (ourForcedClean.compareAndSet(null, Boolean.TRUE)) {
            return null;
        }
        return (StubIndexImpl)getInstance();
    }

    // todo this seems to be copy-pasted from FileBasedIndex
    private <K> boolean registerIndexer( final StubIndexExtension<K, ?> extension, final boolean forceClean) throws IOException {
        final StubIndexKey<K, ?> indexKey = extension.getKey();
        final int version = extension.getVersion();
        myIndexIdToVersionMap.put(indexKey, version);
        final File versionFile = IndexInfrastructure.getVersionFile(indexKey);
        final boolean versionFileExisted = versionFile.exists();
        final File indexRootDir = IndexInfrastructure.getIndexRootDir(indexKey);
        boolean needRebuild = false;
        if (forceClean || IndexingStamp.versionDiffers(versionFile, version)) {
            final String[] children = indexRootDir.list();
            // rebuild only if there exists what to rebuild
            boolean indexRootHasChildren = children != null && children.length > 0;
            needRebuild = !forceClean && (versionFileExisted || indexRootHasChildren);
            if (needRebuild) {
                LOG.info("Version has changed for stub index " + extension.getKey() + ". The index will be rebuilt.");
            }
            if (indexRootHasChildren) FileUtil.deleteWithRenaming(indexRootDir);
            IndexingStamp.rewriteVersion(versionFile, version); // todo snapshots indices
        }

        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                final MapIndexStorage<K, StubIdList> storage = new MapIndexStorage<K, StubIdList>(
                        IndexInfrastructure.getStorageFile(indexKey),
                        extension.getKeyDescriptor(),
                        new StubIdExternalizer(),
                        extension.getCacheSize(),
                        false,
                        extension instanceof StringStubIndexExtension && ((StringStubIndexExtension)extension).traceKeyHashToVirtualFileMapping()
                );

                final MemoryIndexStorage<K, StubIdList> memStorage = new MemoryIndexStorage<K, StubIdList>(storage);
                myIndices.put(indexKey, new MyIndex<K>(memStorage));
                break;
            }
            catch (IOException e) {
                needRebuild = true;
                onExceptionInstantiatingIndex(version, versionFile, indexRootDir, e);
            } catch (RuntimeException e) {
                //noinspection ThrowableResultOfMethodCallIgnored
                Throwable cause = FileBasedIndexImpl.getCauseToRebuildIndex(e);
                if (cause == null) throw e;
                onExceptionInstantiatingIndex(version, versionFile, indexRootDir, e);
            }
        }
        return needRebuild;
    }

    private static void onExceptionInstantiatingIndex(int version, File versionFile, File indexRootDir, Exception e) throws IOException {
        LOG.info(e);
        FileUtil.deleteWithRenaming(indexRootDir);
        IndexingStamp.rewriteVersion(versionFile, version); // todo snapshots indices
    }

    private static class StubIdExternalizer implements DataExternalizer<StubIdList> {
        @Override
        public void save( final DataOutput out,  final StubIdList value) throws IOException {
            int size = value.size();
            if (size == 0) {
                DataInputOutputUtil.writeINT(out, Integer.MAX_VALUE);
            }
            else if (size == 1) {
                DataInputOutputUtil.writeINT(out, value.get(0)); // most often case
            }
            else {
                DataInputOutputUtil.writeINT(out, -size);
                for(int i = 0; i < size; ++i) {
                    DataInputOutputUtil.writeINT(out, value.get(i));
                }
            }
        }

        
        @Override
        public StubIdList read( final DataInput in) throws IOException {
            int size = DataInputOutputUtil.readINT(in);
            if (size == Integer.MAX_VALUE) {
                return new StubIdList();
            }
            else if (size >= 0) {
                return new StubIdList(size);
            }
            else {
                size = -size;
                int[] result = new int[size];
                for(int i = 0; i < size; ++i) {
                    result[i] = DataInputOutputUtil.readINT(in);
                }
                return new StubIdList(result, size);
            }
        }
    }

    
    @Override
    public <Key, Psi extends PsiElement> Collection<Psi> get( final StubIndexKey<Key, Psi> indexKey,
                                                              final Key key,
                                                              final Project project,
                                                              final GlobalSearchScope scope) {
        return get(indexKey, key, project, scope, null);
    }

    @Override
    public <Key, Psi extends PsiElement> Collection<Psi> get( StubIndexKey<Key, Psi> indexKey,
                                                              Key key,
                                                              Project project,
                                                              GlobalSearchScope scope,
                                                             IdFilter filter) {
        final List<Psi> result = new SmartList<Psi>();
        process(indexKey, key, project, scope, filter, new CommonProcessors.CollectProcessor<Psi>(result));
        return result;
    }

    @Override
    public <Key, Psi extends PsiElement> boolean processElements( StubIndexKey<Key, Psi> indexKey,
                                                                  Key key,
                                                                  Project project,
                                                                  GlobalSearchScope scope,
                                                                 Class<Psi> requiredClass,
                                                                  Processor<? super Psi> processor) {
        return processElements(indexKey, key, project, scope, null, requiredClass, processor);
    }

    @Override
    public <Key, Psi extends PsiElement> boolean processElements( final StubIndexKey<Key, Psi> indexKey,
                                                                  final Key key,
                                                                  final Project project,
                                                                  final GlobalSearchScope scope,
                                                                  IdFilter idFilter,
                                                                  final Class<Psi> requiredClass,
                                                                  final Processor<? super Psi> processor) {
        final FileBasedIndexImpl fileBasedIndex = (FileBasedIndexImpl)FileBasedIndex.getInstance();
        fileBasedIndex.ensureUpToDate(StubUpdatingIndex.INDEX_ID, project, scope);

        final PersistentFS fs = (PersistentFS)ManagingFS.getInstance();

        final MyIndex<Key> index = (MyIndex<Key>)myIndices.get(indexKey);

        try {
            try {
                // disable up-to-date check to avoid locks on attempt to acquire index write lock while holding at the same time the readLock for this index
                FileBasedIndexImpl.disableUpToDateCheckForCurrentThread();
                index.getReadLock().lock();
                final ValueContainer<StubIdList> container = index.getData(key);

                final IdFilter finalIdFilter = idFilter != null ? idFilter : fileBasedIndex.projectIndexableFiles(project);

                return container.forEach(new ValueContainer.ContainerAction<StubIdList>() {
                    @Override
                    public boolean perform(final int id,  final StubIdList value) {
                        ProgressManager.checkCanceled();
                        if (finalIdFilter != null && !finalIdFilter.containsFileId(id)) return true;
                        final VirtualFile file = IndexInfrastructure.findFileByIdIfCached(fs, id);
                        if (file == null || scope != null && !scope.contains(file)) {
                            return true;
                        }
                        return myStubProcessingHelper.processStubsInFile(project, file, value, processor, requiredClass);
                    }

                });
            }
            finally {
                index.getReadLock().unlock();
                FileBasedIndexImpl.enableUpToDateCheckForCurrentThread();
            }
        }
        catch (StorageException e) {
            forceRebuild(e);
        }
        catch (RuntimeException e) {
            final Throwable cause = FileBasedIndexImpl.getCauseToRebuildIndex(e);
            if (cause != null) {
                forceRebuild(cause);
            }
            else {
                throw e;
            }
        } catch (AssertionError ae) {
            forceRebuild(ae);
        }

        return true;
    }

    public void forceRebuild( Throwable e) {
        LOG.info(e);
        FileBasedIndex.getInstance().scheduleRebuild(StubUpdatingIndex.INDEX_ID, e);
    }

    private static void requestRebuild() {
        FileBasedIndex.getInstance().requestRebuild(StubUpdatingIndex.INDEX_ID);
    }

    @Override
    
    public <K> Collection<K> getAllKeys( StubIndexKey<K, ?> indexKey,  Project project) {
        Set<K> allKeys = ContainerUtil.newTroveSet();
        processAllKeys(indexKey, project, new CommonProcessors.CollectProcessor<K>(allKeys));
        return allKeys;
    }

    @Override
    public <K> boolean processAllKeys( StubIndexKey<K, ?> indexKey,  Project project, Processor<K> processor) {
        return processAllKeys(indexKey, processor, GlobalSearchScope.allScope(project), null);
    }

    public <K> boolean processAllKeys( StubIndexKey<K, ?> indexKey,  Processor<K> processor,  GlobalSearchScope scope,  IdFilter idFilter) {

        FileBasedIndex.getInstance().ensureUpToDate(StubUpdatingIndex.INDEX_ID, scope.getProject(), scope);

        final MyIndex<K> index = (MyIndex<K>)myIndices.get(indexKey);
        try {
            return index.processAllKeys(processor, scope, idFilter);
        }
        catch (StorageException e) {
            forceRebuild(e);
        }
        catch (RuntimeException e) {
            final Throwable cause = e.getCause();
            if (cause instanceof IOException || cause instanceof StorageException) {
                forceRebuild(e);
            }
            throw e;
        }
        return true;
    }

    @Override
    
    public String getComponentName() {
        return "Stub.IndexManager";
    }

    @Override
    public void initComponent() {
    }

    @Override
    public void disposeComponent() {
        // This index must be disposed only after StubUpdatingIndex is disposed
        // To ensure this, disposing is done explicitly from StubUpdatingIndex by calling dispose() method
        // do not call this method here to avoid double-disposal
    }

    public void dispose() {
        for (UpdatableIndex index : myIndices.values()) {
            index.dispose();
        }
    }

    public void setDataBufferingEnabled(final boolean enabled) {
        for (UpdatableIndex index : myIndices.values()) {
            final IndexStorage indexStorage = ((MapReduceIndex)index).getStorage();
            ((MemoryIndexStorage)indexStorage).setBufferingEnabled(enabled);
        }
    }

    public void cleanupMemoryStorage() {
        for (UpdatableIndex index : myIndices.values()) {
            final IndexStorage indexStorage = ((MapReduceIndex)index).getStorage();
            index.getWriteLock().lock();
            try {
                ((MemoryIndexStorage)indexStorage).clearMemoryMap();
            }
            finally {
                index.getWriteLock().unlock();
            }
        }
    }


    public void clearAllIndices() {
        for (UpdatableIndex index : myIndices.values()) {
            try {
                index.clear();
            }
            catch (StorageException e) {
                LOG.error(e);
                throw new RuntimeException(e);
            }
        }
    }

    private void dropUnregisteredIndices() {
        final Set<String> indicesToDrop = new HashSet<String>(myPreviouslyRegistered != null? myPreviouslyRegistered.registeredIndices : Collections.<String>emptyList());
        for (ID<?, ?> key : myIndices.keySet()) {
            indicesToDrop.remove(key.toString());
        }

        for (String s : indicesToDrop) {
            FileUtil.delete(IndexInfrastructure.getIndexRootDir(ID.create(s)));
        }
    }

    
    @Override
    public StubIndexState getState() {
        return new StubIndexState(myIndices.keySet());
    }

    @Override
    public void loadState(final StubIndexState state) {
        myPreviouslyRegistered = state;
    }

    public final Lock getWriteLock(StubIndexKey indexKey) {
        return myIndices.get(indexKey).getWriteLock();
    }

    public Collection<StubIndexKey> getAllStubIndexKeys() {
        return Collections.<StubIndexKey>unmodifiableCollection(myIndices.keySet());
    }

    public void flush(StubIndexKey key) throws StorageException {
        final MyIndex<?> index = myIndices.get(key);
        index.flush();
    }

    public <K> void updateIndex( StubIndexKey key, int fileId,  final Map<K, StubIdList> oldValues,  final Map<K, StubIdList> newValues) {
        try {
            final MyIndex<K> index = (MyIndex<K>)myIndices.get(key);
            UpdateData<K, StubIdList> updateData;

            if (MapDiffUpdateData.ourDiffUpdateEnabled) {
                updateData = new MapDiffUpdateData<K, StubIdList>(key) {
                    @Override
                    public void save(int inputId) throws IOException {
                    }

                    @Override
                    protected Map<K, StubIdList> getNewValue() {
                        return newValues;
                    }

                    @Override
                    protected Map<K, StubIdList> getCurrentValue() throws IOException {
                        return oldValues;
                    }
                };
            } else {
                updateData = index.new SimpleUpdateData(key, fileId, newValues, new NotNullComputable<Collection<K>>() {
                    
                    @Override
                    public Collection<K> compute() {
                        return oldValues.keySet();
                    }
                });
            }
            index.updateWithMap(fileId, updateData);
        }
        catch (StorageException e) {
            LOG.info(e);
            requestRebuild();
        }
    }

    private static class MyIndex<K> extends MapReduceIndex<K, StubIdList, Void> {
        public MyIndex(final IndexStorage<K, StubIdList> storage) throws IOException {
            super(null, null, storage);
        }

        @Override
        public void updateWithMap(final int inputId,
                                   UpdateData<K, StubIdList> updateData) throws StorageException {
            super.updateWithMap(inputId, updateData);
        }
    }

    @Override
    protected <Psi extends PsiElement> void reportStubPsiMismatch(Psi psi, VirtualFile file, Class<Psi> requiredClass) {
        if (file == null) {
            super.reportStubPsiMismatch(psi, file, requiredClass);
            return;
        }

        StringWriter writer = new StringWriter();
        //noinspection IOResourceOpenedButNotSafelyClosed
        PrintWriter out = new PrintWriter(writer);

        out.print("Invalid stub element type in index:");
        out.printf("\nfile: %s\npsiElement: %s\nrequiredClass: %s\nactualClass: %s",
                file, psi, requiredClass, psi.getClass());

        FileType fileType = file.getFileType();
        Language language = fileType instanceof LanguageFileType ?
                LanguageSubstitutors.INSTANCE.substituteLanguage(((LanguageFileType)fileType).getLanguage(), file, psi.getProject()) :
                Language.ANY;
        out.printf("\nvirtualFile: size:%s; stamp:%s; modCount:%s; fileType:%s; language:%s",
                file.getLength(), file.getModificationStamp(), file.getModificationCount(),
                fileType.getName(), language.getID());

        Document document = FileDocumentManager.getInstance().getCachedDocument(file);
        if (document != null) {
            boolean committed = PsiDocumentManager.getInstance(psi.getProject()).isCommitted(document);
            boolean saved = !FileDocumentManager.getInstance().isDocumentUnsaved(document);
            out.printf("\ndocument: size:%s; stamp:%s; committed:%s; saved:%s",
                    document.getTextLength(), document.getModificationStamp(), committed, saved);
        }

        PsiFile psiFile = psi.getManager().findFile(file);
        if (psiFile != null) {
            out.printf("\npsiFile: size:%s; stamp:%s; class:%s; language:%s",
                    psiFile.getTextLength(), psiFile.getViewProvider().getModificationStamp(), psiFile.getClass().getName(),
                    psiFile.getLanguage().getID());
        }

        StubTree stub = psiFile instanceof PsiFileWithStubSupport ? ((PsiFileWithStubSupport)psiFile).getStubTree() : null;
        FileElement treeElement = stub == null && psiFile instanceof PsiFileImpl? ((PsiFileImpl)psiFile).getTreeElement() : null;
        if (stub != null) {
            out.printf("\nstubInfo: " + stub.getDebugInfo());
        }
        else if (treeElement != null) {
            out.printf("\nfileAST: size:%s; parsed:%s", treeElement.getTextLength(), treeElement.isParsed());
        }

        out.printf("\nindexing info: " + StubUpdatingIndex.getIndexingStampInfo(file));
        LOG.error(writer.toString());
    }
}
