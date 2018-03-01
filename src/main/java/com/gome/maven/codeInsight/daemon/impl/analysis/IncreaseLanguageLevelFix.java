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
package com.gome.maven.codeInsight.daemon.impl.analysis;

import com.gome.maven.codeInsight.CodeInsightBundle;
import com.gome.maven.codeInsight.intention.IntentionAction;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.openapi.module.Module;
import com.gome.maven.openapi.module.ModuleUtilCore;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.projectRoots.JavaSdkVersion;
import com.gome.maven.openapi.projectRoots.JdkVersionUtil;
import com.gome.maven.openapi.projectRoots.Sdk;
import com.gome.maven.openapi.roots.*;
import com.gome.maven.openapi.roots.ex.ProjectRootManagerEx;
import com.gome.maven.openapi.util.EmptyRunnable;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.pom.java.LanguageLevel;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.util.IncorrectOperationException;

/**
 * @author cdr
 */
public class IncreaseLanguageLevelFix implements IntentionAction {
    private static final Logger LOG = Logger.getInstance("#" + IncreaseLanguageLevelFix.class.getName());

    private final LanguageLevel myLevel;

    public IncreaseLanguageLevelFix( LanguageLevel targetLevel) {
        myLevel = targetLevel;
    }

    @Override
    
    public String getText() {
        return CodeInsightBundle.message("set.language.level.to.0", myLevel.getPresentableText());
    }

    @Override
    
    public String getFamilyName() {
        return CodeInsightBundle.message("set.language.level");
    }

    private static boolean isJdkSupportsLevel( final Sdk jdk,  LanguageLevel level) {
        if (jdk == null) return true;
        String versionString = jdk.getVersionString();
        JavaSdkVersion version = versionString == null ? null : JdkVersionUtil.getVersion(versionString);
        return version != null && version.getMaxLanguageLevel().isAtLeast(level);
    }

    @Override
    public boolean isAvailable( final Project project, final Editor editor, final PsiFile file) {
        final VirtualFile virtualFile = file.getVirtualFile();
        if (virtualFile == null) return false;
        final Module module = ModuleUtilCore.findModuleForFile(virtualFile, project);
        if (module == null) return false;
        return isLanguageLevelAcceptable(project, module, myLevel);
    }

    private static boolean isLanguageLevelAcceptable( Project project, Module module,  LanguageLevel level) {
        return isJdkSupportsLevel(getRelevantJdk(project, module), level);
    }

    @Override
    public void invoke( final Project project, final Editor editor, final PsiFile file) throws IncorrectOperationException {
        final VirtualFile virtualFile = file.getVirtualFile();
        LOG.assertTrue(virtualFile != null);
        final Module module = ModuleUtilCore.findModuleForFile(virtualFile, project);
        final LanguageLevel moduleLevel = module == null ? null : LanguageLevelModuleExtensionImpl.getInstance(module).getLanguageLevel();
        ApplicationManager.getApplication().runWriteAction(new Runnable() {
            @Override
            public void run() {
                if (moduleLevel != null && isLanguageLevelAcceptable(project, module, myLevel)) {
                    final ModifiableRootModel rootModel = ModuleRootManager.getInstance(module).getModifiableModel();
                    rootModel.getModuleExtension(LanguageLevelModuleExtension.class).setLanguageLevel(myLevel);
                    rootModel.commit();
                }
                else {
                    LanguageLevelProjectExtension.getInstance(project).setLanguageLevel(myLevel);
                    ProjectRootManagerEx.getInstanceEx(project).makeRootsChange(EmptyRunnable.INSTANCE, false, true);
                }
            }
        });
    }

    
    private static Sdk getRelevantJdk( Project project,  Module module) {
        Sdk projectJdk = ProjectRootManager.getInstance(project).getProjectSdk();
        Sdk moduleJdk = module == null ? null : ModuleRootManager.getInstance(module).getSdk();
        return moduleJdk == null ? projectJdk : moduleJdk;
    }

    @Override
    public boolean startInWriteAction() {
        return false;
    }
}
