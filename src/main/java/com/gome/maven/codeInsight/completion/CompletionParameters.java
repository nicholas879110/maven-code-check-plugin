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
package com.gome.maven.codeInsight.completion;

import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiFile;

/**
 * @author peter
 */
public final class CompletionParameters {
    private final PsiElement myPosition;
    private final PsiFile myOriginalFile;
    private final CompletionType myCompletionType;
    private final Editor myEditor;
    private final int myOffset;
    private final int myInvocationCount;

    CompletionParameters( final PsiElement position,  final PsiFile originalFile,
                          CompletionType completionType, int offset, final int invocationCount,  Editor editor) {
        assert position.getTextRange().containsOffset(offset) : position;
        myPosition = position;
        assert position.isValid();
        myOriginalFile = originalFile;
        myCompletionType = completionType;
        myOffset = offset;
        myInvocationCount = invocationCount;
        myEditor = editor;
    }

    
    public CompletionParameters delegateToClassName() {
        return withType(CompletionType.CLASS_NAME).withInvocationCount(myInvocationCount - 1);
    }

    
    public CompletionParameters withType( CompletionType type) {
        return new CompletionParameters(myPosition, myOriginalFile, type, myOffset, myInvocationCount, myEditor);
    }

    
    public CompletionParameters withInvocationCount(int newCount) {
        return new CompletionParameters(myPosition, myOriginalFile, myCompletionType, myOffset, newCount, myEditor);
    }

    
    public PsiElement getPosition() {
        return myPosition;
    }

    
    public PsiElement getOriginalPosition() {
        return myOriginalFile.findElementAt(myPosition.getTextRange().getStartOffset());
    }

    
    public PsiFile getOriginalFile() {
        return myOriginalFile;
    }

    
    public CompletionType getCompletionType() {
        return myCompletionType;
    }

    public int getOffset() {
        return myOffset;
    }

    /**
     * @return
     * 0 for autopopup
     * 1 for explicitly invoked completion
     * >1 for next completion invocations when one lookup is already active
     */
    public int getInvocationCount() {
        return myInvocationCount;
    }

    public boolean isAutoPopup() {
        return myInvocationCount == 0;
    }

    
    public CompletionParameters withPosition( PsiElement element, int offset) {
        return new CompletionParameters(element, myOriginalFile, myCompletionType, offset, myInvocationCount, myEditor);
    }

    public boolean isExtendedCompletion() {
        return myCompletionType == CompletionType.BASIC && myInvocationCount >= 2;
    }

    
    public Editor getEditor() {
        return myEditor;
    }
}
