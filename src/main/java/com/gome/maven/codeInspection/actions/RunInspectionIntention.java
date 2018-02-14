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

package com.gome.maven.codeInspection.actions;

import com.gome.maven.analysis.AnalysisScope;
import com.gome.maven.analysis.AnalysisScopeBundle;
import com.gome.maven.analysis.AnalysisUIOptions;
import com.gome.maven.analysis.BaseAnalysisActionDialog;
import com.gome.maven.codeInsight.daemon.HighlightDisplayKey;
import com.gome.maven.codeInsight.intention.HighPriorityAction;
import com.gome.maven.codeInsight.intention.IntentionAction;
import com.gome.maven.codeInspection.InspectionManager;
import com.gome.maven.codeInspection.InspectionsBundle;
import com.gome.maven.codeInspection.ex.*;
import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.openapi.module.Module;
import com.gome.maven.openapi.module.ModuleUtilCore;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.InvalidDataException;
import com.gome.maven.openapi.util.WriteExternalException;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.profile.codeInspection.InspectionProfileManager;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.util.IncorrectOperationException;
import org.jdom.Element;

import java.util.LinkedHashSet;

/**
 * User: anna
 * Date: 21-Feb-2006
 */
public class RunInspectionIntention implements IntentionAction, HighPriorityAction {
    private final String myShortName;

    public RunInspectionIntention( InspectionToolWrapper toolWrapper) {
        myShortName = toolWrapper.getShortName();
    }

    public RunInspectionIntention(final HighlightDisplayKey key) {
        myShortName = key.toString();
    }

    @Override
    
    public String getText() {
        return InspectionsBundle.message("run.inspection.on.file.intention.text");
    }

    @Override
    
    public String getFamilyName() {
        return getText();
    }

    @Override
    public boolean isAvailable( Project project, Editor editor, PsiFile file) {
        return LocalInspectionToolWrapper.findTool2RunInBatch(project, file, myShortName) != null;
    }

    @Override
    public void invoke( Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        final InspectionManagerEx managerEx = (InspectionManagerEx)InspectionManager.getInstance(project);
        final Module module = ModuleUtilCore.findModuleForPsiElement(file);
        AnalysisScope analysisScope = new AnalysisScope(file);
        final VirtualFile virtualFile = file.getVirtualFile();
        if (file.isPhysical() || virtualFile == null || !virtualFile.isInLocalFileSystem()) {
            analysisScope = new AnalysisScope(project);
        }
        final BaseAnalysisActionDialog dlg = new BaseAnalysisActionDialog(
                AnalysisScopeBundle.message("specify.analysis.scope", InspectionsBundle.message("inspection.action.title")),
                AnalysisScopeBundle.message("analysis.scope.title", InspectionsBundle.message("inspection.action.noun")),
                project,
                analysisScope,
                module != null ? module.getName() : null,
                true, AnalysisUIOptions.getInstance(project), file);
        if (!dlg.showAndGet()) {
            return;
        }
        final AnalysisUIOptions uiOptions = AnalysisUIOptions.getInstance(project);
        analysisScope = dlg.getScope(uiOptions, analysisScope, project, module);
        rerunInspection(LocalInspectionToolWrapper.findTool2RunInBatch(project, file, myShortName), managerEx, analysisScope, file);
    }

    public static void rerunInspection( InspectionToolWrapper toolWrapper,
                                        InspectionManagerEx managerEx,
                                        AnalysisScope scope,
                                       PsiElement psiElement) {
        GlobalInspectionContextImpl inspectionContext = createContext(toolWrapper, managerEx, psiElement);
        inspectionContext.doInspections(scope);
    }

    public static GlobalInspectionContextImpl createContext( InspectionToolWrapper toolWrapper,
                                                             InspectionManagerEx managerEx,
                                                            PsiElement psiElement) {
        final InspectionProfileImpl rootProfile = (InspectionProfileImpl)InspectionProfileManager.getInstance().getRootProfile();
        LinkedHashSet<InspectionToolWrapper> allWrappers = new LinkedHashSet<InspectionToolWrapper>();
        allWrappers.add(toolWrapper);
        rootProfile.collectDependentInspections(toolWrapper, allWrappers, managerEx.getProject());
        InspectionToolWrapper[] toolWrappers = allWrappers.toArray(new InspectionToolWrapper[allWrappers.size()]);
        final InspectionProfileImpl model = InspectionProfileImpl.createSimple(toolWrapper.getDisplayName(), managerEx.getProject(), toolWrappers);
        try {
            Element element = new Element("toCopy");
            for (InspectionToolWrapper wrapper : toolWrappers) {
                wrapper.getTool().writeSettings(element);
                InspectionToolWrapper tw = psiElement == null ? model.getInspectionTool(wrapper.getShortName(), managerEx.getProject())
                        : model.getInspectionTool(wrapper.getShortName(), psiElement);
                tw.getTool().readSettings(element);
            }
        }
        catch (WriteExternalException ignored) {
        }
        catch (InvalidDataException ignored) {
        }
        model.setEditable(toolWrapper.getDisplayName());
        final GlobalInspectionContextImpl inspectionContext = managerEx.createNewGlobalContext(false);
        inspectionContext.setExternalProfile(model);
        return inspectionContext;
    }

    @Override
    public boolean startInWriteAction() {
        return false;
    }
}
