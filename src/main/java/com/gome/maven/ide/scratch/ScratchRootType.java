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
package com.gome.maven.ide.scratch;

import com.gome.maven.icons.AllIcons;
import com.gome.maven.lang.Language;
import com.gome.maven.openapi.application.Result;
import com.gome.maven.openapi.application.RunResult;
import com.gome.maven.openapi.command.UndoConfirmationPolicy;
import com.gome.maven.openapi.command.WriteCommandAction;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.ui.Messages;
import com.gome.maven.openapi.vfs.VfsUtil;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.ui.LayeredIcon;
import com.gome.maven.ui.UIBundle;
import com.gome.maven.util.ObjectUtils;

import javax.swing.*;

/**
 * @author gregsh
 */
public final class ScratchRootType extends RootType {

    
    public static ScratchRootType getInstance() {
        return findByClass(ScratchRootType.class);
    }

    ScratchRootType() {
        super("scratches", "Scratches");
    }

    @Override
    public Language substituteLanguage( Project project,  VirtualFile file) {
        Language language = ScratchFileService.getInstance().getScratchesMapping().getMapping(file);
        return substituteLanguageImpl(language, file, project);
    }

    
    @Override
    public Icon substituteIcon( Project project,  VirtualFile file) {
        Icon icon = ObjectUtils.chooseNotNull(super.substituteIcon(project, file), ScratchFileType.INSTANCE.getIcon());
        return LayeredIcon.create(icon, AllIcons.Actions.Scratch);
    }

    public VirtualFile createScratchFile(Project project, final String fileName, final Language language, final String text) {
        RunResult<VirtualFile> result =
                new WriteCommandAction<VirtualFile>(project, UIBundle.message("file.chooser.create.new.file.command.name")) {
                    @Override
                    protected boolean isGlobalUndoAction() {
                        return true;
                    }

                    @Override
                    protected UndoConfirmationPolicy getUndoConfirmationPolicy() {
                        return UndoConfirmationPolicy.REQUEST_CONFIRMATION;
                    }

                    @Override
                    protected void run( Result<VirtualFile> result) throws Throwable {
                        ScratchFileService fileService = ScratchFileService.getInstance();
                        VirtualFile file = fileService.findFile(ScratchRootType.this, "scratch", ScratchFileService.Option.create_new_always);
                        fileService.getScratchesMapping().setMapping(file, language);
                        VfsUtil.saveText(file, text);
                        result.setResult(file);

                    }
                }.execute();
        if (result.hasException()) {
            Messages.showMessageDialog(UIBundle.message("create.new.file.could.not.create.file.error.message", fileName),
                    UIBundle.message("error.dialog.title"), Messages.getErrorIcon());
            return null;
        }
        return result.getResultObject();
    }
}
