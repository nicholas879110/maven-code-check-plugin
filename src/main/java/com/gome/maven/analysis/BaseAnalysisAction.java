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

package com.gome.maven.analysis;

import com.gome.maven.ide.highlighter.ArchiveFileType;
import com.gome.maven.openapi.actionSystem.*;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.fileEditor.FileDocumentManager;
import com.gome.maven.openapi.help.HelpManager;
import com.gome.maven.openapi.module.Module;
import com.gome.maven.openapi.module.ModuleUtilCore;
import com.gome.maven.openapi.project.DumbService;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.roots.ProjectFileIndex;
import com.gome.maven.openapi.roots.ProjectRootManager;
import com.gome.maven.openapi.vfs.JarFileSystem;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.psi.PsiDirectory;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.psi.PsiManager;

import javax.swing.*;
import java.util.HashSet;
import java.util.Set;

public abstract class BaseAnalysisAction extends AnAction {
    private final String myTitle;
    private final String myAnalysisNoon;
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.analysis.BaseAnalysisAction");

    protected BaseAnalysisAction(String title, String analysisNoon) {
        myTitle = title;
        myAnalysisNoon = analysisNoon;
    }

    @Override
    public void update(AnActionEvent event) {
        Presentation presentation = event.getPresentation();
        final DataContext dataContext = event.getDataContext();
        final Project project = event.getProject();
        final boolean dumbMode = project == null || DumbService.getInstance(project).isDumb();
        presentation.setEnabled(!dumbMode && getInspectionScope(dataContext) != null);
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        DataContext dataContext = e.getDataContext();
        final Project project = e.getData(CommonDataKeys.PROJECT);
        final Module module = e.getData(LangDataKeys.MODULE);
        if (project == null) {
            return;
        }
        AnalysisScope scope = getInspectionScope(dataContext);
        LOG.assertTrue(scope != null);
        final boolean rememberScope = ActionPlaces.isMainMenuOrActionSearch(e.getPlace());
        final AnalysisUIOptions uiOptions = AnalysisUIOptions.getInstance(project);
        PsiElement element = CommonDataKeys.PSI_ELEMENT.getData(dataContext);
        BaseAnalysisActionDialog dlg = new BaseAnalysisActionDialog(AnalysisScopeBundle.message("specify.analysis.scope", myTitle),
                AnalysisScopeBundle.message("analysis.scope.title", myAnalysisNoon),
                project,
                scope,
                module != null ? ModuleUtilCore
                        .getModuleNameInReadAction(module) : null,
                rememberScope, AnalysisUIOptions.getInstance(project), element) {
            @Override
            
            protected JComponent getAdditionalActionSettings(final Project project) {
                return BaseAnalysisAction.this.getAdditionalActionSettings(project, this);
            }


            @Override
            protected void doHelpAction() {
                HelpManager.getInstance().invokeHelp(getHelpTopic());
            }

            
            @Override
            protected Action[] createActions() {
                return new Action[]{getOKAction(), getCancelAction(), getHelpAction()};
            }
        };
        if (!dlg.showAndGet()) {
            canceled();
            return;
        }
        final int oldScopeType = uiOptions.SCOPE_TYPE;
        scope = dlg.getScope(uiOptions, scope, project, module);
        if (!rememberScope) {
            uiOptions.SCOPE_TYPE = oldScopeType;
        }
        uiOptions.ANALYZE_TEST_SOURCES = dlg.isInspectTestSources();
        FileDocumentManager.getInstance().saveAllDocuments();

        analyze(project, scope);
    }

    
    protected String getHelpTopic() {
        return "reference.dialogs.analyzeDependencies.scope";
    }

    protected void canceled() {
    }

    protected abstract void analyze( Project project,  AnalysisScope scope);

    
    private AnalysisScope getInspectionScope( DataContext dataContext) {
        if (CommonDataKeys.PROJECT.getData(dataContext) == null) return null;

        AnalysisScope scope = getInspectionScopeImpl(dataContext);

        return scope != null && scope.getScopeType() != AnalysisScope.INVALID ? scope : null;
    }

    
    private AnalysisScope getInspectionScopeImpl( DataContext dataContext) {
        //Possible scopes: file, directory, package, project, module.
        Project projectContext = PlatformDataKeys.PROJECT_CONTEXT.getData(dataContext);
        if (projectContext != null) {
            return new AnalysisScope(projectContext);
        }

        final AnalysisScope analysisScope = AnalysisScopeUtil.KEY.getData(dataContext);
        if (analysisScope != null) {
            return analysisScope;
        }

        final PsiFile psiFile = CommonDataKeys.PSI_FILE.getData(dataContext);
        if (psiFile != null && psiFile.getManager().isInProject(psiFile)) {
            final VirtualFile file = psiFile.getVirtualFile();
            if (file != null && file.isValid() && file.getFileType() instanceof ArchiveFileType && acceptNonProjectDirectories()) {
                final VirtualFile jarRoot = JarFileSystem.getInstance().getJarRootForLocalFile(file);
                if (jarRoot != null) {
                    PsiDirectory psiDirectory = psiFile.getManager().findDirectory(jarRoot);
                    if (psiDirectory != null) {
                        return new AnalysisScope(psiDirectory);
                    }
                }
            }
            return new AnalysisScope(psiFile);
        }

        VirtualFile[] virtualFiles = CommonDataKeys.VIRTUAL_FILE_ARRAY.getData(dataContext);
        Project project = CommonDataKeys.PROJECT.getData(dataContext);
        if (virtualFiles != null && project != null) { //analyze on selection
            ProjectFileIndex fileIndex = ProjectRootManager.getInstance(project).getFileIndex();
            if (virtualFiles.length == 1) {
                PsiDirectory psiDirectory = PsiManager.getInstance(project).findDirectory(virtualFiles[0]);
                if (psiDirectory != null && (acceptNonProjectDirectories() || psiDirectory.getManager().isInProject(psiDirectory))) {
                    return new AnalysisScope(psiDirectory);
                }
            }
            Set<VirtualFile> files = new HashSet<VirtualFile>();
            for (VirtualFile vFile : virtualFiles) {
                if (fileIndex.isInContent(vFile)) {
                    files.add(vFile);
                }
            }
            return new AnalysisScope(project, files);
        }

        Module moduleContext = LangDataKeys.MODULE_CONTEXT.getData(dataContext);
        if (moduleContext != null) {
            return new AnalysisScope(moduleContext);
        }

        Module[] modulesArray = LangDataKeys.MODULE_CONTEXT_ARRAY.getData(dataContext);
        if (modulesArray != null) {
            return new AnalysisScope(modulesArray);
        }
        return project == null ? null : new AnalysisScope(project);
    }

    protected boolean acceptNonProjectDirectories() {
        return false;
    }

    
    protected JComponent getAdditionalActionSettings(Project project, BaseAnalysisActionDialog dialog){
        return null;
    }

}
