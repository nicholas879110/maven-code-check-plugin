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
package com.gome.maven.util.ui;


import java.awt.*;

/**
 * @author Konstantin Bulenkov
 */
public class ColorIcon extends EmptyIcon {
    private final Color myColor;
    private boolean myBorder;
    private int myColorSize;


    public ColorIcon(int size, int colorSize,  Color color, final boolean border) {
        super(size, size);
        myColor = color;
        myColorSize = colorSize;
        myBorder = border;
    }

    public ColorIcon(int size,  Color color, final boolean border) {
        this(size, size, color, border);
    }

    public ColorIcon(int size,  Color color) {
        this(size, color, false);
    }

    public Color getIconColor() {
        return myColor;
    }

    @Override
    public void paintIcon(final Component component, final Graphics g, final int i, final int j) {
        final int iconWidth = getIconWidth();
        final int iconHeight = getIconHeight();
        g.setColor(getIconColor());

        final int x = i + (iconWidth - myColorSize) / 2;
        final int y = j + (iconHeight - myColorSize) / 2;

        g.fillRect(x, y, myColorSize, myColorSize);

        if (myBorder) {
            g.setColor(Color.BLACK);
            g.drawRect(x, y, myColorSize, myColorSize);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        ColorIcon icon = (ColorIcon)o;

        if (myBorder != icon.myBorder) return false;
        if (myColorSize != icon.myColorSize) return false;
        if (myColor != null ? !myColor.equals(icon.myColor) : icon.myColor != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (myColor != null ? myColor.hashCode() : 0);
        result = 31 * result + (myBorder ? 1 : 0);
        result = 31 * result + myColorSize;
        return result;
    }
}
