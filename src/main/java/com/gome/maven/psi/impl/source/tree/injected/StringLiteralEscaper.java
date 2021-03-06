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
package com.gome.maven.psi.impl.source.tree.injected;

import com.gome.maven.openapi.util.TextRange;
import com.gome.maven.psi.LiteralTextEscaper;
import com.gome.maven.psi.PsiLanguageInjectionHost;
import com.gome.maven.psi.impl.source.tree.java.PsiLiteralExpressionImpl;

/**
 * @author cdr
 */
public class StringLiteralEscaper<T extends PsiLanguageInjectionHost> extends LiteralTextEscaper<T> {
    private int[] outSourceOffsets;

    public StringLiteralEscaper(T host) {
        super(host);
    }

    @Override
    public boolean decode( final TextRange rangeInsideHost,  StringBuilder outChars) {
        String subText = rangeInsideHost.substring(myHost.getText());
        outSourceOffsets = new int[subText.length()+1];
        return PsiLiteralExpressionImpl.parseStringCharacters(subText, outChars, outSourceOffsets);
    }

    @Override
    public int getOffsetInHost(int offsetInDecoded,  final TextRange rangeInsideHost) {
        int result = offsetInDecoded < outSourceOffsets.length ? outSourceOffsets[offsetInDecoded] : -1;
        if (result == -1) return -1;
        return (result <= rangeInsideHost.getLength() ? result : rangeInsideHost.getLength()) + rangeInsideHost.getStartOffset();
    }

    @Override
    public boolean isOneLine() {
        return true;
    }
}
