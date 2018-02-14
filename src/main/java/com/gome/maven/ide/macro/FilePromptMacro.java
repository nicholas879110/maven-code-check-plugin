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
package com.gome.maven.ide.macro;

import com.gome.maven.ide.IdeBundle;
import com.gome.maven.openapi.actionSystem.CommonDataKeys;
import com.gome.maven.openapi.actionSystem.DataContext;
import com.gome.maven.openapi.actionSystem.PlatformDataKeys;
import com.gome.maven.openapi.fileChooser.*;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.io.FileUtil;
import com.gome.maven.openapi.vfs.VirtualFile;

/**
 * @author yole
 */
public class FilePromptMacro extends PromptingMacro implements SecondQueueExpandMacro {
    @Override
    public String getName() {
        return "FilePrompt";
    }

    @Override
    public String getDescription() {
        return "Shows a file chooser dialog";
    }

    @Override
    protected String promptUser(DataContext dataContext) {
        Project project = CommonDataKeys.PROJECT.getData(dataContext);
        final FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createSingleLocalFileDescriptor();
        final VirtualFile[] result = FileChooser.chooseFiles(descriptor, project, null);
        return result.length == 1? FileUtil.toSystemDependentName(result[0].getPath()) : null;
    }

    @Override
    public void cachePreview(DataContext dataContext) {
        myCachedPreview = IdeBundle.message("macro.fileprompt.preview");
    }
}
