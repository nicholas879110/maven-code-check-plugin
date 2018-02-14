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

package com.gome.maven.codeInsight.folding;

import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.openapi.editor.FoldRegion;
import com.gome.maven.openapi.fileEditor.impl.text.CodeFoldingState;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.WriteExternalException;
import org.jdom.Element;

public abstract class CodeFoldingManager {
    public static CodeFoldingManager getInstance(Project project){
        return project.getComponent(CodeFoldingManager.class);
    }

    public abstract void updateFoldRegions( Editor editor);

    public abstract void forceDefaultState( Editor editor);

    
    public abstract Runnable updateFoldRegionsAsync( Editor editor, boolean firstTime);

    
    public abstract FoldRegion findFoldRegion( Editor editor, int startOffset, int endOffset);
    public abstract FoldRegion[] getFoldRegionsAtOffset( Editor editor, int offset);

    public abstract CodeFoldingState saveFoldingState( Editor editor);
    public abstract void restoreFoldingState( Editor editor,  CodeFoldingState state);

    public abstract void writeFoldingState( CodeFoldingState state,  Element element) throws WriteExternalException;
    public abstract CodeFoldingState readFoldingState( Element element,  Document document);

    public abstract void releaseFoldings( Editor editor);
    public abstract void buildInitialFoldings( Editor editor);
    
    public abstract CodeFoldingState buildInitialFoldings( Document document);
}
