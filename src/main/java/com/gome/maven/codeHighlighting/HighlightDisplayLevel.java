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
package com.gome.maven.codeHighlighting;

import com.gome.maven.icons.AllIcons;
import com.gome.maven.lang.annotation.HighlightSeverity;
import com.gome.maven.openapi.editor.colors.CodeInsightColors;
import com.gome.maven.openapi.editor.colors.EditorColorsManager;
import com.gome.maven.openapi.editor.colors.TextAttributesKey;
import com.gome.maven.openapi.editor.markup.TextAttributes;
import com.gome.maven.openapi.util.Comparing;
import com.gome.maven.ui.JBColor;
import com.gome.maven.util.IconUtil;
import com.gome.maven.util.ObjectUtils;
import com.gome.maven.util.containers.HashMap;
import com.gome.maven.util.ui.ColorIcon;
import com.gome.maven.util.ui.JBUI;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

public class HighlightDisplayLevel {
    private static final Map<HighlightSeverity, HighlightDisplayLevel> ourMap = new HashMap<HighlightSeverity, HighlightDisplayLevel>();

    public static final HighlightDisplayLevel GENERIC_SERVER_ERROR_OR_WARNING = new HighlightDisplayLevel(HighlightSeverity.GENERIC_SERVER_ERROR_OR_WARNING,
            createIconByKey(CodeInsightColors.GENERIC_SERVER_ERROR_OR_WARNING));
    public static final HighlightDisplayLevel ERROR = new HighlightDisplayLevel(HighlightSeverity.ERROR, createErrorIcon());

    
    private static Icon createErrorIcon() {
        return new SingleColorIcon(CodeInsightColors.ERRORS_ATTRIBUTES) {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                IconUtil.colorize(AllIcons.General.InspectionsError, getColor()).paintIcon(c, g, x, y);
            }
        };
    }

    public static final HighlightDisplayLevel WARNING = new HighlightDisplayLevel(HighlightSeverity.WARNING, createIconByKey(CodeInsightColors.WARNINGS_ATTRIBUTES));
    private static final Icon DO_NOT_SHOW_KEY = createIconByKey(TextAttributesKey.createTextAttributesKey("DO_NOT_SHOW"));
    public static final HighlightDisplayLevel DO_NOT_SHOW = new HighlightDisplayLevel(HighlightSeverity.INFORMATION, DO_NOT_SHOW_KEY);
    /**
     * use #WEAK_WARNING instead
     */
    @Deprecated
    public static final HighlightDisplayLevel INFO = new HighlightDisplayLevel(HighlightSeverity.INFO, DO_NOT_SHOW.getIcon());
    public static final HighlightDisplayLevel WEAK_WARNING = new HighlightDisplayLevel(HighlightSeverity.WEAK_WARNING, createIconByKey(CodeInsightColors.WEAK_WARNING_ATTRIBUTES));

    public static final HighlightDisplayLevel NON_SWITCHABLE_ERROR = new HighlightDisplayLevel(HighlightSeverity.ERROR);

    private Icon myIcon;
    private final HighlightSeverity mySeverity;

    
    public static HighlightDisplayLevel find(String name) {
        for (Map.Entry<HighlightSeverity, HighlightDisplayLevel> entry : ourMap.entrySet()) {
            HighlightSeverity severity = entry.getKey();
            HighlightDisplayLevel displayLevel = entry.getValue();
            if (Comparing.strEqual(severity.getName(), name)) {
                return displayLevel;
            }
        }
        return null;
    }

    public static HighlightDisplayLevel find(HighlightSeverity severity) {
        return ourMap.get(severity);
    }

    public HighlightDisplayLevel( HighlightSeverity severity,  Icon icon) {
        this(severity);
        myIcon = icon;
        ourMap.put(mySeverity, this);
    }

    public HighlightDisplayLevel( HighlightSeverity severity) {
        mySeverity = severity;
    }


    public String toString() {
        return mySeverity.toString();
    }

    
    public String getName() {
        return mySeverity.getName();
    }

    public Icon getIcon() {
        return myIcon;
    }

    
    public HighlightSeverity getSeverity(){
        return mySeverity;
    }

    public static void registerSeverity( HighlightSeverity severity,  TextAttributesKey key,  Icon icon) {
        Icon severityIcon = icon != null ? icon : createIconByKey(key);
        final HighlightDisplayLevel level = ourMap.get(severity);
        if (level == null) {
            new HighlightDisplayLevel(severity, severityIcon);
        }
        else {
            level.myIcon = severityIcon;
        }
    }

    public static int getEmptyIconDim() {
        return JBUI.scale(14);
    }

    public static Icon createIconByKey( TextAttributesKey key) {
        return new SingleColorIcon(key);
    }

    
    public static Icon createIconByMask(final Color renderColor) {
        return new MyColorIcon(getEmptyIconDim(), renderColor);
    }

    private static class MyColorIcon extends ColorIcon implements ColoredIcon {
        public MyColorIcon(int size,  Color color) {
            super(size, color);
        }

        @Override
        public Color getColor() {
            return getIconColor();
        }
    }

    public interface ColoredIcon {
        Color getColor();
    }

    public static class SingleColorIcon implements Icon, ColoredIcon {
        private final TextAttributesKey myKey;

        public SingleColorIcon( TextAttributesKey key) {
            myKey = key;
        }

        
        public Color getColor() {
            return ObjectUtils.notNull(getColorInner(), JBColor.GRAY);
        }

        
        public Color getColorInner() {
            final EditorColorsManager manager = EditorColorsManager.getInstance();
            if (manager != null) {
                TextAttributes attributes = manager.getGlobalScheme().getAttributes(myKey);
                Color stripe = attributes.getErrorStripeColor();
                if (stripe != null) return stripe;
                return attributes.getEffectColor();
            }
            TextAttributes defaultAttributes = myKey.getDefaultAttributes();
            if (defaultAttributes == null) defaultAttributes = TextAttributes.ERASE_MARKER;
            return defaultAttributes.getErrorStripeColor();
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            g.setColor(getColor());
            g.fillRect(x + 2, y + 2, 10, 10);
        }

        @Override
        public int getIconWidth() {
            return getEmptyIconDim();
        }

        @Override
        public int getIconHeight() {
            return getEmptyIconDim();
        }
    }
}
