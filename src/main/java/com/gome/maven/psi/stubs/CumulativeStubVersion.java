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
package com.gome.maven.psi.stubs;

import com.gome.maven.lang.Language;
import com.gome.maven.lang.LanguageParserDefinitions;
import com.gome.maven.lang.ParserDefinition;
import com.gome.maven.openapi.fileTypes.FileType;
import com.gome.maven.openapi.fileTypes.FileTypeRegistry;
import com.gome.maven.openapi.fileTypes.LanguageFileType;
import com.gome.maven.psi.tree.IFileElementType;
import com.gome.maven.psi.tree.IStubFileElementType;

public class CumulativeStubVersion {
    private static final int VERSION = 29;

    public static int getCumulativeVersion() {
        int version = VERSION;
        for (final FileType fileType : FileTypeRegistry.getInstance().getRegisteredFileTypes()) {
            if (fileType instanceof LanguageFileType) {
                Language l = ((LanguageFileType)fileType).getLanguage();
                ParserDefinition parserDefinition = LanguageParserDefinitions.INSTANCE.forLanguage(l);
                if (parserDefinition != null) {
                    final IFileElementType type = parserDefinition.getFileNodeType();
                    if (type instanceof IStubFileElementType) {
                        version += ((IStubFileElementType)type).getStubVersion();
                    }
                }
            }

            BinaryFileStubBuilder builder = BinaryFileStubBuilders.INSTANCE.forFileType(fileType);
            if (builder != null) {
                version += builder.getStubVersion();
            }
        }
        return version;
    }
}
