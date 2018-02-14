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
package com.gome.maven.injected.editor;

import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.openapi.editor.FoldRegion;
import com.gome.maven.openapi.editor.FoldingGroup;
import com.gome.maven.openapi.editor.ex.RangeMarkerEx;

/**
 * User: cdr
 */
class FoldingRegionWindow extends RangeMarkerWindow implements FoldRegion {
    private final EditorWindow myEditorWindow;

    private final FoldRegion myHostRegion;

    FoldingRegionWindow( DocumentWindow documentWindow,
                         EditorWindow editorWindow,
                         FoldRegion hostRegion,
                        int startShift,
                        int endShift)
    {
        super(documentWindow, (RangeMarkerEx)hostRegion, startShift, endShift);
        myEditorWindow = editorWindow;
        myHostRegion = hostRegion;
    }

    @Override
    public boolean isExpanded() {
        return myHostRegion.isExpanded();
    }

    @Override
    public void setExpanded(boolean expanded) {
        myHostRegion.setExpanded(expanded);
    }

    @Override
    
    public String getPlaceholderText() {
        return myHostRegion.getPlaceholderText();
    }

    @Override
    public Editor getEditor() {
        return myEditorWindow;
    }

    @Override
    public FoldingGroup getGroup() {
        return myHostRegion.getGroup();
    }

    @Override
    public boolean shouldNeverExpand() {
        return false;
    }

    @Override
    public RangeMarkerEx getDelegate() {
        return (RangeMarkerEx)myHostRegion;
    }
}
