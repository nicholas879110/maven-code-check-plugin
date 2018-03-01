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

package com.gome.maven.psi.impl.java.stubs.impl;

import com.gome.maven.psi.PsiClass;
import com.gome.maven.psi.PsiJavaFile;
import com.gome.maven.psi.impl.java.stubs.*;
import com.gome.maven.psi.stubs.PsiFileStubImpl;
import com.gome.maven.psi.tree.IStubFileElementType;
import com.gome.maven.util.io.StringRef;

/*
 * @author max
 */
public class PsiJavaFileStubImpl extends PsiFileStubImpl<PsiJavaFile> implements PsiJavaFileStub {
    private StringRef myPackageName;
    private final boolean myCompiled;
    private StubPsiFactory myFactory;

    public PsiJavaFileStubImpl(final PsiJavaFile file, final StringRef packageName, final boolean compiled) {
        super(file);
        myPackageName = packageName;
        myCompiled = compiled;
        myFactory = compiled ? new ClsStubPsiFactory() : new SourceStubPsiFactory();
    }

    public PsiJavaFileStubImpl(final String packageName, final boolean compiled) {
        this(null, StringRef.fromString(packageName), compiled);
    }

    @Override
    public String getPackageName() {
        return StringRef.toString(myPackageName);
    }

    @Override
    public boolean isCompiled() {
        return myCompiled;
    }

    @Override
    public StubPsiFactory getPsiFactory() {
        return myFactory;
    }

    @Override
    public void setPsiFactory(StubPsiFactory factory) {
        myFactory = factory;
    }

    public void setPackageName(final String packageName) {
        myPackageName = StringRef.fromString(packageName);
    }

    @Override
    public IStubFileElementType getType() {
        return JavaStubElementTypes.JAVA_FILE;
    }

    @SuppressWarnings({"HardCodedStringLiteral"})
    public String toString() {
        return "PsiJavaFileStub [" + myPackageName + "]";
    }

    @Override
    public PsiClass[] getClasses() {
        return getChildrenByType(JavaStubElementTypes.CLASS, PsiClass.ARRAY_FACTORY);
    }
}
