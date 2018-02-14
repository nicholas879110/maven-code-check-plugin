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

package com.gome.maven.packageDependencies.ui;

import com.gome.maven.analysis.AnalysisScopeBundle;
import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.actionSystem.DataProvider;
import com.gome.maven.openapi.actionSystem.PlatformDataKeys;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.progress.ProcessCanceledException;
import com.gome.maven.openapi.progress.ProgressIndicator;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.Disposer;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.usageView.UsageInfo;
import com.gome.maven.usages.*;
import com.gome.maven.util.Alarm;

import javax.swing.*;
import java.awt.*;

public abstract class UsagesPanel extends JPanel implements Disposable, DataProvider {
    protected static final Logger LOG = Logger.getInstance("#com.intellij.packageDependencies.ui.UsagesPanel");

    private final Project myProject;
    protected ProgressIndicator myCurrentProgress;
    private JComponent myCurrentComponent;
    private UsageView myCurrentUsageView;
    protected final Alarm myAlarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD);

    public UsagesPanel(Project project) {
        super(new BorderLayout());
        myProject = project;
    }

    public void setToInitialPosition() {
        cancelCurrentFindRequest();
        setToComponent(createLabel(getInitialPositionText()));
    }

    public abstract String getInitialPositionText();
    public abstract String getCodeUsagesString();


    protected void cancelCurrentFindRequest() {
        if (myCurrentProgress != null) {
            myCurrentProgress.cancel();
        }
    }

    protected void showUsages( PsiElement[] primaryElements,  UsageInfo[] usageInfos) {
        if (myCurrentUsageView != null) {
            Disposer.dispose(myCurrentUsageView);
        }
        try {
            Usage[] usages = UsageInfoToUsageConverter.convert(primaryElements, usageInfos);
            UsageViewPresentation presentation = new UsageViewPresentation();
            presentation.setCodeUsagesString(getCodeUsagesString());
            myCurrentUsageView = UsageViewManager.getInstance(myProject).createUsageView(UsageTarget.EMPTY_ARRAY, usages, presentation, null);
            setToComponent(myCurrentUsageView.getComponent());
        }
        catch (ProcessCanceledException e) {
            setToCanceled();
        }
    }

    private void setToCanceled() {
        setToComponent(createLabel(AnalysisScopeBundle.message("usage.view.canceled")));
    }

    protected void setToComponent(final JComponent cmp) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (myCurrentComponent != null) {
                    if (myCurrentUsageView != null && myCurrentComponent == myCurrentUsageView.getComponent()){
                        Disposer.dispose(myCurrentUsageView);
                    }
                    remove(myCurrentComponent);
                }
                myCurrentComponent = cmp;
                add(cmp, BorderLayout.CENTER);
                revalidate();
            }
        });
    }

    @Override
    public void dispose(){
        if (myCurrentUsageView != null){
            Disposer.dispose(myCurrentUsageView);
        }
    }

    private static JComponent createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setHorizontalAlignment(SwingConstants.CENTER);
        return label;
    }

    @Override


    public Object getData( String dataId) {
        if (PlatformDataKeys.HELP_ID.is(dataId)) {
            return "ideaInterface.find";
        }
        return null;
    }
}
