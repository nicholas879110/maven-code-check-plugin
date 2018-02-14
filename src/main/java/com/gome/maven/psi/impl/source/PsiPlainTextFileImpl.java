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

package com.gome.maven.psi.impl.source;

import com.gome.maven.openapi.fileTypes.FileType;
import com.gome.maven.openapi.fileTypes.PlainTextFileType;
import com.gome.maven.openapi.fileTypes.PlainTextLanguage;
import com.gome.maven.psi.*;
import com.gome.maven.psi.impl.source.resolve.reference.ReferenceProvidersRegistry;

public class PsiPlainTextFileImpl extends PsiFileImpl implements PsiPlainTextFile{
    private final FileType myFileType;

    public PsiPlainTextFileImpl(FileViewProvider viewProvider) {
        super(PlainTextTokenTypes.PLAIN_TEXT_FILE, PlainTextTokenTypes.PLAIN_TEXT_FILE, viewProvider);
        myFileType = viewProvider.getBaseLanguage() != PlainTextLanguage.INSTANCE ? PlainTextFileType.INSTANCE : viewProvider.getVirtualFile().getFileType();
    }

    @Override
    public void accept( PsiElementVisitor visitor){
        visitor.visitPlainTextFile(this);
    }

    public String toString(){
        return "PsiFile(plain text):" + getName();
    }

    @Override
    
    public FileType getFileType() {
        return myFileType;
    }

    @Override
    
    public PsiReference[] getReferences() {
        return ReferenceProvidersRegistry.getReferencesFromProviders(this,PsiPlainTextFile.class);
    }
}
