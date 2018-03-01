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

/*
 * Created by IntelliJ IDEA.
 * User: max
 * Date: Feb 7, 2002
 * Time: 1:11:08 PM
 * To change template for new class use
 * Code Style | Class Templates options (Tools | IDE Options).
 */
package com.gome.maven.codeInspection.dataFlow.instructions;

import com.gome.maven.codeInspection.dataFlow.*;
import com.gome.maven.codeInspection.dataFlow.value.DfaValue;
import com.gome.maven.codeInspection.dataFlow.value.DfaValueFactory;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.psi.*;
import com.gome.maven.psi.search.GlobalSearchScope;
import com.gome.maven.psi.tree.IElementType;
import com.gome.maven.psi.tree.TokenSet;

import static com.gome.maven.psi.JavaTokenType.*;

public class BinopInstruction extends BranchingInstruction {
    private static final TokenSet ourSignificantOperations = TokenSet.create(EQEQ, NE, LT, GT, LE, GE, INSTANCEOF_KEYWORD, PLUS);
    private final IElementType myOperationSign;
    private final Project myProject;

    public BinopInstruction(IElementType opSign,  PsiElement psiAnchor,  Project project) {
        super(psiAnchor);
        myProject = project;
        myOperationSign = ourSignificantOperations.contains(opSign) ? opSign : null;
    }

    @Override
    public DfaInstructionState[] accept(DataFlowRunner runner, DfaMemoryState stateBefore, InstructionVisitor visitor) {
        return visitor.visitBinop(this, runner, stateBefore);
    }

    public DfaValue getNonNullStringValue(final DfaValueFactory factory) {
        PsiElement anchor = getPsiAnchor();
        Project project = myProject;
        PsiClassType string = PsiType.getJavaLangString(PsiManager.getInstance(project), anchor == null ? GlobalSearchScope.allScope(project) : anchor.getResolveScope());
        return factory.createTypeValue(string, Nullness.NOT_NULL);
    }

    public String toString() {
        return "BINOP " + myOperationSign;
    }

    public IElementType getOperationSign() {
        return myOperationSign;
    }
}
