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
package com.gome.maven.openapi.fileTypes.impl;

import com.gome.maven.openapi.fileTypes.FileType;
import com.gome.maven.openapi.fileTypes.FileTypeManager;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.ui.LayeredIcon;
import com.gome.maven.ui.ListCellRendererWrapper;
import com.gome.maven.util.ui.EmptyIcon;

import javax.swing.*;
import java.util.Arrays;
import java.util.List;

public class FileTypeRenderer extends ListCellRendererWrapper<FileType> {
    private static final Icon EMPTY_ICON = EmptyIcon.ICON_18;

    public interface FileTypeListProvider {
        Iterable<FileType> getCurrentFileTypeList();
    }

    private final FileTypeListProvider myFileTypeListProvider;

    public FileTypeRenderer() {
        this(new DefaultFileTypeListProvider());
    }

    public FileTypeRenderer( FileTypeListProvider fileTypeListProvider) {
        super();
        myFileTypeListProvider = fileTypeListProvider;
    }

    /** @deprecated use {@link #FileTypeRenderer()} (to remove in IDEA 14) */
    @SuppressWarnings("UnusedDeclaration")
    public FileTypeRenderer(final ListCellRenderer renderer) {
        this();
    }

    /** @deprecated use {@link #FileTypeRenderer(FileTypeListProvider)} (to remove in IDEA 14) */
    @SuppressWarnings("UnusedDeclaration")
    public FileTypeRenderer(final ListCellRenderer renderer, final FileTypeListProvider fileTypeListProvider) {
        this(fileTypeListProvider);
    }

    @Override
    public void customize(JList list, FileType type, int index, boolean selected, boolean hasFocus) {
        LayeredIcon layeredIcon = new LayeredIcon(2);
        layeredIcon.setIcon(EMPTY_ICON, 0);
        final Icon icon = type.getIcon();
        if (icon != null) {
            layeredIcon.setIcon(icon, 1, (- icon.getIconWidth() + EMPTY_ICON.getIconWidth())/2, (EMPTY_ICON.getIconHeight() - icon.getIconHeight())/2);
        }

        setIcon(layeredIcon);

        String description = type.getDescription();
        String trimmedDescription = StringUtil.capitalizeWords(description.replaceAll("(?i)\\s*file(?:s)?$", ""), true);
        if (isDuplicated(description)) {
            setText(trimmedDescription + " (" + type.getName() + ")");

        }
        else {
            setText(trimmedDescription);
        }
    }

    private boolean isDuplicated(final String description) {
        boolean found = false;

        for (FileType type : myFileTypeListProvider.getCurrentFileTypeList()) {
            if (description.equals(type.getDescription())) {
                if (!found) {
                    found = true;
                }
                else {
                    return true;
                }
            }
        }
        return false;
    }

    private static class DefaultFileTypeListProvider implements FileTypeListProvider {
        private final List<FileType> myFileTypes;

        public DefaultFileTypeListProvider() {
            myFileTypes = Arrays.asList(FileTypeManager.getInstance().getRegisteredFileTypes());
        }

        public Iterable<FileType> getCurrentFileTypeList() {
            return myFileTypes;
        }
    }
}
