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
package org.jetbrains.ide;

import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.components.ApplicationComponent;
import com.gome.maven.util.Url;

public abstract class BuiltInServerManager extends ApplicationComponent.Adapter {
    public static BuiltInServerManager getInstance() {
        return ApplicationManager.getApplication().getComponent(BuiltInServerManager.class);
    }

    public abstract int getPort();

    public abstract BuiltInServerManager waitForStart();

    public abstract Disposable getServerDisposable();

    public abstract boolean isOnBuiltInWebServer( Url url);
}