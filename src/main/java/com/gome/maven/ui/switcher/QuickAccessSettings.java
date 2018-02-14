/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.gome.maven.ui.switcher;

import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.actionSystem.KeyboardShortcut;
import com.gome.maven.openapi.actionSystem.Shortcut;
import com.gome.maven.openapi.application.Application;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.components.ApplicationComponent;
import com.gome.maven.openapi.keymap.Keymap;
import com.gome.maven.openapi.keymap.KeymapManager;
import com.gome.maven.openapi.keymap.KeymapManagerListener;
import com.gome.maven.openapi.util.Disposer;
import com.gome.maven.openapi.util.SystemInfo;
import com.gome.maven.openapi.util.registry.Registry;
import com.gome.maven.openapi.util.registry.RegistryValue;
import com.gome.maven.openapi.util.registry.RegistryValueListener;
import com.gome.maven.util.containers.ContainerUtil;
//import org.gome.maven.lang.annotations.JdkConstants;

import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.HashSet;
import java.util.Set;

public class QuickAccessSettings implements ApplicationComponent, KeymapManagerListener, Disposable {
    private final Set<Integer> myModifierVks = new HashSet<Integer>();
    private Keymap myKeymap;
     public static final String SWITCH_UP = "SwitchUp";
     public static final String SWITCH_DOWN = "SwitchDown";
     public static final String SWITCH_LEFT = "SwitchLeft";
     public static final String SWITCH_RIGHT = "SwitchRight";
     public static final String SWITCH_APPLY = "SwitchApply";
    private RegistryValue myModifiersValue;

    @Override
    
    public String getComponentName() {
        return "QuickAccess";
    }

    @Override
    public void initComponent() {
        myModifiersValue = Registry.get("actionSystem.quickAccessModifiers");
        myModifiersValue.addListener(new RegistryValueListener.Adapter() {
            @Override
            public void afterValueChanged(RegistryValue value) {
                applyModifiersFromRegistry();
            }
        }, this);

        KeymapManager kmMgr = KeymapManager.getInstance();
        kmMgr.addKeymapManagerListener(this);

        activeKeymapChanged(kmMgr.getActiveKeymap());

        applyModifiersFromRegistry();
    }

    @Override
    public void disposeComponent() {
        KeymapManager.getInstance().removeKeymapManagerListener(this);
        Disposer.dispose(this);
    }

    @Override
    public void dispose() {
    }

    @Override
    public void activeKeymapChanged(Keymap keymap) {
        myKeymap = KeymapManager.getInstance().getActiveKeymap();
    }

    Keymap getKeymap() {
        return myKeymap;
    }

    void saveModifiersToRegistry(Set<String> codeTexts) {
        StringBuilder value = new StringBuilder();
        for (String each : codeTexts) {
            if (value.length() > 0) {
                value.append(" ");
            }
            value.append(each);
        }
        myModifiersValue.setValue(value.toString());
    }

    private void applyModifiersFromRegistry() {
        Application app = ApplicationManager.getApplication();
        if (app != null && app.isUnitTestMode()) return;

        String text = getModifierRegistryValue();
        String[] vks = text.split(" ");

        HashSet<String> vksSet = new HashSet<String>();
        ContainerUtil.addAll(vksSet, vks);
        myModifierVks.clear();
        int mask = getModifierMask(vksSet);
        myModifierVks.addAll(getModifiersVKs(mask));

        reassignActionShortcut(SWITCH_UP, mask, KeyEvent.VK_UP);
        reassignActionShortcut(SWITCH_DOWN, mask, KeyEvent.VK_DOWN);
        reassignActionShortcut(SWITCH_LEFT, mask, KeyEvent.VK_LEFT);
        reassignActionShortcut(SWITCH_RIGHT, mask, KeyEvent.VK_RIGHT);
        reassignActionShortcut(SWITCH_APPLY, mask, KeyEvent.VK_ENTER);
    }

    private String getModifierRegistryValue() {
        String value = myModifiersValue.asString().trim();
        if (value.length() > 0) return value;

        if (SystemInfo.isMac) {
            return "control alt";
        }
        else {
            return "shift alt";
        }
    }

    private void reassignActionShortcut(String actionId, /*@JdkConstants.InputEventMask*/ int modifiers, int actionCode) {
        removeShortcuts(actionId);
        if (modifiers > 0) {
            myKeymap.addShortcut(actionId, new KeyboardShortcut(KeyStroke.getKeyStroke(actionCode, modifiers), null));
        }
    }

    private void removeShortcuts(String actionId) {
        Shortcut[] shortcuts = myKeymap.getShortcuts(actionId);
        for (Shortcut each : shortcuts) {
            if (each instanceof KeyboardShortcut) {
                myKeymap.removeShortcut(actionId, each);
            }
        }
    }

//    @JdkConstants.InputEventMask
    int getModifierMask(Set<String> codeTexts) {
        int mask = 0;
        for (String each : codeTexts) {
            if ("control".equals(each)) {
                mask |= InputEvent.CTRL_MASK;
            }
            else if ("shift".equals(each)) {
                mask |= InputEvent.SHIFT_MASK;
            }
            else if ("alt".equals(each)) {
                mask |= InputEvent.ALT_MASK;
            }
            else if ("meta".equals(each)) {
                mask |= InputEvent.META_MASK;
            }
        }

        return mask;
    }

    public static Set<Integer> getModifiersVKs(int mask) {
        Set<Integer> codes = new HashSet<Integer>();
        if ((mask & InputEvent.SHIFT_MASK) > 0) {
            codes.add(KeyEvent.VK_SHIFT);
        }
        if ((mask & InputEvent.CTRL_MASK) > 0) {
            codes.add(KeyEvent.VK_CONTROL);
        }

        if ((mask & InputEvent.META_MASK) > 0) {
            codes.add(KeyEvent.VK_META);
        }

        if ((mask & InputEvent.ALT_MASK) > 0) {
            codes.add(KeyEvent.VK_ALT);
        }

        return codes;
    }

    public static QuickAccessSettings getInstance() {
        return ApplicationManager.getApplication().getComponent(QuickAccessSettings.class);
    }

    public boolean isEnabled() {
        return Registry.is("actionSystem.quickAccessEnabled");
    }

    public Set<Integer> getModiferCodes() {
        return myModifierVks;
    }
}
