/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.gome.maven.ui.components;

import com.gome.maven.util.ui.UIUtil;
//import org.intellij.lang.annotations.JdkConstants;

import javax.swing.*;
import java.awt.*;

/**
 * @author evgeny.zakrevsky
 */
public class JBLabelDecorator extends JBLabel {
    private JBLabelDecorator() {
        super();
    }

    private JBLabelDecorator( Icon image) {
        super(image);
    }

    private JBLabelDecorator( String text) {
        super(text);
    }

    private JBLabelDecorator( String text, /*@JdkConstants.HorizontalAlignment */int horizontalAlignment) {
        super(text, horizontalAlignment);
    }

    private JBLabelDecorator( Icon image, /*@JdkConstants.HorizontalAlignment*/ int horizontalAlignment) {
        super(image, horizontalAlignment);
    }

    private JBLabelDecorator( String text,  Icon icon, /*@JdkConstants.HorizontalAlignment*/ int horizontalAlignment) {
        super(text, icon, horizontalAlignment);
    }

    public static JBLabelDecorator createJBLabelDecorator() {
        return new JBLabelDecorator();
    }

    public static JBLabelDecorator createJBLabelDecorator(String text) {
        return new JBLabelDecorator(text);
    }

    public JBLabelDecorator setBold(boolean isBold) {
        if (isBold) {
            setFont(getFont().deriveFont(Font.BOLD));
        } else {
            setFont(getFont().deriveFont(Font.PLAIN));
        }
        return this;
    }

    public JBLabelDecorator setComponentStyleDecorative( UIUtil.ComponentStyle componentStyle) {
        super.setComponentStyle(componentStyle);
        return this;
    }

    public JBLabelDecorator setFontColorDecorative( UIUtil.FontColor fontColor) {
        super.setFontColor(fontColor);
        return this;
    }
}
