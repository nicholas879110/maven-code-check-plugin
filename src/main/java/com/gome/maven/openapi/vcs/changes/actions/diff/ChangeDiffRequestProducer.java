/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.gome.maven.openapi.vcs.changes.actions.diff;

import com.gome.maven.CommonBundle;
import com.gome.maven.diff.DiffContentFactory;
import com.gome.maven.diff.DiffRequestFactory;
import com.gome.maven.diff.DiffRequestFactoryImpl;
import com.gome.maven.diff.chains.DiffRequestProducer;
import com.gome.maven.diff.chains.DiffRequestProducerException;
import com.gome.maven.diff.contents.DiffContent;
import com.gome.maven.diff.impl.DiffViewerWrapper;
import com.gome.maven.diff.requests.DiffRequest;
import com.gome.maven.diff.requests.ErrorDiffRequest;
import com.gome.maven.diff.requests.SimpleDiffRequest;
import com.gome.maven.diff.util.DiffUserDataKeys;
import com.gome.maven.diff.util.Side;
import com.gome.maven.openapi.actionSystem.DataProvider;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.diff.impl.GenericDataProvider;
import com.gome.maven.openapi.fileTypes.FileTypes;
import com.gome.maven.openapi.fileTypes.ex.FileTypeChooser;
import com.gome.maven.openapi.progress.ProcessCanceledException;
import com.gome.maven.openapi.progress.ProgressIndicator;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.ui.Messages;
import com.gome.maven.openapi.util.Comparing;
import com.gome.maven.openapi.util.Key;
import com.gome.maven.openapi.util.Ref;
import com.gome.maven.openapi.util.UserDataHolder;
import com.gome.maven.openapi.util.io.FileUtil;
import com.gome.maven.openapi.util.io.FileUtilRt;
import com.gome.maven.openapi.vcs.*;
import com.gome.maven.openapi.vcs.changes.*;
import com.gome.maven.openapi.vcs.merge.MergeData;
import com.gome.maven.openapi.vfs.CharsetToolkit;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.util.ThreeState;
import com.gome.maven.util.containers.ContainerUtil;
import com.gome.maven.util.ui.UIUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ChangeDiffRequestProducer implements DiffRequestProducer {
    private static final Logger LOG = Logger.getInstance(ChangeDiffRequestProducer.class);

    private static Key<List<String>> CONTEXT_KEY = Key.create("Diff.ChangeDiffRequestPresentableContextKey");
    public static Key<Change> CHANGE_KEY = Key.create("DiffRequestPresentable.Change");

     private final Project myProject;
     private final Change myChange;
     private final Map<Key, Object> myChangeContext;

    private ChangeDiffRequestProducer( Project project,  Change change,  Map<Key, Object> changeContext) {
        myChange = change;
        myProject = project;
        myChangeContext = changeContext;
    }

    
    public Change getChange() {
        return myChange;
    }

    
    public Project getProject() {
        return myProject;
    }

    
    @Override
    public String getName() {
        return ChangesUtil.getFilePath(myChange).getPath();
    }

    public static boolean isEquals( Change change1,  Change change2) {
        for (ChangeDiffViewerWrapperProvider provider : ChangeDiffViewerWrapperProvider.EP_NAME.getExtensions()) {
            ThreeState equals = provider.isEquals(change1, change2);
            if (equals == ThreeState.NO) return false;
        }
        for (ChangeDiffRequestProvider provider : ChangeDiffRequestProvider.EP_NAME.getExtensions()) {
            ThreeState equals = provider.isEquals(change1, change2);
            if (equals == ThreeState.YES) return true;
            if (equals == ThreeState.NO) return false;
        }

        if (!Comparing.equal(change1.getClass(), change2.getClass())) return false;
        if (!Comparing.equal(change1.getBeforeRevision(), change2.getBeforeRevision())) return false;
        if (!Comparing.equal(change1.getAfterRevision(), change2.getAfterRevision())) return false;

        return true;
    }

    
    public static ChangeDiffRequestProducer create( Project project,  Change change) {
        return create(project, change, Collections.<Key, Object>emptyMap());
    }

    
    public static ChangeDiffRequestProducer create( Project project,
                                                    Change change,
                                                    Map<Key, Object> changeContext) {
        if (!canCreate(project, change)) return null;
        return new ChangeDiffRequestProducer(project, change, changeContext);
    }

    public static boolean canCreate( Project project,  Change change) {
        for (ChangeDiffViewerWrapperProvider provider : ChangeDiffViewerWrapperProvider.EP_NAME.getExtensions()) {
            if (provider.canCreate(project, change)) return true;
        }
        for (ChangeDiffRequestProvider provider : ChangeDiffRequestProvider.EP_NAME.getExtensions()) {
            if (provider.canCreate(project, change)) return true;
        }

        ContentRevision bRev = change.getBeforeRevision();
        ContentRevision aRev = change.getAfterRevision();

        if (bRev == null && aRev == null) return false;
        if (bRev != null && bRev.getFile().isDirectory()) return false;
        if (aRev != null && aRev.getFile().isDirectory()) return false;

        return true;
    }

    
    @Override
    public DiffRequest process( UserDataHolder context,
                                ProgressIndicator indicator) throws DiffRequestProducerException, ProcessCanceledException {
        try {
            return loadCurrentContents(context, indicator);
        }
        catch (ProcessCanceledException e) {
            throw e;
        }
        catch (DiffRequestProducerException e) {
            throw e;
        }
        catch (Exception e) {
            LOG.warn(e);
            throw new DiffRequestProducerException(e.getMessage());
        }
    }

    
    protected DiffRequest loadCurrentContents( UserDataHolder context,
                                               ProgressIndicator indicator) throws DiffRequestProducerException {
        DiffRequestProducerException wrapperException = null;
        DiffRequestProducerException requestException = null;

        DiffViewerWrapper wrapper = null;
        try {
            for (ChangeDiffViewerWrapperProvider provider : ChangeDiffViewerWrapperProvider.EP_NAME.getExtensions()) {
                if (provider.canCreate(myProject, myChange)) {
                    wrapper = provider.process(this, context, indicator);
                    break;
                }
            }
        }
        catch (DiffRequestProducerException e) {
            wrapperException = e;
        }

        DiffRequest request = null;
        try {
            for (ChangeDiffRequestProvider provider : ChangeDiffRequestProvider.EP_NAME.getExtensions()) {
                if (provider.canCreate(myProject, myChange)) {
                    request = provider.process(this, context, indicator);
                    break;
                }
            }
            if (request == null) request = createRequest(myProject, myChange, context, indicator);
        }
        catch (DiffRequestProducerException e) {
            requestException = e;
        }

        if (requestException != null && wrapperException != null) {
            String message = requestException.getMessage() + "\n\n" + wrapperException.getMessage();
            throw new DiffRequestProducerException(message);
        }
        if (requestException != null) {
            request = new ErrorDiffRequest(getRequestTitle(myChange), requestException);
            LOG.info("Request: " + requestException.getMessage());
        }
        if (wrapperException != null) {
            LOG.info("Wrapper: " + wrapperException.getMessage());
        }

        request.putUserData(CHANGE_KEY, myChange);
        request.putUserData(DiffViewerWrapper.KEY, wrapper);

        for (Map.Entry<Key, Object> entry : myChangeContext.entrySet()) {
            request.putUserData(entry.getKey(), entry.getValue());
        }

        DataProvider dataProvider = request.getUserData(DiffUserDataKeys.DATA_PROVIDER);
        if (dataProvider == null) {
            dataProvider = new GenericDataProvider();
            request.putUserData(DiffUserDataKeys.DATA_PROVIDER, dataProvider);
        }
        if (dataProvider instanceof GenericDataProvider) ((GenericDataProvider)dataProvider).putData(VcsDataKeys.CURRENT_CHANGE, myChange);

        return request;
    }

    
    private static DiffRequest createRequest( Project project,
                                              Change change,
                                              UserDataHolder context,
                                              ProgressIndicator indicator) throws DiffRequestProducerException {
        if (ChangesUtil.isTextConflictingChange(change)) { // three side diff
            // FIXME: This part is ugly as a VCS merge subsystem itself.

            FilePath path = ChangesUtil.getFilePath(change);
            VirtualFile file = path.getVirtualFile();
            if (file == null) {
                path.hardRefresh();
                file = path.getVirtualFile();
            }
            if (file == null) throw new DiffRequestProducerException("Can't show merge conflict - file not found");

            if (project == null) {
                throw new DiffRequestProducerException("Can't show merge conflict - project is unknown");
            }
            final AbstractVcs vcs = ChangesUtil.getVcsForChange(change, project);
            if (vcs == null || vcs.getMergeProvider() == null) {
                throw new DiffRequestProducerException("Can't show merge conflict - operation nos supported");
            }
            try {
                // FIXME: loadRevisions() can call runProcessWithProgressSynchronously() inside
                final Ref<Throwable> exceptionRef = new Ref<Throwable>();
                final Ref<MergeData> mergeDataRef = new Ref<MergeData>();
                final VirtualFile finalFile = file;
                UIUtil.invokeAndWaitIfNeeded(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            mergeDataRef.set(vcs.getMergeProvider().loadRevisions(finalFile));
                        }
                        catch (VcsException e) {
                            exceptionRef.set(e);
                        }
                    }
                });
                if (!exceptionRef.isNull()) {
                    Throwable e = exceptionRef.get();
                    if (e instanceof VcsException) throw (VcsException)e;
                    if (e instanceof Error) throw (Error)e;
                    if (e instanceof RuntimeException) throw (RuntimeException)e;
                    throw new RuntimeException(e);
                }
                MergeData mergeData = mergeDataRef.get();

                ContentRevision bRev = change.getBeforeRevision();
                ContentRevision aRev = change.getAfterRevision();
                String beforeRevisionTitle = getRevisionTitle(bRev, "Your version");
                String afterRevisionTitle = getRevisionTitle(aRev, "Server version");

                String title = DiffRequestFactory.getInstance().getTitle(file);
                List<String> titles = ContainerUtil.list(beforeRevisionTitle, "Base Version", afterRevisionTitle);

                // Yep, we hope that it's a text file. And that charset wasn't changed.
                List<DiffContent> contents = ContainerUtil.list(
                        createTextContent(mergeData.CURRENT, file),
                        createTextContent(mergeData.ORIGINAL, file),
                        createTextContent(mergeData.LAST, file)
                );

                SimpleDiffRequest request = new SimpleDiffRequest(title, contents, titles);

                boolean bRevCurrent = bRev instanceof CurrentContentRevision;
                boolean aRevCurrent = aRev instanceof CurrentContentRevision;
                if (bRevCurrent && !aRevCurrent) request.putUserData(DiffUserDataKeys.MASTER_SIDE, Side.LEFT);
                if (!bRevCurrent && aRevCurrent) request.putUserData(DiffUserDataKeys.MASTER_SIDE, Side.RIGHT);

                return request;
            }
            catch (VcsException e) {
                LOG.info(e);
                throw new DiffRequestProducerException(e);
            }
        }
        else {
            ContentRevision bRev = change.getBeforeRevision();
            ContentRevision aRev = change.getAfterRevision();

            if (bRev == null && aRev == null) {
                LOG.warn("Both revision contents are empty");
                throw new DiffRequestProducerException("Bad revisions contents");
            }
            if (bRev != null) checkContentRevision(project, bRev, context, indicator);
            if (aRev != null) checkContentRevision(project, aRev, context, indicator);

            String title = getRequestTitle(change);

            indicator.setIndeterminate(true);
            DiffContent content1 = createContent(project, bRev, context, indicator);
            DiffContent content2 = createContent(project, aRev, context, indicator);

            String beforeRevisionTitle = getRevisionTitle(bRev, "Base version");
            String afterRevisionTitle = getRevisionTitle(aRev, "Your version");

            return new SimpleDiffRequest(title, content1, content2, beforeRevisionTitle, afterRevisionTitle);
        }
    }

    
    public static String getRequestTitle( Change change) {
        ContentRevision bRev = change.getBeforeRevision();
        ContentRevision aRev = change.getAfterRevision();

        assert bRev != null || aRev != null;
        if (bRev != null && aRev != null) {
            FilePath bPath = bRev.getFile();
            FilePath aPath = aRev.getFile();
            if (bPath.equals(aPath)) {
                return DiffRequestFactoryImpl.getContentTitle(bPath);
            }
            else {
                return DiffRequestFactoryImpl.getTitle(bPath, aPath, " -> ");
            }
        }
        else if (bRev != null) {
            return DiffRequestFactoryImpl.getContentTitle(bRev.getFile());
        }
        else {
            return DiffRequestFactoryImpl.getContentTitle(aRev.getFile());
        }
    }

    
    public static String getRevisionTitle( ContentRevision revision,  String defaultValue) {
        if (revision == null) return defaultValue;
        String title = revision.getRevisionNumber().asString();
        if (title == null || title.isEmpty()) return defaultValue;
        return title;
    }

    
    public static DiffContent createContent( Project project,
                                             ContentRevision revision,
                                             UserDataHolder context,
                                             ProgressIndicator indicator) throws DiffRequestProducerException {
        try {
            indicator.checkCanceled();

            if (revision == null) return DiffContentFactory.getInstance().createEmpty();

            if (revision instanceof CurrentContentRevision) {
                VirtualFile vFile = ((CurrentContentRevision)revision).getVirtualFile();
                if (vFile == null) throw new DiffRequestProducerException("Can't get current revision content");
                return DiffContentFactory.getInstance().create(project, vFile);
            }

            FilePath filePath = revision.getFile();
            if (revision instanceof BinaryContentRevision) {
                if (FileTypes.UNKNOWN.equals(filePath.getFileType())) {
                    checkAssociate(project, filePath, context, indicator);
                }

                byte[] content = ((BinaryContentRevision)revision).getBinaryContent();
                if (content == null) {
                    throw new DiffRequestProducerException("Can't get binary revision content");
                }
                return DiffContentFactory.getInstance().createBinary(project, filePath.getName(), filePath.getFileType(), content);
            }

            String revisionContent = revision.getContent();
            if (revisionContent == null) throw new DiffRequestProducerException("Can't get revision content");
            return FileAwareDocumentContent.create(project, revisionContent, filePath);
        }
        catch (IOException e) {
            LOG.info(e);
            throw new DiffRequestProducerException(e);
        }
        catch (VcsException e) {
            LOG.info(e);
            throw new DiffRequestProducerException(e);
        }
    }

    
    public static DiffContent createTextContent( byte[] bytes,  VirtualFile file) {
        return DiffContentFactory.getInstance().create(CharsetToolkit.bytesToString(bytes, file.getCharset()), file.getFileType());
    }

    public static void checkContentRevision( Project project,
                                             ContentRevision rev,
                                             UserDataHolder context,
                                             ProgressIndicator indicator) throws DiffRequestProducerException {
        if (rev.getFile().isDirectory()) {
            throw new DiffRequestProducerException("Can't show diff for directory");
        }
    }

    private static void checkAssociate( final Project project,
                                        final FilePath file,
                                        final UserDataHolder context,
                                        ProgressIndicator indicator) {
        final String pattern = FileUtilRt.getExtension(file.getName()).toLowerCase();
        if (getSkippedExtensionsFromContext(context).contains(pattern)) return;

        // TODO: this popup breaks focus. Why do we do it here anyway?
        // We should move it into CacheDiffRequestChainProcessor, but it requires some changes in ContentRevision/BinaryContentRevision API.
        ApplicationManager.getApplication().invokeAndWait(new Runnable() {
            @Override
            public void run() {
                int result = Messages.showOkCancelDialog(project,
                        VcsBundle.message("diff.unknown.file.type.prompt", file.getName()),
                        VcsBundle.message("diff.unknown.file.type.title"),
                        VcsBundle.message("diff.unknown.file.type.associate"),
                        CommonBundle.getCancelButtonText(),
                        Messages.getQuestionIcon());
                if (result == Messages.OK) {
                    FileTypeChooser.associateFileType(file.getName());
                }
                else {
                    getSkippedExtensionsFromContext(context).add(pattern);
                }
            }
        }, indicator.getModalityState());
    }

    
    private static List<String> getSkippedExtensionsFromContext( UserDataHolder context) {
        List<String> strings = CONTEXT_KEY.get(context);
        if (strings == null) {
            strings = new ArrayList<String>();
            context.putUserData(CONTEXT_KEY, strings);
        }
        return strings;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ChangeDiffRequestProducer that = (ChangeDiffRequestProducer)o;

        return myChange.equals(that.myChange);
    }

    @Override
    public int hashCode() {
        return myChange.hashCode();
    }
}
