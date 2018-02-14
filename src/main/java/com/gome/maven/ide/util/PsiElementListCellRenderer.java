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

package com.gome.maven.ide.util;

import com.gome.maven.ide.ui.UISettings;
import com.gome.maven.navigation.ColoredItemPresentation;
import com.gome.maven.navigation.ItemPresentation;
import com.gome.maven.navigation.NavigationItem;
import com.gome.maven.openapi.editor.colors.EditorColorsManager;
import com.gome.maven.openapi.editor.colors.TextAttributesKey;
import com.gome.maven.openapi.editor.markup.EffectType;
import com.gome.maven.openapi.editor.markup.TextAttributes;
import com.gome.maven.openapi.ui.popup.PopupChooserBuilder;
import com.gome.maven.openapi.util.Iconable;
import com.gome.maven.openapi.vcs.FileStatus;
import com.gome.maven.openapi.vcs.FileStatusManager;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.problems.WolfTheProblemSolver;
import com.gome.maven.psi.PsiDirectory;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.ui.*;
import com.gome.maven.ui.speedSearch.SpeedSearchUtil;
import com.gome.maven.util.Function;
import com.gome.maven.util.IconUtil;
import com.gome.maven.util.text.Matcher;
import com.gome.maven.util.text.MatcherHolder;
import com.gome.maven.util.ui.UIUtil;

import javax.swing.*;
import java.awt.*;
import java.util.Comparator;

public abstract class PsiElementListCellRenderer<T extends PsiElement> extends JPanel implements ListCellRenderer, MatcherHolder {

    private Matcher myMatcher;
    private boolean myFocusBorderEnabled = true;
    protected int myRightComponentWidth;

    protected PsiElementListCellRenderer() {
        super(new BorderLayout());
    }

    @Override
    public void setPatternMatcher(final Matcher matcher) {
        myMatcher = matcher;
    }

    protected static Color getBackgroundColor( Object value) {
        if (value instanceof PsiElement) {
            final PsiElement psiElement = (PsiElement)value;
            final FileColorManager colorManager = FileColorManager.getInstance(psiElement.getProject());

            if (colorManager.isEnabled()) {
                VirtualFile file = null;
                PsiFile psiFile = psiElement.getContainingFile();

                if (psiFile != null) {
                    file = psiFile.getVirtualFile();
                } else if (psiElement instanceof PsiDirectory) {
                    file = ((PsiDirectory)psiElement).getVirtualFile();
                }
                final Color fileBgColor = file != null ? colorManager.getRendererBackground(file) : null;

                if (fileBgColor != null) {
                    return fileBgColor;
                }
            }
        }

        return UIUtil.getListBackground();
    }

    private class LeftRenderer extends ColoredListCellRenderer {
        private final String myModuleName;
        private final Matcher myMatcher;

        public LeftRenderer(final String moduleName, Matcher matcher) {
            myModuleName = moduleName;
            myMatcher = matcher;
        }

        @Override
        protected void customizeCellRenderer(JList list, Object value, int index, boolean selected, boolean hasFocus) {
            Color bgColor = UIUtil.getListBackground();
            Color color = list.getForeground();
            setPaintFocusBorder(hasFocus && UIUtil.isToUseDottedCellBorder() && myFocusBorderEnabled);
            if (value instanceof PsiElement) {
                T element = (T)value;
                String name = element.isValid() ? getElementText(element) : "INVALID";
                PsiFile psiFile = element.isValid() ? element.getContainingFile() : null;
                boolean isProblemFile = false;

                if (psiFile != null) {
                    VirtualFile vFile = psiFile.getVirtualFile();
                    if (vFile != null) {
                        if (WolfTheProblemSolver.getInstance(psiFile.getProject()).isProblemFile(vFile)) {
                            isProblemFile = true;
                        }
                        FileStatus status = FileStatusManager.getInstance(psiFile.getProject()).getStatus(vFile);
                        color = status.getColor();

                        final FileColorManager colorManager = FileColorManager.getInstance(psiFile.getProject());
                        if (colorManager.isEnabled()) {
                            final Color fileBgColor = colorManager.getRendererBackground(psiFile);
                            bgColor = fileBgColor == null ? bgColor : fileBgColor;
                        }
                    }
                }

                TextAttributes attributes = getNavigationItemAttributes(value);

                if (isProblemFile) {
                    attributes = TextAttributes.merge(new TextAttributes(color, null, JBColor.RED, EffectType.WAVE_UNDERSCORE, Font.PLAIN), attributes);
                }

                SimpleTextAttributes nameAttributes = attributes != null ? SimpleTextAttributes.fromTextAttributes(attributes) : null;

                if (nameAttributes == null) nameAttributes = new SimpleTextAttributes(Font.PLAIN, color);

                assert name != null : "Null name for PSI element " + element + " (by " + PsiElementListCellRenderer.this + ")";
                SpeedSearchUtil.appendColoredFragmentForMatcher(name,  this, nameAttributes, myMatcher, bgColor, selected);
                if (!element.isValid()) {
                    append(" Invalid", SimpleTextAttributes.ERROR_ATTRIBUTES);
                    return;
                }
                setIcon(PsiElementListCellRenderer.this.getIcon(element));

                String containerText = getContainerTextForLeftComponent(element, name + (myModuleName != null ? myModuleName + "        " : ""));
                if (containerText != null) {
                    append(" " + containerText, new SimpleTextAttributes(Font.PLAIN, JBColor.GRAY));
                }
            }
            else if (!customizeNonPsiElementLeftRenderer(this, list, value, index, selected, hasFocus)) {
                setIcon(IconUtil.getEmptyIcon(false));
                append(value == null ? "" : value.toString(), new SimpleTextAttributes(Font.PLAIN, list.getForeground()));
            }
            setBackground(selected ? UIUtil.getListSelectionBackground() : bgColor);
        }

    }

    
    protected static TextAttributes getNavigationItemAttributes(Object value) {
        TextAttributes attributes = null;

        if (value instanceof NavigationItem) {
            TextAttributesKey attributesKey = null;
            final ItemPresentation presentation = ((NavigationItem)value).getPresentation();
            if (presentation instanceof ColoredItemPresentation) attributesKey = ((ColoredItemPresentation) presentation).getTextAttributesKey();

            if (attributesKey != null) {
                attributes = EditorColorsManager.getInstance().getGlobalScheme().getAttributes(attributesKey);
            }
        }
        return attributes;
    }

    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        removeAll();
        myRightComponentWidth = 0;
        DefaultListCellRenderer rightRenderer = getRightCellRenderer(value);
        Component rightCellRendererComponent = null;
        JPanel spacer = null;
        if (rightRenderer != null) {
            rightCellRendererComponent = rightRenderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            add(rightCellRendererComponent, BorderLayout.EAST);
            spacer = new JPanel();
            spacer.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
            add(spacer, BorderLayout.CENTER);
            myRightComponentWidth = rightCellRendererComponent.getPreferredSize().width;
            myRightComponentWidth += spacer.getPreferredSize().width;
        }

        ListCellRenderer leftRenderer = new LeftRenderer(null, myMatcher);
        final Component leftCellRendererComponent = leftRenderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        add(leftCellRendererComponent, BorderLayout.WEST);
        final Color bg = isSelected ? UIUtil.getListSelectionBackground() : leftCellRendererComponent.getBackground();
        setBackground(bg);
        if (rightCellRendererComponent != null) {
            rightCellRendererComponent.setBackground(bg);
        }
        if (spacer != null) {
            spacer.setBackground(bg);
        }
        return this;
    }

    protected void setFocusBorderEnabled(boolean enabled) {
        myFocusBorderEnabled = enabled;
    }

    protected boolean customizeNonPsiElementLeftRenderer(ColoredListCellRenderer renderer,
                                                         JList list,
                                                         Object value,
                                                         int index,
                                                         boolean selected,
                                                         boolean hasFocus) {
        return false;
    }

    
    protected DefaultListCellRenderer getRightCellRenderer(final Object value) {
        if (UISettings.getInstance().SHOW_ICONS_IN_QUICK_NAVIGATION) {
            final DefaultListCellRenderer renderer = ModuleRendererFactory.findInstance(value).getModuleRenderer();
            if (renderer instanceof PlatformModuleRendererFactory.PlatformModuleRenderer) {
                // it won't display any new information
                return null;
            }
            return renderer;
        }
        return null;
    }

    public abstract String getElementText(T element);

    
    protected abstract String getContainerText(T element, final String name);

    
    protected String getContainerTextForLeftComponent(T element, final String name) {
        return getContainerText(element, name);
    }

    @Iconable.IconFlags
    protected abstract int getIconFlags();

    protected Icon getIcon(PsiElement element) {
        return element.getIcon(getIconFlags());
    }

    public Comparator<T> getComparator() {
        return new Comparator<T>() {
            @Override
            public int compare(T o1, T o2) {
                return getComparingObject(o1).compareTo(getComparingObject(o2));
            }
        };
    }

    
    public Comparable getComparingObject(T element) {
        String elementText = getElementText(element);
        String containerText = getContainerText(element, elementText);
        return containerText != null ? elementText + " " + containerText : elementText;
    }

    public void installSpeedSearch(PopupChooserBuilder builder) {
        installSpeedSearch(builder, false);
    }

    public void installSpeedSearch(PopupChooserBuilder builder, final boolean includeContainerText) {
        builder.setFilteringEnabled(new Function<Object, String>() {
            @Override
            public String fun(Object o) {
                if (o instanceof PsiElement) {
                    final String elementText = getElementText((T)o);
                    if (includeContainerText) {
                        return elementText + " " + getContainerText((T)o, elementText);
                    }
                    return elementText;
                }
                else {
                    return o.toString();
                }
            }
        });
    }

    /**
     * User {@link #installSpeedSearch(com.gome.maven.openapi.ui.popup.PopupChooserBuilder)} instead
     */
    @Deprecated
    public void installSpeedSearch(JList list) {
        new ListSpeedSearch(list) {
            @Override
            protected String getElementText(Object o) {
                if (o instanceof PsiElement) {
                    final String elementText = PsiElementListCellRenderer.this.getElementText((T)o);
                    return elementText + " " + getContainerText((T)o, elementText);
                }
                else {
                    return o.toString();
                }
            }
        };
    }
}