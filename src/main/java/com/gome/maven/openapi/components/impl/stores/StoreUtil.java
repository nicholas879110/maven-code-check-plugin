/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.gome.maven.openapi.components.impl.stores;

//import com.gome.maven.diagnostic.IdeErrorsDialog;
import com.gome.maven.diagnostic.IdeErrorsDialog;
import com.gome.maven.diagnostic.PluginException;
import com.gome.maven.ide.plugins.PluginManagerCore;
import com.gome.maven.notification.Notification;
import com.gome.maven.notification.NotificationType;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.application.ApplicationNamesInfo;
import com.gome.maven.openapi.application.ex.ApplicationManagerEx;
import com.gome.maven.openapi.components.PersistentStateComponent;
import com.gome.maven.openapi.components.State;
import com.gome.maven.openapi.components.StateStorage.SaveSession;
import com.gome.maven.openapi.extensions.PluginId;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.Pair;
import com.gome.maven.openapi.util.ShutDownTracker;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.util.SmartList;


public final class StoreUtil {
    private static final Logger LOG = Logger.getInstance(StoreUtil.class);

    private StoreUtil() {
    }

    public static void save( IComponentStore stateStore,  Project project) {
        ShutDownTracker.getInstance().registerStopperThread(Thread.currentThread());
        try {
            stateStore.save(new SmartList<Pair<SaveSession, VirtualFile>>());
        }
        catch (IComponentStore.SaveCancelledException e) {
            LOG.info(e);
        }
        catch (Throwable e) {
            if (ApplicationManager.getApplication().isUnitTestMode()) {
                LOG.error("Save settings failed", e);
            }
            else {
                LOG.warn("Save settings failed", e);
            }

            String messagePostfix = " Please restart " + ApplicationNamesInfo.getInstance().getFullProductName() + "</p>" +
                    (ApplicationManagerEx.getApplicationEx().isInternal() ? "<p>" + StringUtil.getThrowableText(e) + "</p>" : "");
            PluginId pluginId = IdeErrorsDialog.findPluginId(e);
            if (pluginId == null) {
                new Notification("Settings Error", "Unable to save settings",
                        "<p>Failed to save settings." + messagePostfix,
                        NotificationType.ERROR).notify(project);
            }
            else {
                PluginManagerCore.disablePlugin(pluginId.getIdString());

                new Notification("Settings Error", "Unable to save plugin settings",
                        "<p>The plugin <i>" + pluginId + "</i> failed to save settings and has been disabled." + messagePostfix,
                        NotificationType.ERROR).notify(project);
            }
        }
        finally {
            ShutDownTracker.getInstance().unregisterStopperThread(Thread.currentThread());
        }
    }

    
    public static <T> State getStateSpec( PersistentStateComponent<T> persistentStateComponent) {
        Class<? extends PersistentStateComponent> componentClass = persistentStateComponent.getClass();
        State spec = getStateSpec(componentClass);
        if (spec != null) {
            return spec;
        }

        PluginId pluginId = PluginManagerCore.getPluginByClassName(componentClass.getName());
        if (pluginId == null) {
            throw new RuntimeException("No @State annotation found in " + componentClass);
        }
        else {
            throw new PluginException("No @State annotation found in " + componentClass, pluginId);
        }
    }

    
    public static State getStateSpec( Class<?> aClass) {
        do {
            State stateSpec = aClass.getAnnotation(State.class);
            if (stateSpec != null) {
                return stateSpec;
            }
        }
        while ((aClass = aClass.getSuperclass()) != null);
        return null;
    }
}
