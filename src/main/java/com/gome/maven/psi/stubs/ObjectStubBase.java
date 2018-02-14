/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.gome.maven.psi.stubs;

import com.gome.maven.openapi.util.UserDataHolderBase;

/**
 * @author Dmitry Avdeev
 *         Date: 8/3/12
 */
public abstract class ObjectStubBase<T extends Stub>  extends UserDataHolderBase implements Stub {

    protected final T myParent;
    public int id;

    public ObjectStubBase(T parent) {
        myParent = parent;
    }

    public T getParentStub() {
        return myParent;
    }
}
