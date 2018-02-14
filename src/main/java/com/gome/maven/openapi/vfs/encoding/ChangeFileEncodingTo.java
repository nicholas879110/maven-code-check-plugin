/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package com.gome.maven.openapi.vfs.encoding;

import com.gome.maven.openapi.actionSystem.AnAction;
import com.gome.maven.openapi.actionSystem.AnActionEvent;
import com.gome.maven.openapi.project.DumbAware;
import com.gome.maven.openapi.vfs.VirtualFile;

import java.nio.charset.Charset;

/**
 * @author cdr
 */
abstract class ChangeFileEncodingTo extends AnAction implements DumbAware {
    private final VirtualFile myFile;
    private final Charset myCharset;

    ChangeFileEncodingTo( VirtualFile file,  Charset charset) {
        super(charset.displayName());
        myFile = file;
        myCharset = charset;
    }

    @Override
    public void actionPerformed(final AnActionEvent e) {
        chosen(myFile, myCharset);
    }

    protected abstract void chosen( VirtualFile file,  Charset charset);
}
