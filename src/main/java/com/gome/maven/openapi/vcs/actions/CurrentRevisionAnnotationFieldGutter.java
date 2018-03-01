/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package com.gome.maven.openapi.vcs.actions;

import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.openapi.editor.colors.ColorKey;
import com.gome.maven.openapi.util.Couple;
import com.gome.maven.openapi.vcs.VcsBundle;
import com.gome.maven.openapi.vcs.annotate.AnnotationSource;
import com.gome.maven.openapi.vcs.annotate.FileAnnotation;
import com.gome.maven.openapi.vcs.annotate.LineAnnotationAspect;
import com.gome.maven.openapi.vcs.annotate.TextAnnotationPresentation;
import com.gome.maven.openapi.vcs.history.VcsRevisionNumber;
import com.gome.maven.util.Consumer;

import java.awt.*;
import java.util.Map;

/**
 * shown additionally only when merge
 *
 * @author Konstantin Bulenkov
 */
class CurrentRevisionAnnotationFieldGutter extends AnnotationFieldGutter implements Consumer<AnnotationSource> {
    // merge source showing is turned on
    private boolean myTurnedOn;

    CurrentRevisionAnnotationFieldGutter(FileAnnotation annotation,
                                         Editor editor,
                                         LineAnnotationAspect aspect,
                                         TextAnnotationPresentation highlighting,
                                         Couple<Map<VcsRevisionNumber, Color>> colorScheme) {
        super(annotation, editor, aspect, highlighting, colorScheme);
    }

    @Override
    public ColorKey getColor(int line, Editor editor) {
        return AnnotationSource.LOCAL.getColor();
    }

    @Override
    public String getLineText(int line, Editor editor) {
        final String value = myAspect.getValue(line);
        if (String.valueOf(myAnnotation.getLineRevisionNumber(line)).equals(value)) {
            return "";
        }
        // shown in merge sources mode
        return myTurnedOn ? value : "";
    }

    @Override
    public String getToolTip(int line, Editor editor) {
        final String aspectTooltip = myAspect.getTooltipText(line);
        if (aspectTooltip != null) {
            return aspectTooltip;
        }
        final String text = getLineText(line, editor);
        return ((text == null) || (text.length() == 0)) ? "" : VcsBundle.message("annotation.original.revision.text", text);
    }

    public void consume(final AnnotationSource annotationSource) {
        myTurnedOn = annotationSource.showMerged();
    }
}
