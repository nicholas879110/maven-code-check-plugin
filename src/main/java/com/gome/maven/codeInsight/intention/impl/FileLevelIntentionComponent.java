/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package com.gome.maven.codeInsight.intention.impl;

import com.gome.maven.codeInsight.daemon.GutterMark;
import com.gome.maven.codeInsight.daemon.impl.HighlightInfo;
import com.gome.maven.codeInsight.daemon.impl.SeverityRegistrar;
import com.gome.maven.codeInsight.daemon.impl.ShowIntentionsPass;
import com.gome.maven.codeInsight.intention.EmptyIntentionAction;
import com.gome.maven.codeInsight.intention.IntentionAction;
import com.gome.maven.icons.AllIcons;
import com.gome.maven.lang.annotation.HighlightSeverity;
import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.ui.popup.JBPopupFactory;
import com.gome.maven.openapi.ui.popup.ListPopup;
import com.gome.maven.openapi.util.Pair;
import com.gome.maven.openapi.util.TextRange;
import com.gome.maven.psi.PsiDocumentManager;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.ui.ClickListener;
import com.gome.maven.ui.EditorNotificationPanel;
import com.gome.maven.ui.LightColors;
import com.gome.maven.ui.awt.RelativePoint;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * @author max
 */
public class FileLevelIntentionComponent extends EditorNotificationPanel {
    private final Project myProject;
    private final Color myBackground;

    public FileLevelIntentionComponent(final String description,
                                       final HighlightSeverity severity,
                                       final GutterMark gutterMark,
                                       final List<Pair<HighlightInfo.IntentionActionDescriptor, TextRange>> intentions,
                                       final Project project, final PsiFile psiFile, final Editor editor) {
        myProject = project;
        myBackground = getColor(severity);

        final ShowIntentionsPass.IntentionsInfo info = new ShowIntentionsPass.IntentionsInfo();

        if (intentions != null) {
            for (Pair<HighlightInfo.IntentionActionDescriptor, TextRange> intention : intentions) {
                final HighlightInfo.IntentionActionDescriptor descriptor = intention.getFirst();
                info.intentionsToShow.add(descriptor);
                final IntentionAction action = descriptor.getAction();
                if (action instanceof EmptyIntentionAction) {
                    continue;
                }
                final String text = action.getText();
                createActionLabel(text, new Runnable() {
                    @Override
                    public void run() {
                        PsiDocumentManager.getInstance(myProject).commitAllDocuments();
                        ShowIntentionActionsHandler.chooseActionAndInvoke(psiFile, editor, action, text);
                    }
                });
            }
        }

        myLabel.setText(description);
        if (gutterMark != null) {
            myLabel.setIcon(gutterMark.getIcon());
        }

        if (intentions != null && !intentions.isEmpty()) {
            myGearLabel.setIcon(AllIcons.General.Gear);

            new ClickListener() {
                @Override
                public boolean onClick( MouseEvent e, int clickCount) {
                    IntentionListStep step = new IntentionListStep(null, editor, psiFile, project);
                    HighlightInfo.IntentionActionDescriptor descriptor = intentions.get(0).getFirst();
                    IntentionActionWithTextCaching actionWithTextCaching = step.wrapAction(descriptor, psiFile, psiFile, editor);
                    if (step.hasSubstep(actionWithTextCaching)) {
                        step = step.getSubStep(actionWithTextCaching, null);
                    }
                    ListPopup popup = JBPopupFactory.getInstance().createListPopup(step);
                    Dimension dimension = popup.getContent().getPreferredSize();
                    Point at = new Point(-dimension.width + myGearLabel.getWidth(), FileLevelIntentionComponent.this.getHeight());
                    popup.show(new RelativePoint(e.getComponent(), at));
                    return true;
                }
            }.installOn(myGearLabel);
        }
    }

    @Override
    public Color getBackground() {
        return myBackground;
    }

    private  Color getColor(HighlightSeverity severity) {
        if (SeverityRegistrar.getSeverityRegistrar(myProject).compare(severity, HighlightSeverity.ERROR) >= 0) {
            return LightColors.RED;
        }

        if (SeverityRegistrar.getSeverityRegistrar(myProject).compare(severity, HighlightSeverity.WARNING) >= 0) {
            return LightColors.YELLOW;
        }

        return LightColors.GREEN;
    }
}
