/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.gome.maven.codeHighlighting;

import com.gome.maven.openapi.progress.ProgressIndicator;

/**
 * Pass performs analysis in background and highlights found issues in the editor.
 */
public interface HighlightingPass {
    HighlightingPass[] EMPTY_ARRAY = new HighlightingPass[0];

    /**
     * Asks this pass to start analysis and hold collected information.
     * This method is called from a background thread.
     *
     * @param progress to check if highlighting process is cancelled. Pass is to check progress.isCanceled() as often as possible and
     *                 throw {@link com.gome.maven.openapi.progress.ProcessCanceledException} if <code>true</code> is returned.
     *                 See also {@link com.gome.maven.openapi.progress.ProgressIndicator#checkCanceled()}.
     */
    void collectInformation( ProgressIndicator progress);

    /**
     * Called to apply information collected by {@linkplain #collectInformation(com.gome.maven.openapi.progress.ProgressIndicator)} to the editor.
     * This method is called from the event dispatch thread.
     */
    void applyInformationToEditor();
}
