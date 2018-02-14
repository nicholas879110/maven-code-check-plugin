/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

package com.gome.maven.ide.util.gotoByName;

import com.gome.maven.ide.util.PlatformModuleRendererFactory;
import com.gome.maven.ide.util.PsiElementListCellRenderer;
import com.gome.maven.navigation.ItemPresentation;
import com.gome.maven.navigation.NavigationItem;
import com.gome.maven.openapi.editor.colors.EditorColorsManager;
import com.gome.maven.openapi.editor.colors.EditorColorsScheme;
import com.gome.maven.openapi.editor.markup.TextAttributes;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.roots.ProjectFileIndex;
import com.gome.maven.openapi.util.Iconable;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.openapi.vfs.VfsUtilCore;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.psi.PsiDirectory;
import com.gome.maven.psi.PsiFileSystemItem;
import com.gome.maven.ui.ColoredListCellRenderer;
import com.gome.maven.ui.JBColor;
import com.gome.maven.ui.SimpleTextAttributes;
import com.gome.maven.util.ui.FilePathSplittingPolicy;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class GotoFileCellRenderer extends PsiElementListCellRenderer<PsiFileSystemItem> {
    private final int myMaxWidth;

    public GotoFileCellRenderer(int maxSize) {
        myMaxWidth = maxSize;
    }

    @Override
    public String getElementText(PsiFileSystemItem element) {
        return element.getName();
    }

    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        if (value == ChooseByNameBase.NON_PREFIX_SEPARATOR) {
            Object previousElement = index > 0 ? list.getModel().getElementAt(index - 1) : null;
            return ChooseByNameBase.renderNonPrefixSeparatorComponent(getBackgroundColor(previousElement));
        }
        else {
            Component component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            EditorColorsScheme scheme = EditorColorsManager.getInstance().getGlobalScheme();
            Font editorFont = new Font(scheme.getEditorFontName(), Font.PLAIN, scheme.getEditorFontSize());
            setFont(editorFont);
            return component;
        }
    }

    @Override
    protected String getContainerText(PsiFileSystemItem element, String name) {
        PsiFileSystemItem parent = element.getParent();
        final PsiDirectory psiDirectory = parent instanceof PsiDirectory ? (PsiDirectory)parent : null;
        if (psiDirectory == null) return null;
        final VirtualFile virtualFile = psiDirectory.getVirtualFile();
        final String relativePath = getRelativePath(virtualFile, element.getProject());
        if (relativePath == null) return "( " + File.separator + " )";
        String path =
                FilePathSplittingPolicy.SPLIT_BY_SEPARATOR.getOptimalTextForComponent(name + "          ", new File(relativePath), this, myMaxWidth);
        return "(" + path + ")";
    }


    static String getRelativePath(final VirtualFile virtualFile, final Project project) {
        String url = virtualFile.getPresentableUrl();
        if (project == null) {
            return url;
        }
        VirtualFile root = ProjectFileIndex.SERVICE.getInstance(project).getContentRootForFile(virtualFile);
        if (root != null) {
            return root.getName() + File.separatorChar + VfsUtilCore.getRelativePath(virtualFile, root, File.separatorChar);
        }

        final VirtualFile baseDir = project.getBaseDir();
        if (baseDir != null) {
            //noinspection ConstantConditions
            final String projectHomeUrl = baseDir.getPresentableUrl();
            if (url.startsWith(projectHomeUrl)) {
                final String cont = url.substring(projectHomeUrl.length());
                if (cont.isEmpty()) return null;
                url = "..." + cont;
            }
        }
        return url;
    }

    @Override
    protected boolean customizeNonPsiElementLeftRenderer(ColoredListCellRenderer renderer,
                                                         JList list,
                                                         Object value,
                                                         int index,
                                                         boolean selected,
                                                         boolean hasFocus) {
        if (!(value instanceof NavigationItem)) return false;

        NavigationItem item = (NavigationItem)value;

        TextAttributes attributes = getNavigationItemAttributes(item);

        SimpleTextAttributes nameAttributes = attributes != null ? SimpleTextAttributes.fromTextAttributes(attributes) : null;

        Color color = list.getForeground();
        if (nameAttributes == null) nameAttributes = new SimpleTextAttributes(Font.PLAIN, color);

        renderer.append(item + " ", nameAttributes);
        ItemPresentation itemPresentation = item.getPresentation();
        assert itemPresentation != null;
        renderer.setIcon(itemPresentation.getIcon(true));

        String locationString = itemPresentation.getLocationString();
        if (!StringUtil.isEmpty(locationString)) {
            renderer.append(locationString, new SimpleTextAttributes(Font.PLAIN, JBColor.GRAY));
        }
        return true;
    }

    @Override
    protected DefaultListCellRenderer getRightCellRenderer(final Object value) {
        final DefaultListCellRenderer rightRenderer = super.getRightCellRenderer(value);
        if (rightRenderer instanceof PlatformModuleRendererFactory.PlatformModuleRenderer) {
            // that renderer will display file path, but we're showing it ourselves - no need to show twice
            return null;
        }
        return rightRenderer;
    }

    @Override
    protected int getIconFlags() {
        return Iconable.ICON_FLAG_READ_STATUS;
    }
}
