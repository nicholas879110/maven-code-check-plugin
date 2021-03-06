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
package com.gome.maven.usages.impl;

import com.gome.maven.usageView.UsageViewBundle;
import com.gome.maven.usages.TextChunk;
import com.gome.maven.usages.UsageGroup;
import com.gome.maven.usages.UsageViewSettings;
import com.gome.maven.util.SystemProperties;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import javax.swing.tree.DefaultMutableTreeNode;
import java.util.Enumeration;
import java.util.TooManyListenersException;

/**
 * @author max
 */
class ExporterToTextFile implements com.gome.maven.ide.ExporterToTextFile {
    private final UsageViewImpl myUsageView;

    public ExporterToTextFile( UsageViewImpl usageView) {
        myUsageView = usageView;
    }

    @Override
    public JComponent getSettingsEditor() {
        return null;
    }

    @Override
    public void addSettingsChangedListener(ChangeListener listener) throws TooManyListenersException {
    }

    @Override
    public void removeSettingsChangedListener(ChangeListener listener) {
    }

    @Override
    public String getReportText() {
        StringBuilder buf = new StringBuilder();
        appendNode(buf, myUsageView.getModelRoot(), SystemProperties.getLineSeparator(), "");
        return buf.toString();
    }

    private void appendNode(StringBuilder buf, DefaultMutableTreeNode node, String lineSeparator, String indent) {
        buf.append(indent);
        final String childIndent;
        if (node.getParent() != null) {
            childIndent = indent + "    ";
            appendNodeText(buf, node, lineSeparator);
        }
        else {
            childIndent = indent;
        }

        Enumeration enumeration = node.children();
        while (enumeration.hasMoreElements()) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode)enumeration.nextElement();
            appendNode(buf, child, lineSeparator, childIndent);
        }
    }

    private void appendNodeText(StringBuilder buf, DefaultMutableTreeNode node, String lineSeparator) {
        if (node instanceof Node && ((Node)node).isExcluded()) {
            buf.append("(").append(UsageViewBundle.message("usage.excluded")).append(") ");
        }

        if (node instanceof UsageNode) {
            TextChunk[] chunks = ((UsageNode)node).getUsage().getPresentation().getText();
            for (TextChunk chunk : chunks) {
                buf.append(chunk.getText());
            }
        }
        else if (node instanceof GroupNode) {
            UsageGroup group = ((GroupNode)node).getGroup();
            buf.append(group != null ? group.getText(myUsageView) : UsageViewBundle.message("usages.title"));
            buf.append(" ");
            int count = ((GroupNode)node).getRecursiveUsageCount();
            buf.append(" (").append(UsageViewBundle.message("usages.n", count)).append(")");
        }
        else if (node instanceof UsageTargetNode) {
            buf.append(((UsageTargetNode)node).getTarget().getPresentation().getPresentableText());
        }
        else {
            buf.append(node.toString());
        }
        buf.append(lineSeparator);
    }

    @Override
    public String getDefaultFilePath() {
        return UsageViewSettings.getInstance().EXPORT_FILE_NAME;
    }

    @Override
    public void exportedTo(String filePath) {
        UsageViewSettings.getInstance().EXPORT_FILE_NAME = filePath;
    }

    @Override
    public boolean canExport() {
        return !myUsageView.isSearchInProgress() && myUsageView.areTargetsValid();
    }
}
