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
package com.gome.maven.openapi.options.ex;

import com.gome.maven.openapi.actionSystem.DataKey;
import com.gome.maven.openapi.options.Configurable;
import com.gome.maven.openapi.options.ConfigurableGroup;
import com.gome.maven.openapi.options.UnnamedConfigurable;
import com.gome.maven.openapi.util.ActionCallback;

import java.util.IdentityHashMap;

/**
 * @author Sergey.Malenkov
 */
public abstract class Settings {
    public static final DataKey<Settings> KEY = DataKey.create("settings.editor");

    private final ConfigurableGroup[] myGroups;
    private final IdentityHashMap<UnnamedConfigurable, ConfigurableWrapper>
            myMap = new IdentityHashMap<UnnamedConfigurable, ConfigurableWrapper>();

    protected Settings( ConfigurableGroup... groups) {
        myGroups = groups;
    }

    
    public final <T extends Configurable> T find( Class<T> type) {
        return unwrap(new ConfigurableVisitor.ByType(type).find(myGroups), type);
    }

    
    public final Configurable find( String id) {
        return unwrap(new ConfigurableVisitor.ByID(id).find(myGroups), Configurable.class);
    }

    
    public final ActionCallback select(Configurable configurable) {
        return configurable != null
                ? selectImpl(choose(configurable, myMap.get(configurable)))
                : new ActionCallback.Rejected();
    }

    protected abstract ActionCallback selectImpl(Configurable configurable);

    private <T extends Configurable> T unwrap(Configurable configurable, Class<T> type) {
        T result = ConfigurableWrapper.cast(type, configurable);
        if (result != null && configurable instanceof ConfigurableWrapper) {
            myMap.put(result, (ConfigurableWrapper)configurable);
        }
        return result;
    }

    private static Configurable choose(Configurable configurable, Configurable variant) {
        return variant != null ? variant : configurable;
    }
}
