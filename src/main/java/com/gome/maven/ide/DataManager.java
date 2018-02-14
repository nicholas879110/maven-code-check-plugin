/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.gome.maven.ide;

import com.gome.maven.openapi.actionSystem.DataContext;
import com.gome.maven.openapi.actionSystem.DataProvider;
import com.gome.maven.openapi.application.Application;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.util.AsyncResult;
import com.gome.maven.openapi.util.Key;

import javax.swing.*;
import java.awt.*;

public abstract class DataManager {
    public static DataManager getInstance() {
        Application application = ApplicationManager.getApplication();
        return application != null ? application.getComponent(DataManager.class) : null;
    }

     public static final String CLIENT_PROPERTY_DATA_PROVIDER = "DataProvider";

    /**
     * @return {@link DataContext} constructed by the current focused component
     * @deprecated use either {@link #getDataContext(java.awt.Component)} or {@link #getDataContextFromFocus()}
     */
    
    public abstract DataContext getDataContext();

    public abstract AsyncResult<DataContext> getDataContextFromFocus();

    /**
     * @return {@link DataContext} constructed by the specified <code>component</code>
     */
    public abstract DataContext getDataContext(Component component);

    /**
     * @return {@link DataContext} constructed be the specified <code>component</code>
     * and the point specified by <code>x</code> and <code>y</code> coordinate inside the
     * component.
     *
     * @exception java.lang.IllegalArgumentException if point <code>(x, y)</code> is not inside
     * component's bounds
     */
    public abstract DataContext getDataContext( Component component, int x, int y);

    /**
     * @param dataContext should be instance of {@link com.gome.maven.openapi.util.UserDataHolder}
     * @param dataKey key to store value
     * @param data value to store
     */
    public abstract <T> void saveInDataContext( DataContext dataContext,  Key<T> dataKey,  T data);

    /**
     * @param dataContext find by key if instance of {@link com.gome.maven.openapi.util.UserDataHolder}
     * @param dataKey key to find value by
     * @return value stored by {@link #saveInDataContext(com.gome.maven.openapi.actionSystem.DataContext, com.gome.maven.openapi.util.Key, Object)}
     */
    
    public abstract <T> T loadFromDataContext( DataContext dataContext,  Key<T> dataKey);

    public static void registerDataProvider( JComponent component,  DataProvider provider) {
        component.putClientProperty(CLIENT_PROPERTY_DATA_PROVIDER, provider);
    }

    
    public static DataProvider getDataProvider( JComponent component) {
        return (DataProvider)component.getClientProperty(CLIENT_PROPERTY_DATA_PROVIDER);
    }

    public static void removeDataProvider( JComponent component) {
        component.putClientProperty(CLIENT_PROPERTY_DATA_PROVIDER, null);
    }
}
