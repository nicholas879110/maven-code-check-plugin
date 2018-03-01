/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
 * User: anna
 * Date: 17-Jun-2009
 */
package com.gome.maven.spellchecker;

import com.gome.maven.codeInsight.daemon.impl.HighlightInfoType;
import com.gome.maven.codeInsight.daemon.impl.SeveritiesProvider;
import com.gome.maven.icons.AllIcons;
import com.gome.maven.lang.annotation.HighlightSeverity;
import com.gome.maven.openapi.editor.colors.TextAttributesKey;
import com.gome.maven.openapi.editor.markup.TextAttributes;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;
import java.util.List;

public class SpellCheckerSeveritiesProvider extends SeveritiesProvider {
    private static final TextAttributesKey TYPO_KEY = TextAttributesKey.createTextAttributesKey("TYPO");
    public static final HighlightSeverity TYPO = new HighlightSeverity("TYPO", HighlightSeverity.INFORMATION.myVal + 5);

    @Override
    
    public List<HighlightInfoType> getSeveritiesHighlightInfoTypes() {
        class T extends HighlightInfoType.HighlightInfoTypeImpl implements HighlightInfoType.Iconable{
            public T( HighlightSeverity severity, TextAttributesKey attributesKey) {
                super(severity, attributesKey);
            }

            @Override
            public Icon getIcon() {
                return AllIcons.General.InspectionsTypos;
            }
        }
        return Collections.<HighlightInfoType>singletonList(new T(TYPO, TYPO_KEY));
    }

    @Override
    public Color getTrafficRendererColor( TextAttributes textAttributes) {
        return textAttributes.getErrorStripeColor();
    }

    @Override
    public boolean isGotoBySeverityEnabled(HighlightSeverity minSeverity) {
        return TYPO != minSeverity;
    }
}