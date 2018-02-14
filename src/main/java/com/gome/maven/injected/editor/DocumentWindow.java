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
 * @author max
 */
package com.gome.maven.injected.editor;

import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.util.Segment;
import com.gome.maven.openapi.util.TextRange;

public interface DocumentWindow extends Document {
     Document getDelegate();
    int injectedToHost(int injectedOffset);
     TextRange injectedToHost( TextRange injectedOffset);
    int hostToInjected(int hostOffset);

    /**
     * Use com.gome.maven.lang.injection.InjectedLanguageManager#intersectWithAllEditableFragments(com.gome.maven.psi.PsiFile, com.gome.maven.openapi.util.TextRange)
     * since editable fragments may well spread over several injection hosts
     */
    @Deprecated
    
    TextRange intersectWithEditable( TextRange range);

    
    TextRange getHostRange(int hostOffset);

    int injectedToHostLine(int line);

    
    Segment[] getHostRanges();

    boolean areRangesEqual( DocumentWindow documentWindow);

    boolean isValid();

    boolean containsRange(int start, int end);
}
