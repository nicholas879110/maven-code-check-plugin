package com.gome.maven.ui;


import javax.swing.*;

/**
 * @author evgeny.zakrevsky
 */

public interface AnchorableComponent {
     JComponent getAnchor();
    void setAnchor( JComponent anchor);
}
