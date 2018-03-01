/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package com.gome.maven.openapi.vcs.changes.patch;

import com.gome.maven.openapi.actionSystem.AnAction;
import com.gome.maven.openapi.diff.DiffRequestFactory;
import com.gome.maven.openapi.diff.MergeRequest;
import com.gome.maven.openapi.diff.SimpleDiffRequest;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.Getter;
import com.gome.maven.openapi.util.io.FileUtil;
import com.gome.maven.openapi.vcs.FilePathImpl;
import com.gome.maven.openapi.vcs.VcsBundle;
import com.gome.maven.openapi.vcs.changes.actions.*;
import com.gome.maven.openapi.vfs.VirtualFile;

import java.util.Collections;
import java.util.List;

public class MergedDiffRequestPresentable implements DiffRequestPresentable {
    private final Project myProject;
    private final VirtualFile myFile;
    private final String myAfterTitle;
    private final Getter<ApplyPatchForBaseRevisionTexts> myTexts;

    public MergedDiffRequestPresentable(final Project project, final Getter<ApplyPatchForBaseRevisionTexts> texts, final VirtualFile file, final String afterTitle) {
        myTexts = texts;
        myProject = project;
        myFile = file;
        myAfterTitle = afterTitle;
    }

    public MyResult step(DiffChainContext context) {
        FilePathImpl filePath = new FilePathImpl(myFile);
        if (filePath.getFileType().isBinary()) {
            final boolean nowItIsText = ChangeDiffRequestPresentable.checkAssociate(myProject, filePath, context);
            if (! nowItIsText) {
                final SimpleDiffRequest request = new SimpleDiffRequest(myProject, null);
                return new MyResult(request, DiffPresentationReturnValue.removeFromList);
            }
        }
        final ApplyPatchForBaseRevisionTexts revisionTexts = myTexts.get();
        if (revisionTexts.getBase() == null) {
            final SimpleDiffRequest badDiffRequest = ApplyPatchAction.createBadDiffRequest(myProject, myFile, revisionTexts, true);
            return new MyResult(badDiffRequest, DiffPresentationReturnValue.useRequest);
        }
        final MergeRequest request = DiffRequestFactory.getInstance()
                .create3WayDiffRequest(revisionTexts.getLocal().toString(),
                        revisionTexts.getPatched(),
                        revisionTexts.getBase().toString(),
                        filePath.getFileType(), myProject, null, null);
        request.setWindowTitle(VcsBundle.message("patch.apply.conflict.title", FileUtil.toSystemDependentName(myFile.getPresentableUrl())));
        request.setVersionTitles(new String[] {"Current Version", "Base Version", FileUtil.toSystemDependentName(myAfterTitle)});
        return new MyResult(request, DiffPresentationReturnValue.useRequest);
    }

    @Override
    public String getPathPresentation() {
        return myFile.getPath();
    }

    public void haveStuff() {
    }

    public List<? extends AnAction> createActions(DiffExtendUIFactory uiFactory) {
        return Collections.emptyList();
    }
}
