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
package com.gome.maven.openapi.application.ex;

import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.components.impl.stores.DirectoryStorageData;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.util.io.URLUtil;
import gnu.trove.THashMap;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.Map;

public class DecodeDefaultsUtil {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.openapi.application.ex.DecodeDefaultsUtil");
    private static final Map<String, URL> RESOURCE_CACHE = Collections.synchronizedMap(new THashMap<String, URL>());

    public static URL getDefaults(Object requestor,  String componentResourcePath) {
        URL url = RESOURCE_CACHE.get(componentResourcePath);
        if (url == null) {
            Class<?> requestorClass = requestor.getClass();
            if (StringUtil.startsWithChar(componentResourcePath, '/')) {
                url = requestorClass.getResource(componentResourcePath + DirectoryStorageData.DEFAULT_EXT);
            }
            else {
                url = requestorClass.getResource('/' + ApplicationManagerEx.getApplicationEx().getName() + '/' + componentResourcePath + DirectoryStorageData.DEFAULT_EXT);
                if (url == null) {
                    url = requestorClass.getResource('/' + componentResourcePath + DirectoryStorageData.DEFAULT_EXT);
                }
            }
            RESOURCE_CACHE.put(componentResourcePath, url);
        }
        return url;
    }

    public static InputStream getDefaultsInputStream(Object requestor,  String componentResourcePath) {
        try {
            final URL defaults = getDefaults(requestor, componentResourcePath);
            return defaults == null ? null : URLUtil.openStream(defaults);
        }
        catch (IOException e) {
            LOG.error(e);
            return null;
        }
    }
}
