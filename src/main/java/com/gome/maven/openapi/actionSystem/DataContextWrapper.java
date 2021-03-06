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
package com.gome.maven.openapi.actionSystem;

import com.gome.maven.openapi.util.Key;
import com.gome.maven.openapi.util.UserDataHolder;
import com.gome.maven.openapi.util.UserDataHolderBase;

// We implement UserDataHolder to support DataManager.saveInDataContext/loadFromDataContext methods
public class DataContextWrapper implements DataContext, UserDataHolder {
    private final DataContext myDelegate;
    private final UserDataHolder myDataHolder;

    public DataContextWrapper( DataContext delegate) {
        myDelegate = delegate;
        myDataHolder = delegate instanceof UserDataHolder ? (UserDataHolder) delegate : new UserDataHolderBase();
    }

    
    @Override
    public Object getData( String dataId) {
        return myDelegate.getData(dataId);
    }

    
    @Override
    public <T> T getUserData( Key<T> key) {
        return myDataHolder.getUserData(key);
    }

    @Override
    public <T> void putUserData( Key<T> key,  T value) {
        myDataHolder.putUserData(key, value);
    }
}
