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
package com.gome.maven.openapi.vcs.changes.committed;

import com.gome.maven.codeInsight.hint.HintUtil;
import com.gome.maven.ide.DataManager;
import com.gome.maven.openapi.actionSystem.AnAction;
import com.gome.maven.openapi.actionSystem.AnActionEvent;
import com.gome.maven.openapi.actionSystem.CommonDataKeys;
import com.gome.maven.openapi.project.DumbAware;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.ui.popup.JBPopup;
import com.gome.maven.openapi.ui.popup.JBPopupFactory;
import com.gome.maven.openapi.vcs.*;
import com.gome.maven.openapi.vcs.changes.ChangeList;
import com.gome.maven.openapi.vcs.changes.issueLinks.IssueLinkHtmlRenderer;
import com.gome.maven.openapi.vcs.versionBrowser.CommittedChangeList;
import com.gome.maven.ui.BrowserHyperlinkListener;
import com.gome.maven.ui.ScrollPaneFactory;
import com.gome.maven.util.text.DateFormatUtil;
import com.gome.maven.util.ui.UIUtil;
import com.gome.maven.xml.util.XmlStringUtil;

import javax.swing.*;

/**
 * @author yole
 */
public class ChangeListDetailsAction extends AnAction implements DumbAware {
    public void actionPerformed(AnActionEvent e) {
        final Project project = e.getData(CommonDataKeys.PROJECT);
        final ChangeList[] changeLists = e.getData(VcsDataKeys.CHANGE_LISTS);
        if (changeLists != null && changeLists.length > 0 && changeLists [0] instanceof CommittedChangeList) {
            showDetailsPopup(project, (CommittedChangeList) changeLists [0]);
        }
    }

    public void update(final AnActionEvent e) {
        final Project project = e.getData(CommonDataKeys.PROJECT);
        final ChangeList[] changeLists = e.getData(VcsDataKeys.CHANGE_LISTS);
        e.getPresentation().setEnabled(project != null && changeLists != null && changeLists.length == 1 &&
                changeLists [0] instanceof CommittedChangeList);
    }

    public static void showDetailsPopup(final Project project, final CommittedChangeList changeList) {
        StringBuilder detailsBuilder = new StringBuilder("<html><head>");
        detailsBuilder.append(UIUtil.getCssFontDeclaration(UIUtil.getLabelFont())).append("</head><body>");
        final AbstractVcs vcs = changeList.getVcs();
        CachingCommittedChangesProvider provider = null;
        if (vcs != null) {
            provider = vcs.getCachingCommittedChangesProvider();
            if (provider != null && provider.getChangelistTitle() != null) {
                detailsBuilder.append(provider.getChangelistTitle()).append(" #").append(changeList.getNumber()).append("<br>");
            }
        }
         String committer = "<b>" + changeList.getCommitterName() + "</b>";
        detailsBuilder.append(VcsBundle.message("changelist.details.committed.format", committer,
                DateFormatUtil.formatPrettyDateTime(changeList.getCommitDate())));
        detailsBuilder.append("<br>");

        if (provider != null) {
            final CommittedChangeList originalChangeList = ReceivedChangeList.unwrap(changeList);
            for(ChangeListColumn column: provider.getColumns()) {
                if (ChangeListColumn.isCustom(column)) {
                    String value = column.getValue(originalChangeList).toString();
                    if (value.length() == 0) {
                        value = "<none>";
                    }
                    detailsBuilder.append(column.getTitle()).append(": ").append(XmlStringUtil.escapeString(value)).append("<br>");
                }
            }
        }

        detailsBuilder.append(IssueLinkHtmlRenderer.formatTextWithLinks(project, changeList.getComment()));
        detailsBuilder.append("</body></html>");

        JEditorPane editorPane = new JEditorPane(UIUtil.HTML_MIME, detailsBuilder.toString());
        editorPane.setEditable(false);
        editorPane.setBackground(HintUtil.INFORMATION_COLOR);
        editorPane.select(0, 0);
        editorPane.addHyperlinkListener(BrowserHyperlinkListener.INSTANCE);
        JScrollPane scrollPane = ScrollPaneFactory.createScrollPane(editorPane);
        final JBPopup hint =
                JBPopupFactory.getInstance().createComponentPopupBuilder(scrollPane, editorPane)
                        .setDimensionServiceKey(project, "changelist.details.popup", false)
                        .setResizable(true)
                        .setMovable(true)
                        .setRequestFocus(true)
                        .setTitle(VcsBundle.message("changelist.details.title"))
                        .createPopup();
        hint.showInBestPositionFor(DataManager.getInstance().getDataContext());
    }

}
