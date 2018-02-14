//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package org.java.ayatana;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.AWTEventListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JRootPane;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

public final class ApplicationMenu implements WindowListener, AWTEventListener, ContainerListener, ComponentListener, PropertyChangeListener {
    private static final List<Window> windows = new ArrayList();
    private static boolean initialized = false;
    private Window window;
    private JMenuBar menubar;
    private boolean tryInstalled = false;
    private ExtraMenuAction extraMenuAction;
    private long windowxid = -1L;
    private boolean allowDynamicMenuBar;
    private long approveRebuild = -1L;

    public static JMenuBar getWindowMenuBar(Window window) {
        if (window instanceof JFrame) {
            return ((JFrame)window).getJMenuBar();
        } else {
            return window instanceof JDialog ? ((JDialog)window).getJMenuBar() : null;
        }
    }

    public static String getWindowTitle(Window window) {
        if (window instanceof JFrame) {
            return ((JFrame)window).getTitle();
        } else {
            return window instanceof JDialog ? ((JDialog)window).getTitle() : null;
        }
    }

    public static JRootPane getWindowRootPane(Window window) {
        if (window instanceof JFrame) {
            return ((JFrame)window).getRootPane();
        } else {
            return window instanceof JDialog ? ((JDialog)window).getRootPane() : null;
        }
    }

    public static boolean tryInstall(Window window) {
        return tryInstall(window, getWindowMenuBar(window), new DefaultExtraMenuAction());
    }

    public static boolean tryInstall(Window window, JMenuBar menubar) {
        return tryInstall(window, getWindowMenuBar(window), new DefaultExtraMenuAction());
    }

    public static boolean tryInstall(Window window, ExtraMenuAction additionalMenuAction) {
        return tryInstall(window, getWindowMenuBar(window), additionalMenuAction);
    }

    public static boolean tryInstall(Window window, JMenuBar menubar, ExtraMenuAction additionalMenuAction) {
        if (window != null && additionalMenuAction != null) {
            if (menubar == null) {
                return false;
            } else if (windows.contains(window)) {
                return false;
            } else {
                String menuProxy = System.getenv("UBUNTU_MENUPROXY");
                if (!"libappmenu.so".equals(menuProxy) && !"1".equals(menuProxy)) {
                    return false;
                } else if (AyatanaLibrary.load()) {
                    new ApplicationMenu(window, menubar, additionalMenuAction);
                    return true;
                } else {
                    return false;
                }
            }
        } else {
            throw new NullPointerException();
        }
    }

    private static native void nativeInitialize();

    private static native void nativeUninitialize();

    private static synchronized void initialize() {
        if (!initialized) {
            GMainLoop.run();
            nativeInitialize();
            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            ApplicationMenu.nativeUninitialize();
                        }
                    });
                }
            });
            initialized = true;
        }

    }

    private native void setCurrent(long var1);

    private native long getWindowXID(Window var1);

    private native void registerWatcher(long var1);

    private native void unregisterWatcher(long var1);

    private void addMenu(JMenu menu) {
        if (menu.getText() != null && !"".equals(menu.getText())) {
            this.addMenu(menu.hashCode(), menu.getText(), menu.isEnabled());
        }
    }

    private native void addMenu(int var1, String var2, boolean var3);

    private synchronized void removeAllMenus() {
        this.removeAll();
    }

    private native void removeAll();

    private void addMenuItem(JMenuItem menuitem) {
        if (menuitem.getText() != null && !"".equals(menuitem.getText())) {
            int modifiers = -1;
            int keycode = -1;
            if (menuitem.getAccelerator() != null) {
                modifiers = menuitem.getAccelerator().getModifiers();
                keycode = menuitem.getAccelerator().getKeyCode();
            }

            if (menuitem instanceof JMenu) {
                this.addMenu((JMenu)menuitem);
            } else if (menuitem instanceof JRadioButtonMenuItem) {
                this.addMenuItemRadio(menuitem.hashCode(), menuitem.getText(), menuitem.isEnabled(), modifiers, keycode, menuitem.isSelected());
            } else if (menuitem instanceof JCheckBoxMenuItem) {
                this.addMenuItemCheck(menuitem.hashCode(), menuitem.getText(), menuitem.isEnabled(), modifiers, keycode, menuitem.isSelected());
            } else {
                this.addMenuItem(menuitem.hashCode(), menuitem.getText(), menuitem.isEnabled(), modifiers, keycode);
            }

        }
    }

    private native void addMenuItem(int var1, String var2, boolean var3, int var4, int var5);

    private native void addMenuItemRadio(int var1, String var2, boolean var3, int var4, int var5, boolean var6);

    private native void addMenuItemCheck(int var1, String var2, boolean var3, int var4, int var5, boolean var6);

    private void addSeparator() {
        this.addMenuItemSeparator();
    }

    private native void addMenuItemSeparator();

    private ApplicationMenu(Window window, JMenuBar menubar, ExtraMenuAction additionalMenuAction) {
        windows.add(window);
        this.window = window;
        this.menubar = menubar;
        this.extraMenuAction = additionalMenuAction;
        this.allowDynamicMenuBar = this.extraMenuAction.allowDynamicMenuBar();
        window.addWindowListener(this);
        if (window.isDisplayable()) {
            this.tryInstall();
        }

    }

    private synchronized void tryInstall() {
        if (this.tryInstalled && this.windowxid > -1L) {
            this.setCurrent(this.windowxid);
        } else if (!this.tryInstalled) {
            EventQueue.invokeLater(new Runnable() {
                public void run() {
                    ApplicationMenu.initialize();
                    ApplicationMenu.this.windowxid = ApplicationMenu.this.getWindowXID(ApplicationMenu.this.window);
                    ApplicationMenu.this.registerWatcher(ApplicationMenu.this.windowxid);
                }
            });
            this.tryInstalled = true;
        }

    }

    private synchronized void tryUninstall() {
        if (this.tryInstalled) {
            EventQueue.invokeLater(new Runnable() {
                public void run() {
                    ApplicationMenu.this.unregisterWatcher(ApplicationMenu.this.windowxid);
                    ApplicationMenu.this.window.removeWindowListener(ApplicationMenu.this);
                    ApplicationMenu.windows.remove(ApplicationMenu.this.window);
                }
            });
            this.tryInstalled = false;
        }

    }

    private synchronized void buildMenuBar() {
        this.buildMenuBar(false);
    }

    private synchronized void buildMenuBar(boolean first) {
        Component[] arr$ = this.menubar.getComponents();
        int len$ = arr$.length;

        for(int i$ = 0; i$ < len$; ++i$) {
            Component comp = arr$[i$];
            if (comp instanceof JMenu) {
                if (comp.isVisible()) {
                    this.addMenu((JMenu)comp);
                }

                if (first && this.allowDynamicMenuBar) {
                    ((JMenu)comp).addComponentListener(this);
                    ((JMenu)comp).addPropertyChangeListener(this);
                }
            }
        }

    }

    private synchronized void install() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                Toolkit.getDefaultToolkit().addAWTEventListener(ApplicationMenu.this, 8L);
                ApplicationMenu.this.buildMenuBar(true);
                if (ApplicationMenu.this.allowDynamicMenuBar) {
                    ApplicationMenu.this.menubar.addContainerListener(ApplicationMenu.this);
                }

                ApplicationMenu.this.menubar.setVisible(false);
            }
        });
    }

    private synchronized void uninstall() {
        if (this.allowDynamicMenuBar) {
            Component[] arr$ = this.menubar.getComponents();
            int len$ = arr$.length;

            for(int i$ = 0; i$ < len$; ++i$) {
                Component comp = arr$[i$];
                if (comp instanceof JMenu) {
                    ((JMenu)comp).removeComponentListener(this);
                    ((JMenu)comp).removePropertyChangeListener(this);
                }
            }

            this.menubar.removeContainerListener(this);
        }

        Toolkit.getDefaultToolkit().removeAWTEventListener(this);
        this.menubar.setVisible(true);
    }

    private JMenuItem getJMenuItem(int hashcode) {
        Component[] arr$ = this.menubar.getComponents();
        int len$ = arr$.length;

        for(int i$ = 0; i$ < len$; ++i$) {
            Component comp = arr$[i$];
            JMenuItem item;
            if (comp instanceof JMenuItem && (item = this.getJMenuItem((JMenuItem)comp, hashcode)) != null) {
                return item;
            }
        }

        return null;
    }

    private JMenuItem getJMenuItem(JMenuItem menu, int hashcode) {
        if (menu.hashCode() == hashcode) {
            return menu;
        } else {
            if (menu instanceof JMenu) {
                Component[] arr$ = ((JMenu)menu).getMenuComponents();
                int len$ = arr$.length;

                for(int i$ = 0; i$ < len$; ++i$) {
                    Component comp = arr$[i$];
                    JMenuItem item;
                    if (comp instanceof JMenuItem && (item = this.getJMenuItem((JMenuItem)comp, hashcode)) != null) {
                        return item;
                    }
                }
            }

            return null;
        }
    }

    private JMenuItem getJMenuItem(int keycode, int modifiers) {
        Component[] arr$ = this.menubar.getComponents();
        int len$ = arr$.length;

        for(int i$ = 0; i$ < len$; ++i$) {
            Component comp = arr$[i$];
            JMenuItem item;
            if (comp instanceof JMenuItem && (item = this.getJMenuItem((JMenuItem)comp, keycode, modifiers)) != null) {
                return item;
            }
        }

        return null;
    }

    private JMenuItem getJMenuItem(JMenuItem menu, int keycode, int modifiers) {
        if (menu instanceof JMenu) {
            Component[] arr$ = ((JMenu)menu).getMenuComponents();
            int len$ = arr$.length;

            for(int i$ = 0; i$ < len$; ++i$) {
                Component comp = arr$[i$];
                JMenuItem item;
                if (comp instanceof JMenuItem && (item = this.getJMenuItem((JMenuItem)comp, keycode, modifiers)) != null) {
                    return item;
                }
            }
        } else {
            if (menu.getAccelerator() == null) {
                return null;
            }

            if (menu.getAccelerator().getKeyCode() == keycode && menu.getAccelerator().getModifiers() == modifiers) {
                return menu;
            }
        }

        return null;
    }

    private void itemActivated(final int hashcode) {
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    ApplicationMenu.this.invokeMenuItem(ApplicationMenu.this.getJMenuItem(hashcode), false);
                }
            });
        } catch (InterruptedException var3) {
            Logger.getLogger(ApplicationMenu.class.getName()).log(Level.SEVERE, (String)null, var3);
        } catch (InvocationTargetException var4) {
            Logger.getLogger(ApplicationMenu.class.getName()).log(Level.SEVERE, (String)null, var4);
        }

    }

    private void itemAboutToShow(final int hashcode) {
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    ApplicationMenu.this.invokeSelectMenu((JMenu)ApplicationMenu.this.getJMenuItem(hashcode));
                }
            });
        } catch (InterruptedException var3) {
            Logger.getLogger(ApplicationMenu.class.getName()).log(Level.SEVERE, (String)null, var3);
        } catch (InvocationTargetException var4) {
            Logger.getLogger(ApplicationMenu.class.getName()).log(Level.SEVERE, (String)null, var4);
        }

    }

    private void itemAfterShow(final int hashcode) {
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    ApplicationMenu.this.invokeDeselectMenu((JMenu)ApplicationMenu.this.getJMenuItem(hashcode));
                }
            });
        } catch (InterruptedException var3) {
            Logger.getLogger(ApplicationMenu.class.getName()).log(Level.SEVERE, (String)null, var3);
        } catch (InvocationTargetException var4) {
            Logger.getLogger(ApplicationMenu.class.getName()).log(Level.SEVERE, (String)null, var4);
        }

    }

    private void invokeMenuItem(JMenuItem menuitem, boolean shortcut) {
        if (menuitem != null && menuitem.isEnabled() && menuitem.isVisible() && this.extraMenuAction.allowMenuAction(this.window, this.menubar, menuitem, true, shortcut)) {
            menuitem.getModel().setArmed(true);
            menuitem.getModel().setPressed(true);
            this.extraMenuAction.invokeMenu(this.window, this.menubar, menuitem, true, shortcut);
            menuitem.getModel().setPressed(false);
            menuitem.getModel().setArmed(false);
        }

    }

    private void invokeSelectMenu(JMenu menu) {
        if (menu != null && menu.isEnabled() && menu.isVisible() && this.extraMenuAction.allowMenuAction(this.window, this.menubar, menu, true, false)) {
            this.extraMenuAction.beforInvokeMenu(this.window, this.menubar, menu, true, false);
            menu.getModel().setSelected(true);
            JPopupMenu popupMenu = menu.getPopupMenu();
            PopupMenuEvent pevent = new PopupMenuEvent(popupMenu);
            PopupMenuListener[] arr0$ = menu.getPopupMenu().getPopupMenuListeners();
            int len$ = arr0$.length;

            int i$;
            for(i$ = 0; i$ < len$; ++i$) {
                PopupMenuListener pl = arr0$[i$];
                if (pl != null) {
                    pl.popupMenuWillBecomeVisible(pevent);
                }
            }

            this.extraMenuAction.invokeMenu(this.window, this.menubar, menu, true, false);
            Component[] arr$ = popupMenu.getComponents();
            len$ = arr$.length;

            for(i$ = 0; i$ < len$; ++i$) {
                Component comp = arr$[i$];
                if (comp.isVisible()) {
                    if (comp instanceof JMenu) {
                        this.addMenu((JMenu)comp);
                    } else if (comp instanceof JMenuItem) {
                        this.addMenuItem((JMenuItem)comp);
                    } else if (comp instanceof JSeparator) {
                        this.addSeparator();
                    }
                }
            }

            this.extraMenuAction.afterInvokeMenu(this.window, this.menubar, menu, true, false);
        }

    }

    private void invokeDeselectMenu(JMenu menu) {
        if (menu != null && menu.isEnabled() && menu.isVisible() && this.extraMenuAction.allowMenuAction(this.window, this.menubar, menu, false, false)) {
            this.extraMenuAction.beforInvokeMenu(this.window, this.menubar, menu, false, false);
            this.extraMenuAction.invokeMenu(this.window, this.menubar, menu, false, false);
            PopupMenuEvent pevent = new PopupMenuEvent(menu.getPopupMenu());
            PopupMenuListener[] arr$ = menu.getPopupMenu().getPopupMenuListeners();
            int len$ = arr$.length;

            for(int i$ = 0; i$ < len$; ++i$) {
                PopupMenuListener pl = arr$[i$];
                if (pl != null) {
                    pl.popupMenuWillBecomeInvisible(pevent);
                }
            }

            menu.getModel().setSelected(false);
            this.extraMenuAction.afterInvokeMenu(this.window, this.menubar, menu, false, false);
        }

    }

    private void invokeAccelerator(int keycode, int modifiers) {
        this.invokeMenuItem(this.getJMenuItem(keycode, modifiers), true);
    }

    private Window getWindow(Component comp) {
        if (comp == null) {
            return null;
        } else if (comp instanceof JFrame) {
            return (Window)comp;
        } else {
            return comp instanceof JDialog ? (Window)comp : this.getWindow(comp.getParent());
        }
    }

    public void eventDispatched(AWTEvent event) {
        if (event.getID() == 402) {
            KeyEvent e = (KeyEvent)event;
            if (e.getKeyCode() != 18 && e.getKeyCode() != 16 && e.getKeyCode() != 17 && e.getKeyCode() != 157 && e.getKeyCode() != 65406 && this.window.isActive()) {
                Window currwindow;
                if (event.getSource() instanceof Component) {
                    currwindow = this.getWindow((Component)event.getSource());
                } else if (event.getSource() instanceof JFrame) {
                    currwindow = (Window)event.getSource();
                } else if (event.getSource() instanceof JDialog) {
                    currwindow = (Window)event.getSource();
                } else {
                    currwindow = null;
                }

                if (this.window.equals(currwindow)) {
                    this.invokeAccelerator(e.getKeyCode(), e.getModifiersEx() | e.getModifiers());
                }
            }
        }

    }

    private void rebuildMenuBar() {
        if (this.approveRebuild == -1L) {
            this.approveRebuild = System.currentTimeMillis() + 500L;
            (new Thread() {
                public void run() {
                    try {
                        SwingUtilities.invokeAndWait(new Runnable() {
                            public void run() {
                                ApplicationMenu.this.removeAllMenus();
                                ApplicationMenu.this.buildMenuBar();
                                ApplicationMenu.this.approveRebuild = -1L;
                            }
                        });
                    } catch (InterruptedException var2) {
                        Logger.getLogger(ApplicationMenu.class.getName()).log(Level.SEVERE, (String)null, var2);
                    } catch (InvocationTargetException var3) {
                        Logger.getLogger(ApplicationMenu.class.getName()).log(Level.SEVERE, (String)null, var3);
                    }

                }
            }).start();
        } else {
            this.approveRebuild = System.currentTimeMillis() + 500L;
        }

    }

    public void propertyChange(PropertyChangeEvent evt) {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new RuntimeException("Wrong thread");
        } else {
            if ("enabled".equals(evt.getPropertyName()) && evt.getSource() instanceof JMenu) {
                this.rebuildMenuBar();
            }

        }
    }

    public void componentAdded(ContainerEvent e) {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new RuntimeException("Wrong thread");
        } else {
            if (e.getChild() instanceof JMenu) {
                ((JMenu)e.getChild()).addComponentListener(this);
                ((JMenu)e.getChild()).addPropertyChangeListener(this);
                this.rebuildMenuBar();
            }

        }
    }

    public void componentRemoved(ContainerEvent e) {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new RuntimeException("Wrong thread");
        } else {
            if (e.getChild() instanceof JMenu) {
                ((JMenu)e.getChild()).removeComponentListener(this);
                ((JMenu)e.getChild()).removePropertyChangeListener(this);
                this.rebuildMenuBar();
            }

        }
    }

    public void componentShown(ComponentEvent e) {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new RuntimeException("Wrong thread");
        } else {
            if (e.getSource() instanceof JMenu) {
                this.rebuildMenuBar();
            }

        }
    }

    public void componentHidden(ComponentEvent e) {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new RuntimeException("Wrong thread");
        } else {
            if (e.getSource() instanceof JMenu) {
                this.rebuildMenuBar();
            }

        }
    }

    public void componentMoved(ComponentEvent e) {
    }

    public void componentResized(ComponentEvent e) {
    }

    public void windowActivated(WindowEvent e) {
        this.tryInstall();
    }

    public void windowClosed(WindowEvent e) {
        this.tryUninstall();
    }

    public void windowClosing(WindowEvent e) {
    }

    public void windowDeactivated(WindowEvent e) {
    }

    public void windowDeiconified(WindowEvent e) {
    }

    public void windowIconified(WindowEvent e) {
    }

    public void windowOpened(WindowEvent e) {
        this.tryInstall();
    }
}
