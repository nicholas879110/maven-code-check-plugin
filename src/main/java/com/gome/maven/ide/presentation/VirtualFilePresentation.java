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
package com.gome.maven.ide.presentation;

import com.gome.maven.ide.TypePresentationService;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.util.IconUtil;
import com.gome.maven.util.PlatformIcons;

import javax.swing.*;

/**
 * @author yole
 */
public class VirtualFilePresentation {

    public static Icon getIcon( VirtualFile vFile) {
        return IconUtil.getIcon(vFile, 0, null);
    }

    public static Icon getIconImpl( VirtualFile vFile) {
        Icon icon = TypePresentationService.getService().getIcon(vFile);
        if (icon != null) {
            return icon;
        }
        if (vFile.isDirectory() && vFile.isInLocalFileSystem()) {
            return PlatformIcons.FOLDER_ICON;
        }
        return vFile.getFileType().getIcon();
    }
}
