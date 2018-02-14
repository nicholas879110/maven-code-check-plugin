package com.gome.maven.ui;


import javax.swing.*;

public interface ColoredTextContainer {
    void append( String fragment,  SimpleTextAttributes attributes);

    void append( String fragment,  SimpleTextAttributes attributes, Object tag);

    void setIcon( Icon icon);

    void setToolTipText( String text);
}