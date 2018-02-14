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
package com.gome.maven.openapi.editor;

import com.gome.maven.openapi.extensions.ExtensionPointName;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.vfs.VirtualFile;

import java.util.Collection;

/**
 * @author Konstantin Bulenkov
 */
public abstract class EditorLinePainter {
    public static final ExtensionPointName<EditorLinePainter> EP_NAME = ExtensionPointName.create("com.gome.maven.editor.linePainter");

    public abstract Collection<LineExtensionInfo> getLineExtensions( Project project,  VirtualFile file, int lineNumber);
}
