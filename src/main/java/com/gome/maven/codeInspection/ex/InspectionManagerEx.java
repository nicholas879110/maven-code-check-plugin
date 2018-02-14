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

/*
 * Author: max
 * Date: Oct 9, 2001
 * Time: 8:43:17 PM
 */

package com.gome.maven.codeInspection.ex;

import com.gome.maven.codeInspection.*;
import com.gome.maven.icons.AllIcons;
import com.gome.maven.ide.impl.ContentManagerWatcher;
import com.gome.maven.lang.Language;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.NotNullLazyValue;
import com.gome.maven.openapi.wm.ToolWindow;
import com.gome.maven.openapi.wm.ToolWindowAnchor;
import com.gome.maven.openapi.wm.ToolWindowId;
import com.gome.maven.openapi.wm.ToolWindowManager;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.ui.content.ContentFactory;
import com.gome.maven.ui.content.ContentManager;
import com.gome.maven.ui.content.TabbedPaneContentUI;
import com.gome.maven.util.Function;
import com.gome.maven.util.containers.ContainerUtil;

import java.util.*;

public class InspectionManagerEx extends InspectionManagerBase {
    private final NotNullLazyValue<ContentManager> myContentManager;
    private final Set<GlobalInspectionContextImpl> myRunningContexts = new HashSet<GlobalInspectionContextImpl>();
    private GlobalInspectionContextImpl myGlobalInspectionContext;

    public InspectionManagerEx(final Project project) {
        super(project);
        if (ApplicationManager.getApplication().isHeadlessEnvironment()) {
            myContentManager = new NotNullLazyValue<ContentManager>() {
                
                @Override
                protected ContentManager compute() {
                    ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
                    toolWindowManager.registerToolWindow(ToolWindowId.INSPECTION, true, ToolWindowAnchor.BOTTOM, project);
                    return ContentFactory.SERVICE.getInstance().createContentManager(new TabbedPaneContentUI(), true, project);
                }
            };
        }
        else {
            myContentManager = new NotNullLazyValue<ContentManager>() {
                
                @Override
                protected ContentManager compute() {
                    ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
                    ToolWindow toolWindow =
                            toolWindowManager.registerToolWindow(ToolWindowId.INSPECTION, true, ToolWindowAnchor.BOTTOM, project);
                    ContentManager contentManager = toolWindow.getContentManager();
                    toolWindow.setIcon(AllIcons.Toolwindows.ToolWindowInspection);
                    new ContentManagerWatcher(toolWindow, contentManager);
                    return contentManager;
                }
            };
        }
    }

    
    public static SuppressIntentionAction[] getSuppressActions( InspectionToolWrapper toolWrapper) {
        final InspectionProfileEntry tool = toolWrapper.getTool();
        if (tool instanceof CustomSuppressableInspectionTool) {
            return ((CustomSuppressableInspectionTool)tool).getSuppressActions(null);
        }
        final List<LocalQuickFix> actions = new ArrayList<LocalQuickFix>(Arrays.asList(tool.getBatchSuppressActions(null)));
        if (actions.isEmpty()) {
            final Language language = Language.findLanguageByID(toolWrapper.getLanguage());
            if (language != null) {
                final List<InspectionSuppressor> suppressors = LanguageInspectionSuppressors.INSTANCE.allForLanguage(language);
                for (InspectionSuppressor suppressor : suppressors) {
                    final SuppressQuickFix[] suppressActions = suppressor.getSuppressActions(null, tool.getShortName());
                    Collections.addAll(actions, suppressActions);
                }
            }
        }
        return ContainerUtil.map2Array(actions, SuppressIntentionAction.class, new Function<LocalQuickFix, SuppressIntentionAction>() {
            @Override
            public SuppressIntentionAction fun(final LocalQuickFix fix) {
                return SuppressIntentionActionFromFix.convertBatchToSuppressIntentionAction((SuppressQuickFix)fix);
            }
        });
    }


    
    public ProblemDescriptor createProblemDescriptor( final PsiElement psiElement,
                                                      final String descriptionTemplate,
                                                      final ProblemHighlightType highlightType,
                                                      final HintAction hintAction,
                                                     boolean onTheFly,
                                                     final LocalQuickFix... fixes) {
        return new ProblemDescriptorImpl(psiElement, psiElement, descriptionTemplate, fixes, highlightType, false, null, hintAction, onTheFly);
    }

    @Override
    
    public GlobalInspectionContextImpl createNewGlobalContext(boolean reuse) {
        final GlobalInspectionContextImpl inspectionContext;
        if (reuse) {
            if (myGlobalInspectionContext == null) {
                myGlobalInspectionContext = inspectionContext = new GlobalInspectionContextImpl(getProject(), myContentManager);
            }
            else {
                inspectionContext = myGlobalInspectionContext;
            }
        }
        else {
            inspectionContext = new GlobalInspectionContextImpl(getProject(), myContentManager);
        }
        myRunningContexts.add(inspectionContext);
        return inspectionContext;
    }

    public void setProfile(final String name) {
        myCurrentProfileName = name;
    }

    public void closeRunningContext(GlobalInspectionContextImpl globalInspectionContext){
        myRunningContexts.remove(globalInspectionContext);
    }

    
    public Set<GlobalInspectionContextImpl> getRunningContexts() {
        return myRunningContexts;
    }

    
    @Deprecated
    public ProblemDescriptor createProblemDescriptor( final PsiElement psiElement,
                                                      final String descriptionTemplate,
                                                      final ProblemHighlightType highlightType,
                                                      final HintAction hintAction,
                                                     final LocalQuickFix... fixes) {

        return new ProblemDescriptorImpl(psiElement, psiElement, descriptionTemplate, fixes, highlightType, false, null, hintAction, true);
    }


    public NotNullLazyValue<ContentManager> getContentManager() {
        return myContentManager;
    }

}