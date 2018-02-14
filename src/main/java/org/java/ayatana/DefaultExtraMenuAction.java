//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package org.java.ayatana;

import java.awt.Window;
import javax.swing.FocusManager;
import javax.swing.JComponent;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;

public class DefaultExtraMenuAction implements ExtraMenuAction {
    protected String acceleratorText;

    public DefaultExtraMenuAction() {
    }

    public boolean allowDynamicMenuBar() {
        return System.getProperties().containsKey("jayatana.dynamicMenuBar") ? "true".equals(System.getProperty("jayatana.dynamicMenuBar")) : true;
    }

    public boolean allowMenuAction(Window window, JMenuBar menubar, JMenuItem menuitem, boolean selected, boolean shortcut) {
        if (shortcut) {
            KeyStroke accelerator = menuitem.getAccelerator();
            if (accelerator != null) {
                this.acceleratorText = accelerator.toString();
                if (FocusManager.getCurrentManager().getFocusOwner() instanceof JComponent) {
                    JComponent jcomp = (JComponent)FocusManager.getCurrentManager().getFocusOwner();
                    if (jcomp.getActionForKeyStroke(accelerator) != null || ApplicationMenu.getWindowRootPane(window).getActionForKeyStroke(accelerator) != null) {
                        return false;
                    }
                }
            } else {
                this.acceleratorText = null;
            }
        }

        return true;
    }

    public void beforInvokeMenu(Window window, JMenuBar menubar, JMenuItem menuitem, boolean selected, boolean shortcut) {
    }

    public void invokeMenu(Window window, JMenuBar menubar, JMenuItem menuitem, boolean selected, boolean shortcut) {
    }

    public void afterInvokeMenu(Window window, JMenuBar menubar, JMenuItem menuitem, boolean selected, boolean shortcut) {
    }
}
