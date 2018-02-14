//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package org.java.ayatana;

import java.awt.Window;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

public interface ExtraMenuAction {
    boolean allowDynamicMenuBar();

    boolean allowMenuAction(Window var1, JMenuBar var2, JMenuItem var3, boolean var4, boolean var5);

    void beforInvokeMenu(Window var1, JMenuBar var2, JMenuItem var3, boolean var4, boolean var5);

    void invokeMenu(Window var1, JMenuBar var2, JMenuItem var3, boolean var4, boolean var5);

    void afterInvokeMenu(Window var1, JMenuBar var2, JMenuItem var3, boolean var4, boolean var5);
}
