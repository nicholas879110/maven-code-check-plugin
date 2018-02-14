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
package com.gome.maven.util.net;

import com.gome.maven.openapi.options.ConfigurableBase;

public class HttpProxyConfigurable extends ConfigurableBase<HttpProxySettingsUi, HttpConfigurable> {
    private final HttpConfigurable settings;

    public HttpProxyConfigurable() {
        this(HttpConfigurable.getInstance());
    }

    public HttpProxyConfigurable( HttpConfigurable settings) {
        super("http.proxy", "HTTP Proxy", "http.proxy");

        this.settings = settings;
    }

    
    @Override
    protected HttpConfigurable getSettings() {
        return settings;
    }

    @Override
    protected HttpProxySettingsUi createUi() {
        return new HttpProxySettingsUi(settings);
    }
}