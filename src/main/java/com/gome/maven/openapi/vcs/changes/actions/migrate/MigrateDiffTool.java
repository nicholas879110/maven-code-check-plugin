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
package com.gome.maven.openapi.vcs.changes.actions.migrate;

import com.gome.maven.diff.DiffDialogHints;
import com.gome.maven.diff.DiffManager;
import com.gome.maven.diff.chains.DiffRequestChain;
import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.diff.DiffRequest;
import com.gome.maven.openapi.diff.DiffTool;
import com.gome.maven.openapi.diff.DiffViewer;
import com.gome.maven.openapi.diff.MergeRequest;
import com.gome.maven.openapi.diff.impl.external.BinaryDiffTool;
import com.gome.maven.openapi.diff.impl.external.DiffManagerImpl;
import com.gome.maven.openapi.diff.impl.external.FrameDiffTool;
import com.gome.maven.openapi.ui.WindowWrapper;

import java.awt.*;

public class MigrateDiffTool implements DiffTool {
    public static final MigrateDiffTool INSTANCE = new MigrateDiffTool();

    private MigrateDiffTool() {
    }

    @Override
    public void show(DiffRequest request) {
        DiffRequestChain newChain = MigrateToNewDiffUtil.convertRequestChain(request);
        WindowWrapper.Mode mode = FrameDiffTool.shouldOpenDialog(request.getHints()) ? WindowWrapper.Mode.MODAL : WindowWrapper.Mode.FRAME;
        DiffManager.getInstance().showDiff(request.getProject(), newChain, new DiffDialogHints(mode));
    }

    @Override
    public boolean canShow(DiffRequest request) {
        if (request instanceof MergeRequest) return false;
        if (request.getContents().length != 2) return false;
        if (request.getHints().contains(MigrateToNewDiffUtil.DO_NOT_TRY_MIGRATE)) return false;
        if (request.getOnOkRunnable() != null) return false;
        if (!DiffManagerImpl.INTERNAL_DIFF.canShow(request) && !BinaryDiffTool.INSTANCE.canShow(request)) return false;
        for (DiffTool tool : DiffManagerImpl.getInstanceEx().getAdditionTools()) {
            if (tool == this) continue;
            if (tool.canShow(request)) return false;
        }
        return true;
    }

    @Override
    public DiffViewer createComponent(String title, DiffRequest request, Window window,  Disposable parentDisposable) {
        return null;
    }
}
