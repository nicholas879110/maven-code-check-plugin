package com.gome.maven.ui;


import javax.swing.*;

/**
 * @author evgeny.zakrevsky
 */

public interface PanelWithAnchor {
    JComponent getAnchor();
    void setAnchor( JComponent anchor);
}
