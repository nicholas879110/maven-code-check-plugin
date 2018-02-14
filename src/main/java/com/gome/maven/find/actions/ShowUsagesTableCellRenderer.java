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

package com.gome.maven.find.actions;

import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.psi.PsiManager;
import com.gome.maven.psi.search.SearchScope;
import com.gome.maven.ui.FileColorManager;
import com.gome.maven.ui.SimpleColoredComponent;
import com.gome.maven.ui.SimpleTextAttributes;
import com.gome.maven.usages.TextChunk;
import com.gome.maven.usages.Usage;
import com.gome.maven.usages.UsageGroup;
import com.gome.maven.usages.UsagePresentation;
import com.gome.maven.usages.impl.GroupNode;
import com.gome.maven.usages.impl.UsageNode;
import com.gome.maven.usages.impl.UsageViewImpl;
import com.gome.maven.usages.impl.UsageViewManagerImpl;
import com.gome.maven.usages.rules.UsageInFile;
import com.gome.maven.util.ui.EmptyIcon;
import com.gome.maven.util.ui.UIUtil;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author cdr
 */
class ShowUsagesTableCellRenderer implements TableCellRenderer {
    private final UsageViewImpl myUsageView;
     private final AtomicInteger myOutOfScopeUsages;
     private final SearchScope mySearchScope;

    ShowUsagesTableCellRenderer( UsageViewImpl usageView,  AtomicInteger outOfScopeUsages,  SearchScope searchScope) {
        myUsageView = usageView;
        myOutOfScopeUsages = outOfScopeUsages;
        mySearchScope = searchScope;
    }

    @Override
    public Component getTableCellRendererComponent(JTable list, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        UsageNode usageNode = value instanceof UsageNode ? (UsageNode)value : null;

        Usage usage = usageNode == null ? null : usageNode.getUsage();

        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0,0));
        Color fileBgColor = getBackgroundColor(isSelected, usage);
        final Color bg = UIUtil.getListSelectionBackground();
        final Color fg = UIUtil.getListSelectionForeground();
        Color panelBackground = isSelected ? bg : fileBgColor == null ? list.getBackground() : fileBgColor;
        Color panelForeground = isSelected ? fg : list.getForeground();
        panel.setBackground(panelBackground);
        panel.setForeground(panelForeground);

        SimpleColoredComponent textChunks = new SimpleColoredComponent();
        textChunks.setIpad(new Insets(0, 0, 0, 0));
        textChunks.setBorder(null);

        if (usage == null || usageNode instanceof ShowUsagesAction.StringNode) {
            textChunks.append(value != null ? value.toString() : "", SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
            return textComponentSpanningWholeRow(textChunks, panelBackground, panelForeground, column, list, row);
        }
        if (usage == ShowUsagesAction.MORE_USAGES_SEPARATOR) {
            textChunks.append("...<");
            textChunks.append("more usages", SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
            textChunks.append(">...");
            return textComponentSpanningWholeRow(textChunks, panelBackground, panelForeground, column, list, row);
        }
        else if (usage == ShowUsagesAction.USAGES_OUTSIDE_SCOPE_SEPARATOR) {
            textChunks.append("...<");
            textChunks.append(UsageViewManagerImpl.outOfScopeMessage(myOutOfScopeUsages.get(), mySearchScope), SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
            textChunks.append(">...");
            return textComponentSpanningWholeRow(textChunks, panelBackground, panelForeground, column, list, row);
        }

        if (column == 0) {
            GroupNode parent = (GroupNode)usageNode.getParent();
            appendGroupText(parent, panel, fileBgColor);
        }
        else if (usage != ShowUsagesAction.MORE_USAGES_SEPARATOR && usage != ShowUsagesAction.USAGES_OUTSIDE_SCOPE_SEPARATOR) {
            UsagePresentation presentation = usage.getPresentation();
            TextChunk[] text = presentation.getText();

            if (column == 1) {
                final Icon icon = presentation.getIcon();
                textChunks.setIcon(icon == null ? EmptyIcon.ICON_16 : icon);
                if (text.length != 0) {
                    SimpleTextAttributes attributes = isSelected ?
                            new SimpleTextAttributes(bg, fg, fg, SimpleTextAttributes.STYLE_ITALIC) :
                            deriveAttributesWithColor(text[0].getSimpleAttributesIgnoreBackground(), fileBgColor);
                    textChunks.append(text[0].getText(), attributes);
                }
            }
            else if (column == 2) {
                for (int i = 1; i < text.length; i++) {
                    TextChunk textChunk = text[i];
                    final SimpleTextAttributes attrs = textChunk.getSimpleAttributesIgnoreBackground();
                    SimpleTextAttributes attributes = isSelected ?
                            new SimpleTextAttributes(bg, fg, fg, attrs.getStyle()) : deriveAttributesWithColor(attrs, fileBgColor);
                    textChunks.append(textChunk.getText(), attributes);
                }
            }
            else {
                assert false : column;
            }
        }
        panel.add(textChunks);
        return panel;
    }

    
    private static Component textComponentSpanningWholeRow( SimpleColoredComponent chunks,
                                                           Color panelBackground,
                                                           Color panelForeground,
                                                           final int column,
                                                            final JTable table, int row) {
        final SimpleColoredComponent component = new SimpleColoredComponent() {
            @Override
            protected void doPaint(Graphics2D g) {
                int offset = 0;
                int i = 0;
                final TableColumnModel columnModel = table.getColumnModel();
                while (i < column) {
                    offset += columnModel.getColumn(i).getWidth();
                    i++;
                }
                g.translate(-offset, 0);

                //if (column == columnModel.getColumnCount()-1) {
                //}
                setSize(getWidth()+offset, getHeight()); // should increase the column width so that selection background will be visible even after offset translation

                super.doPaint(g);

                g.translate(+offset, 0);
            }

            
            @Override
            public Dimension getPreferredSize() {
                //return super.getPreferredSize();
                return column == table.getColumnModel().getColumnCount()-1 ? super.getPreferredSize() : new Dimension(0,0);
                // it should span the whole row, so we can't return any specific value here,
                // because otherwise it would be used in the "max width" calculation in com.intellij.find.actions.ShowUsagesAction.calcMaxWidth
            }
        };

        component.setIpad(new Insets(0,0,0,0));
        component.setBorder(null);
        component.setBackground(panelBackground);
        component.setForeground(panelForeground);

        for (SimpleColoredComponent.ColoredIterator iterator = chunks.iterator(); iterator.hasNext(); ) {
            iterator.next();
            String fragment = iterator.getFragment();
            SimpleTextAttributes attributes = iterator.getTextAttributes();
            attributes = attributes.derive(attributes.getStyle(), panelForeground, panelBackground, attributes.getWaveColor());
            component.append(fragment, attributes);
        }

        return component;
    }

    private static SimpleTextAttributes deriveAttributesWithColor(SimpleTextAttributes attributes, Color fileBgColor) {
        if (fileBgColor != null) {
            attributes = attributes.derive(-1,null, fileBgColor,null);
        }
        return attributes;
    }

    private Color getBackgroundColor(boolean isSelected, Usage usage) {
        Color fileBgColor = null;
        if (isSelected) {
            fileBgColor = UIUtil.getListSelectionBackground();
        }
        else {
            VirtualFile virtualFile = usage instanceof UsageInFile ? ((UsageInFile)usage).getFile() : null;
            if (virtualFile != null) {
                Project project = myUsageView.getProject();
                PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
                if (psiFile != null && psiFile.isValid()) {
                    final Color color = FileColorManager.getInstance(project).getRendererBackground(psiFile);
                    if (color != null) fileBgColor = color;
                }
            }
        }
        return fileBgColor;
    }

    private void appendGroupText(final GroupNode node, JPanel panel, Color fileBgColor) {
        UsageGroup group = node == null ? null : node.getGroup();
        if (group == null) return;
        GroupNode parentGroup = (GroupNode)node.getParent();
        appendGroupText(parentGroup, panel, fileBgColor);
        if (node.canNavigateToSource()) {
            SimpleColoredComponent renderer = new SimpleColoredComponent();

            renderer.setIcon(group.getIcon(false));
            SimpleTextAttributes attributes = deriveAttributesWithColor(SimpleTextAttributes.REGULAR_ATTRIBUTES, fileBgColor);
            renderer.append(group.getText(myUsageView), attributes);
            renderer.append(" ", attributes);
            renderer.setIpad(new Insets(0,0,0,0));
            renderer.setBorder(null);
            panel.add(renderer);
        }
    }
}
