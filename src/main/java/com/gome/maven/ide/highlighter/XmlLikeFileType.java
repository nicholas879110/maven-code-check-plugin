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
package com.gome.maven.ide.highlighter;

import com.gome.maven.lang.Language;
import com.gome.maven.openapi.fileTypes.LanguageFileType;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.vfs.CharsetToolkit;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.util.text.XmlCharsetDetector;

import java.nio.charset.Charset;

public abstract class XmlLikeFileType extends LanguageFileType {
    public XmlLikeFileType(Language language) {
        super(language);
    }
    @Override
    public String getCharset( VirtualFile file,  final byte[] content) {
        String charset = XmlCharsetDetector.extractXmlEncodingFromProlog(content);
        return charset == null ? CharsetToolkit.UTF8 : charset;
    }

    @Override
    public Charset extractCharsetFromFileContent(final Project project,  final VirtualFile file,  final CharSequence content) {
        String name = XmlCharsetDetector.extractXmlEncodingFromProlog(content);
        Charset charset = CharsetToolkit.forName(name);
        return charset == null ? CharsetToolkit.UTF8_CHARSET : charset;
    }

    public boolean isCaseSensitive() {
        return false;
    }
}
