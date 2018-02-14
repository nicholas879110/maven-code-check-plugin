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
package com.gome.maven.codeInsight.lookup;

import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.ui.JBColor;
import com.gome.maven.util.Function;
import com.gome.maven.util.SmartList;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;
import java.util.List;

/**
 * @author peter
 */
public class LookupElementPresentation {
    private Icon myIcon;
    private Icon myTypeIcon;
    private String myItemText;
    private String myTypeText;
    private boolean myStrikeout;
    private Color myItemTextForeground = JBColor.foreground();
    private boolean myItemTextBold;
    private boolean myItemTextUnderlined;
    private boolean myTypeGrayed;
     private List<TextFragment> myTail;

    public void setIcon( Icon icon) {
        myIcon = icon;
    }

    public void setItemText( String text) {
        myItemText = text;
    }

    public void setStrikeout(boolean strikeout) {
        myStrikeout = strikeout;
    }

    public void setItemTextBold(boolean bold) {
        myItemTextBold = bold;
    }

    public void setTailText( String text) {
        setTailText(text, false);
    }

    public void clearTail() {
        myTail = null;
    }

    public void appendTailText( String text, boolean grayed) {
        appendTailText(new TextFragment(text, grayed, null));
    }

    private void appendTailText( TextFragment fragment) {
        if (myTail == null) {
            myTail = new SmartList<TextFragment>();
        }
        myTail.add(fragment);
    }

    public void setTailText( String text, boolean grayed) {
        clearTail();
        if (text != null) {
            appendTailText(new TextFragment(text, grayed, null));
        }
    }

    public void setTailText( String text,  Color foreground) {
        clearTail();
        if (text != null) {
            appendTailText(new TextFragment(text, false, foreground));
        }
    }

    public void setTypeText( String text) {
        setTypeText(text, null);
    }

    public void setTypeText( String text,  Icon icon) {
        myTypeText = text;
        myTypeIcon = icon;
    }

    /**
     * Is equivalent to instanceof {@link com.gome.maven.codeInsight.lookup.RealLookupElementPresentation} check.
     *
     * @return whether the presentation is requested to actually render lookup element on screen, or just to estimate its width.
     * In the second, 'non-real' case, some heavy operations (e.g. getIcon()) can be omitted (only icon width is important)
     */
    public boolean isReal() {
        return false;
    }

    
    public Icon getIcon() {
        return myIcon;
    }

    
    public Icon getTypeIcon() {
        return myTypeIcon;
    }

    
    public String getItemText() {
        return myItemText;
    }

    
    public List<TextFragment> getTailFragments() {
        return myTail == null ? Collections.<TextFragment>emptyList() : Collections.unmodifiableList(myTail);
    }

    
    public String getTailText() {
        if (myTail == null) return null;
        return StringUtil.join(myTail, new Function<TextFragment, String>() {
            @Override
            public String fun(TextFragment fragment) {
                return fragment.text;
            }
        }, "");
    }

    
    public String getTypeText() {
        return myTypeText;
    }

    public boolean isStrikeout() {
        return myStrikeout;
    }

    @Deprecated
    public boolean isTailGrayed() {
        return myTail != null && myTail.get(0).grayed;
    }

    
    @Deprecated
    public Color getTailForeground() {
        return myTail != null ? myTail.get(0).fgColor : null;
    }

    public boolean isItemTextBold() {
        return myItemTextBold;
    }

    public boolean isItemTextUnderlined() {
        return myItemTextUnderlined;
    }

    public void setItemTextUnderlined(boolean itemTextUnderlined) {
        myItemTextUnderlined = itemTextUnderlined;
    }

     public Color getItemTextForeground() {
        return myItemTextForeground;
    }

    public void setItemTextForeground( Color itemTextForeground) {
        myItemTextForeground = itemTextForeground;
    }

    public void copyFrom( LookupElementPresentation presentation) {
        myIcon = presentation.myIcon;
        myTypeIcon = presentation.myTypeIcon;
        myItemText = presentation.myItemText;

        List<TextFragment> thatTail = presentation.myTail;
        myTail = thatTail == null ? null : new SmartList<TextFragment>(thatTail);

        myTypeText = presentation.myTypeText;
        myStrikeout = presentation.myStrikeout;
        myItemTextBold = presentation.myItemTextBold;
        myTypeGrayed = presentation.myTypeGrayed;
        myItemTextUnderlined = presentation.myItemTextUnderlined;
        myItemTextForeground = presentation.myItemTextForeground;
    }

    public boolean isTypeGrayed() {
        return myTypeGrayed;
    }

    public void setTypeGrayed(boolean typeGrayed) {
        myTypeGrayed = typeGrayed;
    }

    public static LookupElementPresentation renderElement(LookupElement element) {
        LookupElementPresentation presentation = new LookupElementPresentation();
        element.renderElement(presentation);
        return presentation;
    }

    @Override
    public String toString() {
        return "LookupElementPresentation{" +
                ", itemText='" + myItemText + '\'' +
                ", tail=" + myTail +
                ", typeText='" + myTypeText + '\'' +
                '}';
    }

    public static class TextFragment {
        public final String text;
        private final boolean grayed;
         private final Color fgColor;

        public TextFragment(String text, boolean grayed,  Color fgColor) {
            this.text = text;
            this.grayed = grayed;
            this.fgColor = fgColor;
        }

        @Override
        public String toString() {
            return "TextFragment{" +
                    "text='" + text + '\'' +
                    ", grayed=" + grayed +
                    ", fgColor=" + fgColor +
                    '}';
        }

        public boolean isGrayed() {
            return grayed;
        }

        
        public Color getForegroundColor() {
            return fgColor;
        }
    }
}
