/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.gome.maven.openapi.vcs.changes.actions;

import com.gome.maven.openapi.actionSystem.AnAction;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.diff.DiffRequestFactory;
import com.gome.maven.openapi.diff.MergeRequest;
import com.gome.maven.openapi.diff.SimpleDiffRequest;
import com.gome.maven.openapi.fileTypes.FileType;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.Getter;
import com.gome.maven.openapi.util.io.FileUtil;
import com.gome.maven.openapi.vcs.AbstractVcs;
import com.gome.maven.openapi.vcs.FilePathImpl;
import com.gome.maven.openapi.vcs.VcsException;
import com.gome.maven.openapi.vcs.changes.Change;
import com.gome.maven.openapi.vcs.changes.ChangesUtil;
import com.gome.maven.openapi.vcs.changes.MergeTexts;
import com.gome.maven.openapi.vcs.history.VcsRevisionNumber;
import com.gome.maven.openapi.vcs.merge.MergeData;
import com.gome.maven.openapi.vfs.CharsetToolkit;
import com.gome.maven.openapi.vfs.VirtualFile;

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Irina.Chernushina
 * Date: 2/14/12
 * Time: 4:09 PM
 */
public class ConflictedDiffRequestPresentable implements DiffRequestPresentable {
    private final static Logger LOG = Logger.getInstance("#com.gome.maven.openapi.vcs.changes.actions.ConflictedDiffRequestPresentable");
    private final Project myProject;
    private final VirtualFile myFile;
    private final Change myChange;

    public ConflictedDiffRequestPresentable(final Project project, VirtualFile file, final Change change) {
        myProject = project;
        myFile = file;
        myChange = change;
    }

    @Override
    public MyResult step(DiffChainContext context) {
        if (myChange.getAfterRevision() == null) return createErrorResult();
        final Getter<MergeTexts> mergeProvider = myChange.getMergeProvider();
        FileType type = myChange.getVirtualFile() != null ? myChange.getVirtualFile().getFileType() : null;
        if (mergeProvider != null) {
            // guaranteed text
            final MergeTexts texts = mergeProvider.get();
            if (texts == null) {
                return createErrorResult();
            }
            final MergeRequest request = DiffRequestFactory.getInstance()
                    .create3WayDiffRequest(texts.getLeft(), texts.getRight(), texts.getBase(), type, myProject, null, null);
            request.setWindowTitle(FileUtil.toSystemDependentName(myFile.getPresentableUrl()));
            // todo titles?
            request.setVersionTitles(new String[] {myChange.getAfterRevision().getRevisionNumber().asString(),
                    "Base Version", "Last Revision"});
            return new MyResult(request, DiffPresentationReturnValue.useRequest);

        } else {
            FilePathImpl filePath = new FilePathImpl(myFile);
            if (filePath.getFileType().isBinary()) {
                final boolean nowItIsText = ChangeDiffRequestPresentable.checkAssociate(myProject, filePath, context);
                if (! nowItIsText) {
                    return createErrorResult();
                }
            }
            final AbstractVcs vcs = ChangesUtil.getVcsForChange(myChange, myProject);
            if (vcs == null || vcs.getMergeProvider() == null) {
                return createErrorResult();
            }
            try {
                final MergeData mergeData = vcs.getMergeProvider().loadRevisions(myFile);
                if (mergeData == null) {
                    return createErrorResult();
                }
                final Charset charset = myFile.getCharset();
                final MergeRequest request = DiffRequestFactory.getInstance()
                        .create3WayDiffRequest(CharsetToolkit.bytesToString(mergeData.CURRENT, charset),
                                CharsetToolkit.bytesToString(mergeData.LAST, charset),
                                CharsetToolkit.bytesToString(mergeData.ORIGINAL, charset),
                                type, myProject, null, null);
                request.setWindowTitle(FileUtil.toSystemDependentName(myFile.getPresentableUrl()));
                // todo titles?
                VcsRevisionNumber lastRevisionNumber = mergeData.LAST_REVISION_NUMBER;
                request.setVersionTitles(new String[]{myChange.getAfterRevision().getRevisionNumber().asString(),
                        "Base Version", lastRevisionNumber != null ? lastRevisionNumber.asString() : ""});
                return new MyResult(request, DiffPresentationReturnValue.useRequest);
            }
            catch (VcsException e) {
                LOG.info(e);
                return createErrorResult();
            }
        }
    }

    private MyResult createErrorResult() {
        final SimpleDiffRequest request = new SimpleDiffRequest(myProject, null);
        return new MyResult(request, DiffPresentationReturnValue.removeFromList);
    }

    @Override
    public void haveStuff() throws VcsException {
    }

    @Override
    public List<? extends AnAction> createActions(DiffExtendUIFactory uiFactory) {
        return Collections.emptyList();
    }

    @Override
    public String getPathPresentation() {
        return myFile.getPath();
    }
}
