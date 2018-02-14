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

package com.gome.maven.psi.impl;

import com.gome.maven.lang.PsiBuilderFactory;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.extensions.Extensions;
import com.gome.maven.openapi.fileEditor.FileDocumentManager;
import com.gome.maven.openapi.progress.ProgressIndicatorProvider;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.roots.FileIndexFacade;
import com.gome.maven.openapi.util.Disposer;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.openapi.vfs.VirtualFileFilter;
import com.gome.maven.psi.*;
import com.gome.maven.psi.impl.file.impl.FileManager;
import com.gome.maven.psi.impl.file.impl.FileManagerImpl;
import com.gome.maven.psi.util.PsiModificationTracker;
import com.gome.maven.testFramework.LightVirtualFile;
import com.gome.maven.util.containers.ContainerUtil;
import com.gome.maven.util.messages.MessageBus;
import com.gome.maven.util.messages.Topic;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class PsiManagerImpl extends PsiManagerEx {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.psi.impl.PsiManagerImpl");

    private final Project myProject;
    private final FileIndexFacade myFileIndex;
    private final MessageBus myMessageBus;
    private final PsiModificationTracker myModificationTracker;

    private final FileManager myFileManager;

    private final List<PsiTreeChangePreprocessor> myTreeChangePreprocessors = ContainerUtil.createLockFreeCopyOnWriteList();
    private final List<PsiTreeChangeListener> myTreeChangeListeners = ContainerUtil.createLockFreeCopyOnWriteList();
    private boolean myTreeChangeEventIsFiring = false;

    private boolean myIsDisposed;

    private VirtualFileFilter myAssertOnFileLoadingFilter = VirtualFileFilter.NONE;

    private final AtomicInteger myBatchFilesProcessingModeCount = new AtomicInteger(0);

    public static final Topic<AnyPsiChangeListener> ANY_PSI_CHANGE_TOPIC =
            Topic.create("ANY_PSI_CHANGE_TOPIC", AnyPsiChangeListener.class, Topic.BroadcastDirection.TO_PARENT);

    public PsiManagerImpl(Project project,
                          FileDocumentManager fileDocumentManager,
                          PsiBuilderFactory psiBuilderFactory,
                          FileIndexFacade fileIndex,
                          MessageBus messageBus,
                          PsiModificationTracker modificationTracker) {
        myProject = project;
        myFileIndex = fileIndex;
        myMessageBus = messageBus;
        myModificationTracker = modificationTracker;

        //We need to initialize PsiBuilderFactory service so it won't initialize under PsiLock from ChameleonTransform
        @SuppressWarnings({"UnusedDeclaration", "UnnecessaryLocalVariable"}) Object used = psiBuilderFactory;

        boolean isProjectDefault = project.isDefault();

        myFileManager = isProjectDefault ? new EmptyFileManager(this) : new FileManagerImpl(this, fileDocumentManager, fileIndex);

        myTreeChangePreprocessors.add((PsiTreeChangePreprocessor)modificationTracker);
        Collections.addAll(myTreeChangePreprocessors, Extensions.getExtensions(PsiTreeChangePreprocessor.EP_NAME, myProject));

        Disposer.register(project, new Disposable() {
            @Override
            public void dispose() {
                myIsDisposed = true;
            }
        });
    }

    @Override
    public boolean isDisposed() {
        return myIsDisposed;
    }

    @Override
    public void dropResolveCaches() {
        FileManager fileManager = myFileManager;
        if (fileManager instanceof FileManagerImpl) { // mock tests
            ((FileManagerImpl)fileManager).processQueue();
        }
        beforeChange(true);
        beforeChange(false);
    }

    @Override
    public boolean isInProject( PsiElement element) {
        if (element instanceof PsiDirectoryContainer) {
            PsiDirectory[] dirs = ((PsiDirectoryContainer)element).getDirectories();
            for (PsiDirectory dir : dirs) {
                if (!isInProject(dir)) return false;
            }
            return true;
        }

        PsiFile file = element.getContainingFile();
        VirtualFile virtualFile = null;
        if (file != null) {
            virtualFile = file.getViewProvider().getVirtualFile();
        }
        else if (element instanceof PsiFileSystemItem) {
            virtualFile = ((PsiFileSystemItem)element).getVirtualFile();
        }
        if (file != null && file.isPhysical() && virtualFile instanceof LightVirtualFile) return true;

        if (virtualFile != null) {
            return myFileIndex.isInContent(virtualFile);
        }
        return false;
    }

    public void setAssertOnFileLoadingFilter( VirtualFileFilter filter,  Disposable parentDisposable) {
        // Find something to ensure there's no changed files waiting to be processed in repository indices.
        myAssertOnFileLoadingFilter = filter;
        Disposer.register(parentDisposable, new Disposable() {
            @Override
            public void dispose() {
                myAssertOnFileLoadingFilter = VirtualFileFilter.NONE;
            }
        });
    }

    @Override
    public boolean isAssertOnFileLoading( VirtualFile file) {
        return myAssertOnFileLoadingFilter.accept(file);
    }

    @Override
    
    public Project getProject() {
        return myProject;
    }

    @Override
    
    public FileManager getFileManager() {
        return myFileManager;
    }

    @Override
    public boolean areElementsEquivalent(PsiElement element1, PsiElement element2) {
        ProgressIndicatorProvider.checkCanceled(); // We hope this method is being called often enough to cancel daemon processes smoothly

        if (element1 == element2) return true;
        if (element1 == null || element2 == null) {
            return false;
        }

        return element1.equals(element2) || element1.isEquivalentTo(element2) || element2.isEquivalentTo(element1);
    }

    @Override
    public PsiFile findFile( VirtualFile file) {
        ProgressIndicatorProvider.checkCanceled();
        return myFileManager.findFile(file);
    }

    @Override
    public FileViewProvider findViewProvider( VirtualFile file) {
        ProgressIndicatorProvider.checkCanceled();
        return myFileManager.findViewProvider(file);
    }

    @Override
    public PsiDirectory findDirectory( VirtualFile file) {
        ProgressIndicatorProvider.checkCanceled();
        return myFileManager.findDirectory(file);
    }

    @Override
    public void reloadFromDisk( PsiFile file) {
        myFileManager.reloadFromDisk(file);
    }

    @Override
    public void addPsiTreeChangeListener( PsiTreeChangeListener listener) {
        myTreeChangeListeners.add(listener);
    }

    @Override
    public void addPsiTreeChangeListener( final PsiTreeChangeListener listener,  Disposable parentDisposable) {
        addPsiTreeChangeListener(listener);
        Disposer.register(parentDisposable, new Disposable() {
            @Override
            public void dispose() {
                removePsiTreeChangeListener(listener);
            }
        });
    }

    @Override
    public void removePsiTreeChangeListener( PsiTreeChangeListener listener) {
        myTreeChangeListeners.remove(listener);
    }

    @Override
    public void beforeChildAddition( PsiTreeChangeEventImpl event) {
        beforeChange(true);
        event.setCode(PsiTreeChangeEventImpl.PsiEventType.BEFORE_CHILD_ADDITION);
        if (LOG.isDebugEnabled()) {
            LOG.debug(
                    "beforeChildAddition: parent = " + event.getParent()
            );
        }
        fireEvent(event);
    }

    @Override
    public void beforeChildRemoval( PsiTreeChangeEventImpl event) {
        beforeChange(true);
        event.setCode(PsiTreeChangeEventImpl.PsiEventType.BEFORE_CHILD_REMOVAL);
        if (LOG.isDebugEnabled()) {
            LOG.debug(
                    "beforeChildRemoval: child = " + event.getChild()
                            + ", parent = " + event.getParent()
            );
        }
        fireEvent(event);
    }

    @Override
    public void beforeChildReplacement( PsiTreeChangeEventImpl event) {
        beforeChange(true);
        event.setCode(PsiTreeChangeEventImpl.PsiEventType.BEFORE_CHILD_REPLACEMENT);
        if (LOG.isDebugEnabled()) {
            LOG.debug(
                    "beforeChildReplacement: oldChild = " + event.getOldChild()
                            + ", parent = " + event.getParent()
            );
        }
        fireEvent(event);
    }

    public void beforeChildrenChange( PsiTreeChangeEventImpl event) {
        beforeChange(true);
        event.setCode(PsiTreeChangeEventImpl.PsiEventType.BEFORE_CHILDREN_CHANGE);
        if (LOG.isDebugEnabled()) {
            LOG.debug("beforeChildrenChange: parent = " + event.getParent());
        }
        fireEvent(event);
    }

    public void beforeChildMovement( PsiTreeChangeEventImpl event) {
        beforeChange(true);
        event.setCode(PsiTreeChangeEventImpl.PsiEventType.BEFORE_CHILD_MOVEMENT);
        if (LOG.isDebugEnabled()) {
            LOG.debug(
                    "beforeChildMovement: child = " + event.getChild()
                            + ", oldParent = " + event.getOldParent()
                            + ", newParent = " + event.getNewParent()
            );
        }
        fireEvent(event);
    }

    public void beforePropertyChange( PsiTreeChangeEventImpl event) {
        beforeChange(true);
        event.setCode(PsiTreeChangeEventImpl.PsiEventType.BEFORE_PROPERTY_CHANGE);
        if (LOG.isDebugEnabled()) {
            LOG.debug(
                    "beforePropertyChange: element = " + event.getElement()
                            + ", propertyName = " + event.getPropertyName()
                            + ", oldValue = " + event.getOldValue()
            );
        }
        fireEvent(event);
    }

    public void childAdded( PsiTreeChangeEventImpl event) {
        event.setCode(PsiTreeChangeEventImpl.PsiEventType.CHILD_ADDED);
        if (LOG.isDebugEnabled()) {
            LOG.debug(
                    "childAdded: child = " + event.getChild()
                            + ", parent = " + event.getParent()
            );
        }
        fireEvent(event);
        afterChange(true);
    }

    public void childRemoved( PsiTreeChangeEventImpl event) {
        event.setCode(PsiTreeChangeEventImpl.PsiEventType.CHILD_REMOVED);
        if (LOG.isDebugEnabled()) {
            LOG.debug(
                    "childRemoved: child = " + event.getChild() + ", parent = " + event.getParent()
            );
        }
        fireEvent(event);
        afterChange(true);
    }

    public void childReplaced( PsiTreeChangeEventImpl event) {
        event.setCode(PsiTreeChangeEventImpl.PsiEventType.CHILD_REPLACED);
        if (LOG.isDebugEnabled()) {
            LOG.debug(
                    "childReplaced: oldChild = " + event.getOldChild()
                            + ", newChild = " + event.getNewChild()
                            + ", parent = " + event.getParent()
            );
        }
        fireEvent(event);
        afterChange(true);
    }

    public void childMoved( PsiTreeChangeEventImpl event) {
        event.setCode(PsiTreeChangeEventImpl.PsiEventType.CHILD_MOVED);
        if (LOG.isDebugEnabled()) {
            LOG.debug(
                    "childMoved: child = " + event.getChild()
                            + ", oldParent = " + event.getOldParent()
                            + ", newParent = " + event.getNewParent()
            );
        }
        fireEvent(event);
        afterChange(true);
    }

    public void childrenChanged( PsiTreeChangeEventImpl event) {
        event.setCode(PsiTreeChangeEventImpl.PsiEventType.CHILDREN_CHANGED);
        if (LOG.isDebugEnabled()) {
            LOG.debug(
                    "childrenChanged: parent = " + event.getParent()
            );
        }
        fireEvent(event);
        afterChange(true);
    }

    public void propertyChanged( PsiTreeChangeEventImpl event) {
        event.setCode(PsiTreeChangeEventImpl.PsiEventType.PROPERTY_CHANGED);
        if (LOG.isDebugEnabled()) {
            LOG.debug(
                    "propertyChanged: element = " + event.getElement()
                            + ", propertyName = " + event.getPropertyName()
                            + ", oldValue = " + event.getOldValue()
                            + ", newValue = " + event.getNewValue()
            );
        }
        fireEvent(event);
        afterChange(true);
    }

    public void addTreeChangePreprocessor( PsiTreeChangePreprocessor preprocessor) {
        myTreeChangePreprocessors.add(preprocessor);
    }

    public void removeTreeChangePreprocessor( PsiTreeChangePreprocessor preprocessor) {
        myTreeChangePreprocessors.remove(preprocessor);
    }

    private void fireEvent( PsiTreeChangeEventImpl event) {
        boolean isRealTreeChange = event.getCode() != PsiTreeChangeEventImpl.PsiEventType.PROPERTY_CHANGED
                && event.getCode() != PsiTreeChangeEventImpl.PsiEventType.BEFORE_PROPERTY_CHANGE;

        PsiFile file = event.getFile();
        if (file == null || file.isPhysical()) {
            ApplicationManager.getApplication().assertWriteAccessAllowed();
        }
        if (isRealTreeChange) {
            LOG.assertTrue(!myTreeChangeEventIsFiring, "Changes to PSI are not allowed inside event processing");
            myTreeChangeEventIsFiring = true;
        }
        try {
            for (PsiTreeChangePreprocessor preprocessor : myTreeChangePreprocessors) {
                preprocessor.treeChanged(event);
            }

            for (PsiTreeChangeListener listener : myTreeChangeListeners) {
                try {
                    switch (event.getCode()) {
                        case BEFORE_CHILD_ADDITION:
                            listener.beforeChildAddition(event);
                            break;

                        case BEFORE_CHILD_REMOVAL:
                            listener.beforeChildRemoval(event);
                            break;

                        case BEFORE_CHILD_REPLACEMENT:
                            listener.beforeChildReplacement(event);
                            break;

                        case BEFORE_CHILD_MOVEMENT:
                            listener.beforeChildMovement(event);
                            break;

                        case BEFORE_CHILDREN_CHANGE:
                            listener.beforeChildrenChange(event);
                            break;

                        case BEFORE_PROPERTY_CHANGE:
                            listener.beforePropertyChange(event);
                            break;

                        case CHILD_ADDED:
                            listener.childAdded(event);
                            break;

                        case CHILD_REMOVED:
                            listener.childRemoved(event);
                            break;

                        case CHILD_REPLACED:
                            listener.childReplaced(event);
                            break;

                        case CHILD_MOVED:
                            listener.childMoved(event);
                            break;

                        case CHILDREN_CHANGED:
                            listener.childrenChanged(event);
                            break;

                        case PROPERTY_CHANGED:
                            listener.propertyChanged(event);
                            break;
                    }
                }
                catch (Exception e) {
                    LOG.error(e);
                }
            }
        }
        finally {
            if (isRealTreeChange) {
                myTreeChangeEventIsFiring = false;
            }
        }
    }

    @Override
    public void registerRunnableToRunOnChange( final Runnable runnable) {
        myMessageBus.connect().subscribe(ANY_PSI_CHANGE_TOPIC, new AnyPsiChangeListener.Adapter() {
            @Override
            public void beforePsiChanged(boolean isPhysical) {
                if (isPhysical) runnable.run();
            }
        });
    }

    @Override
    public void registerRunnableToRunOnAnyChange( final Runnable runnable) { // includes non-physical changes
        myMessageBus.connect().subscribe(ANY_PSI_CHANGE_TOPIC, new AnyPsiChangeListener.Adapter() {
            @Override
            public void beforePsiChanged(boolean isPhysical) {
                runnable.run();
            }
        });
    }

    @Override
    public void registerRunnableToRunAfterAnyChange( final Runnable runnable) { // includes non-physical changes
        myMessageBus.connect().subscribe(ANY_PSI_CHANGE_TOPIC, new AnyPsiChangeListener.Adapter() {
            @Override
            public void afterPsiChanged(boolean isPhysical) {
                runnable.run();
            }
        });
    }

    @Override
    public void beforeChange(boolean isPhysical) {
        myMessageBus.syncPublisher(ANY_PSI_CHANGE_TOPIC).beforePsiChanged(isPhysical);
    }

    @Override
    public void afterChange(boolean isPhysical) {
        myMessageBus.syncPublisher(ANY_PSI_CHANGE_TOPIC).afterPsiChanged(isPhysical);
    }

    @Override
    
    public PsiModificationTracker getModificationTracker() {
        return myModificationTracker;
    }

    @Override
    public void startBatchFilesProcessingMode() {
        myBatchFilesProcessingModeCount.incrementAndGet();
    }

    @Override
    public void finishBatchFilesProcessingMode() {
        myBatchFilesProcessingModeCount.decrementAndGet();
        LOG.assertTrue(myBatchFilesProcessingModeCount.get() >= 0);
    }

    @Override
    public boolean isBatchFilesProcessingMode() {
        return myBatchFilesProcessingModeCount.get() > 0;
    }

    public void cleanupForNextTest() {
        assert ApplicationManager.getApplication().isUnitTestMode();
        myFileManager.cleanupForNextTest();
    }
}
