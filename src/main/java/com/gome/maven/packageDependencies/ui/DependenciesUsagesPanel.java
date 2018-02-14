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
 * User: anna
 * Date: 26-Mar-2008
 */
package com.gome.maven.packageDependencies.ui;

import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.application.ModalityState;
import com.gome.maven.openapi.progress.ProcessCanceledException;
import com.gome.maven.openapi.progress.ProgressIndicator;
import com.gome.maven.openapi.progress.ProgressManager;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.packageDependencies.DependenciesBuilder;
import com.gome.maven.packageDependencies.FindDependencyUtil;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.psi.util.PsiUtilCore;
import com.gome.maven.usageView.UsageInfo;
import com.gome.maven.util.Consumer;

import javax.swing.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DependenciesUsagesPanel extends UsagesPanel {
    private final List<DependenciesBuilder> myBuilders;

    public DependenciesUsagesPanel(Project project, final List<DependenciesBuilder> builders) {
        super(project);
        myBuilders = builders;
        setToInitialPosition();
    }

    @Override
    public String getInitialPositionText() {
        return myBuilders.get(0).getInitialUsagesPosition();
    }


    @Override
    public String getCodeUsagesString() {
        return myBuilders.get(0).getRootNodeNameInUsageView();
    }

    public void findUsages(final Set<PsiFile> searchIn, final Set<PsiFile> searchFor) {
        cancelCurrentFindRequest();

        myAlarm.cancelAllRequests();
        myAlarm.addRequest(new Runnable() {
            @Override
            public void run() {
                ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
                    @Override
                    public void run() {
                        final ProgressIndicator progress = new PanelProgressIndicator(new Consumer<JComponent>() {
                            @Override
                            public void consume(final JComponent component) {
                                setToComponent(component);
                            }
                        });
                        myCurrentProgress = progress;
                        ProgressManager.getInstance().runProcess(new Runnable() {
                            @Override
                            public void run() {
                                ApplicationManager.getApplication().runReadAction(new Runnable() {
                                    @Override
                                    public void run() {
                                        UsageInfo[] usages = new UsageInfo[0];
                                        Set<PsiFile> elementsToSearch = null;

                                        try {
                                            if (myBuilders.get(0).isBackward()){
                                                elementsToSearch = searchIn;
                                                usages = FindDependencyUtil.findBackwardDependencies(myBuilders, searchFor, searchIn);
                                            }
                                            else {
                                                elementsToSearch = searchFor;
                                                usages = FindDependencyUtil.findDependencies(myBuilders, searchIn, searchFor);
                                            }
                                            assert !new HashSet<PsiFile>(elementsToSearch).contains(null);
                                        }
                                        catch (ProcessCanceledException e) {
                                        }
                                        catch (Exception e) {
                                            LOG.error(e);
                                        }

                                        if (!progress.isCanceled()) {
                                            final UsageInfo[] finalUsages = usages;
                                            final PsiElement[] _elementsToSearch =
                                                    elementsToSearch != null ? PsiUtilCore.toPsiElementArray(elementsToSearch) : PsiElement.EMPTY_ARRAY;
                                            ApplicationManager.getApplication().invokeLater(new Runnable() {
                                                @Override
                                                public void run() {
                                                    showUsages(_elementsToSearch, finalUsages);
                                                }
                                            }, ModalityState.stateForComponent(DependenciesUsagesPanel.this));
                                        }
                                    }
                                });
                                myCurrentProgress = null;
                            }
                        }, progress);
                    }
                });
            }
        }, 300);
    }

    public void addBuilder(DependenciesBuilder builder) {
        myBuilders.add(builder);
    }
}
