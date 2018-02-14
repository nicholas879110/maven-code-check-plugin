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

package com.gome.maven.ide.macro;

import com.gome.maven.ide.IdeBundle;
import com.gome.maven.openapi.actionSystem.CommonDataKeys;
import com.gome.maven.openapi.actionSystem.DataContext;
import com.gome.maven.openapi.actionSystem.PlatformDataKeys;
import com.gome.maven.openapi.vfs.VirtualFile;

public final class FileEncodingMacro extends Macro {
    @Override
    public String getName() {
        return "FileEncoding";
    }

    @Override
    public String getDescription() {
        return IdeBundle.message("macro.file.encoding");
    }

    @Override
    public String expand(DataContext dataContext) {
        VirtualFile file = CommonDataKeys.VIRTUAL_FILE.getData(dataContext);
        if (file == null) return null;
        return file.getCharset().displayName();
    }
}
