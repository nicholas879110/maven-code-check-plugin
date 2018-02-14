/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package com.gome.maven.psi;

import com.gome.maven.lang.ASTFactory;
import com.gome.maven.lang.ASTNode;
import com.gome.maven.openapi.fileTypes.PlainTextLanguage;
import com.gome.maven.psi.tree.IElementType;
import com.gome.maven.psi.tree.IFileElementType;

public class PlainTextTokenTypes {
    public static final IElementType PLAIN_TEXT_FILE = new IFileElementType("PLAIN_TEXT_FILE", PlainTextLanguage.INSTANCE) {
        @Override
        public ASTNode parseContents(ASTNode chameleon) {
            return ASTFactory.leaf(PLAIN_TEXT, chameleon.getChars());
        }
    };

    public static final IElementType PLAIN_TEXT = new IElementType("PLAIN_TEXT", PlainTextLanguage.INSTANCE);

    private PlainTextTokenTypes() {
    }
}
