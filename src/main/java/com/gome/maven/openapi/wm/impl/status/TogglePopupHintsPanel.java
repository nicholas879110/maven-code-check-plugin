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

package com.gome.maven.openapi.wm.impl.status;

import com.gome.maven.codeInsight.daemon.DaemonCodeAnalyzer;
import com.gome.maven.codeInsight.daemon.impl.HectorComponent;
import com.gome.maven.codeInsight.daemon.impl.analysis.HighlightingLevelManager;
import com.gome.maven.icons.AllIcons;
import com.gome.maven.ide.PowerSaveMode;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.fileEditor.FileEditorManager;
import com.gome.maven.openapi.fileEditor.FileEditorManagerEvent;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.openapi.wm.StatusBarWidget;
import com.gome.maven.profile.codeInspection.InspectionProjectProfileManager;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.psi.PsiManager;
import com.gome.maven.ui.UIBundle;
import com.gome.maven.ui.awt.RelativePoint;
import com.gome.maven.util.Consumer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;

public class TogglePopupHintsPanel extends EditorBasedWidget implements StatusBarWidget.Multiframe, StatusBarWidget.IconPresentation {
    private Icon myCurrentIcon;
    private String myToolTipText;

    public TogglePopupHintsPanel( final Project project) {
        super(project);
        myCurrentIcon = AllIcons.Ide.HectorNo;
        myConnection.subscribe(PowerSaveMode.TOPIC, new PowerSaveMode.Listener() {
            @Override
            public void powerSaveStateChanged() {
                updateStatus();
            }
        });
    }

    @Override
    public void selectionChanged( FileEditorManagerEvent event) {
        updateStatus();
    }


    @Override
    public void fileOpened( FileEditorManager source,  VirtualFile file) {
        updateStatus();
    }

    @Override
    public StatusBarWidget copy() {
        return new TogglePopupHintsPanel(getProject());
    }

    @Override
    
    public Icon getIcon() {
        return myCurrentIcon;
    }

    @Override
    public String getTooltipText() {
        return myToolTipText;
    }

    @Override
    public Consumer<MouseEvent> getClickConsumer() {
        return new Consumer<MouseEvent>() {
            @Override
            public void consume(final MouseEvent e) {
                Point point = new Point(0, 0);
                final PsiFile file = getCurrentFile();
                if (file != null) {
                    if (!DaemonCodeAnalyzer.getInstance(file.getProject()).isHighlightingAvailable(file)) return;
                    final HectorComponent component = new HectorComponent(file);
                    final Dimension dimension = component.getPreferredSize();
                    point = new Point(point.x - dimension.width, point.y - dimension.height);
                    component.showComponent(new RelativePoint(e.getComponent(), point));
                }
            }
        };
    }

    @Override
    
    public String ID() {
        return "InspectionProfile";
    }

    @Override
    public WidgetPresentation getPresentation( PlatformType type) {
        return this;
    }

    public void clear() {
        myCurrentIcon = AllIcons.Ide.HectorNo;
        myToolTipText = null;
        myStatusBar.updateWidget(ID());
    }

    public void updateStatus() {
        updateStatus(getCurrentFile());
    }

    private void updateStatus(PsiFile file) {
        if (isStateChangeable(file)) {
            if (PowerSaveMode.isEnabled()) {
                myCurrentIcon = AllIcons.Ide.HectorNo;
                myToolTipText = "Code analysis is disabled in power save mode.\n";
            }
            else if (HighlightingLevelManager.getInstance(myProject).shouldInspect(file)) {
                myCurrentIcon = AllIcons.Ide.HectorOn;
                myToolTipText = "Current inspection profile: " +
                        InspectionProjectProfileManager.getInstance(file.getProject()).getInspectionProfile().getName() +
                        ".\n";
            }
            else if (HighlightingLevelManager.getInstance(myProject).shouldHighlight(file)) {
                myCurrentIcon = AllIcons.Ide.HectorSyntax;
                myToolTipText = "Highlighting level is: Syntax.\n";
            }
            else {
                myCurrentIcon = AllIcons.Ide.HectorOff;
                myToolTipText = "Inspections are off.\n";
            }
            myToolTipText += UIBundle.message("popup.hints.panel.click.to.configure.highlighting.tooltip.text");
        }
        else {
            myCurrentIcon = AllIcons.Ide.HectorNo;
            myToolTipText = null;
        }

        if (!ApplicationManager.getApplication().isUnitTestMode() && myStatusBar != null) {
            myStatusBar.updateWidget(ID());
        }
    }

    private static boolean isStateChangeable(PsiFile file) {
        return file != null && DaemonCodeAnalyzer.getInstance(file.getProject()).isHighlightingAvailable(file);
    }


    private PsiFile getCurrentFile() {
        VirtualFile virtualFile = getSelectedFile();
        if (virtualFile != null && virtualFile.isValid()){
            return PsiManager.getInstance(getProject()).findFile(virtualFile);
        }
        return null;
    }
}
