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
package com.gome.maven.codeInsight.hint;

import com.gome.maven.icons.AllIcons;
import com.gome.maven.ide.IdeTooltipManager;
import com.gome.maven.openapi.util.Ref;
import com.gome.maven.ui.*;
import com.gome.maven.util.Consumer;
import com.gome.maven.util.ui.Html;
import com.gome.maven.util.ui.UIUtil;
//import org.gome.maven.lang.annotations.JdkConstants;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.event.HyperlinkListener;
import java.awt.*;
import java.awt.event.MouseListener;

public class HintUtil {
    public static final Color INFORMATION_COLOR = new JBColor(new Color(253, 254, 226), new Color(0x4d4f51));
    public static final Color QUESTION_COLOR = new JBColor(new Color(181, 208, 251), new Color(55, 108, 137));
    public static final Color ERROR_COLOR = new JBColor(new Color(255, 220, 220), new Color(0x781732));

    public static final Color QUESTION_UNDERSCORE_COLOR = JBColor.foreground();

    private HintUtil() {
    }

    public static JComponent createInformationLabel( String text) {
        return createInformationLabel(text, null, null, null);
    }

    public static JComponent createInformationLabel( String text,
                                                     HyperlinkListener hyperlinkListener,
                                                     MouseListener mouseListener,
                                                     Ref<Consumer<String>> updatedTextConsumer)
    {
        HintHint hintHint = getInformationHint();

        final HintLabel label = new HintLabel();
        label.setText(text, hintHint);
        label.setIcon(null);

        if (!hintHint.isAwtTooltip()) {
            label.setBorder(createHintBorder());
            label.setForeground(JBColor.foreground());
            label.setFont(getBoldFont());
            label.setBackground(INFORMATION_COLOR);
            label.setOpaque(true);
        }

        if (hyperlinkListener != null) {
            label.myPane.addHyperlinkListener(hyperlinkListener);
        }
        if (mouseListener != null) {
            label.myPane.addMouseListener(mouseListener);
        }
        if (updatedTextConsumer != null) {
            updatedTextConsumer.set(new Consumer<String>() {
                @Override
                public void consume(String s) {
                    label.myPane.setText(s);

                    // Force preferred size recalculation.
                    label.setPreferredSize(null);
                    label.myPane.setPreferredSize(null);
                }
            });
        }

        return label;
    }

    
    public static HintHint getInformationHint() {
        //noinspection UseJBColor
        return new HintHint().setTextBg(INFORMATION_COLOR)
                .setTextFg(UIUtil.isUnderDarcula() ? UIUtil.getLabelForeground() : Color.black)
                .setFont(getBoldFont())
                .setAwtTooltip(true);
    }

    public static CompoundBorder createHintBorder() {
        //noinspection UseJBColor
        return BorderFactory.createCompoundBorder(
                new ColoredSideBorder(Color.white, Color.white, Color.gray, Color.gray, 1),
                BorderFactory.createEmptyBorder(2, 2, 2, 2)
        );
    }

    
    public static JComponent createInformationLabel(SimpleColoredText text) {
        return createInformationLabel(text, null);
    }

    public static JComponent createQuestionLabel(String text) {
        HintHint hintHint = new HintHint().setTextBg(QUESTION_COLOR)
                .setTextFg(JBColor.foreground())
                .setFont(getBoldFont())
                .setAwtTooltip(true);

        HintLabel label = new HintLabel();
        label.setText(text, hintHint);
        label.setIcon(AllIcons.General.Help_small);

        if (!hintHint.isAwtTooltip()) {
            label.setBorder(createHintBorder());
            label.setForeground(JBColor.foreground());
            label.setFont(getBoldFont());
            label.setBackground(QUESTION_COLOR);
            label.setOpaque(true);
        }
        return label;
    }

    
    public static SimpleColoredComponent createInformationComponent() {
        SimpleColoredComponent component = new SimpleColoredComponent();
        component.setBackground(INFORMATION_COLOR);
        component.setForeground(JBColor.foreground());
        component.setFont(getBoldFont());
        return component;
    }

    
    public static JComponent createInformationLabel( SimpleColoredText text,  Icon icon) {
        SimpleColoredComponent component = createInformationComponent();
        component.setIcon(icon);
        text.appendToComponent(component);
        return new HintLabel(component);
    }

    public static JComponent createErrorLabel(String text) {
        HintHint hintHint = new HintHint().setTextBg(ERROR_COLOR)
                .setTextFg(JBColor.foreground())
                .setFont(getBoldFont())
                .setAwtTooltip(true);
        HintLabel label = new HintLabel();
        label.setText(text, hintHint);
        label.setIcon(null);

        if (!hintHint.isAwtTooltip()) {
            label.setBorder(createHintBorder()
            );
            label.setForeground(JBColor.foreground());
            label.setFont(getBoldFont());
            label.setBackground(ERROR_COLOR);
            label.setOpaque(true);
        }

        return label;
    }

    private static Font getBoldFont() {
        return UIUtil.getLabelFont().deriveFont(Font.BOLD);
    }

    public static JLabel createAdComponent(final String bottomText, final Border border, /*@JdkConstants.HorizontalAlignment*/ int alignment) {
        JLabel label = new JLabel();
        label.setText(bottomText);
        label.setHorizontalAlignment(alignment);
        label.setFont(label.getFont().deriveFont((float)(label.getFont().getSize() - 2)));
        if (bottomText != null) {
            label.setBorder(border);
        }
        return label;
    }

    
    public static String prepareHintText( String text,  HintHint hintHint) {
        return prepareHintText(new Html(text), hintHint);
    }

    public static String prepareHintText( Html text,  HintHint hintHint) {
        String htmlBody = UIUtil.getHtmlBody(text);
        return String.format(
                "<html><head>%s</head><body>%s</body></html>",
                UIUtil.getCssFontDeclaration(hintHint.getTextFont(), hintHint.getTextForeground(), hintHint.getLinkForeground(), hintHint.getUlImg()),
                htmlBody
        );
    }

    private static class HintLabel extends JPanel {
        private JEditorPane myPane;
        private SimpleColoredComponent myColored;
        private JLabel myIcon;

        private HintLabel() {
            setLayout(new BorderLayout());
        }

        private HintLabel( SimpleColoredComponent component) {
            this();
            setText(component);
        }

        public void setText( SimpleColoredComponent colored) {
            clearText();

            myColored = colored;
            add(myColored, BorderLayout.CENTER);

            setOpaque(true);
            setBackground(colored.getBackground());

            revalidate();
            repaint();
        }

        public void setText(String s, HintHint hintHint) {
            clearText();

            if (s != null) {
                myPane = IdeTooltipManager.initPane(s, hintHint, null);
                add(myPane, BorderLayout.CENTER);
            }

            setOpaque(true);
            setBackground(hintHint.getTextBackground());

            revalidate();
            repaint();
        }

        private void clearText() {
            if (myPane != null) {
                remove(myPane);
                myPane = null;
            }

            if (myColored != null) {
                remove(myColored);
                myColored = null;
            }
        }

        public void setIcon(Icon icon) {
            if (myIcon != null) {
                remove(myIcon);
            }

            myIcon = new JLabel(icon, SwingConstants.CENTER);
            myIcon.setVerticalAlignment(SwingConstants.TOP);

            add(myIcon, BorderLayout.WEST);

            revalidate();
            repaint();
        }

        @Override
        public String toString() {
            return "Hint: text='" + (myPane != null ? myPane.getText() : "") + "'";
        }
    }
}
