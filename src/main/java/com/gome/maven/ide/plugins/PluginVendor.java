/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package com.gome.maven.ide.plugins;

import com.gome.maven.util.xmlb.annotations.Attribute;
import com.gome.maven.util.xmlb.annotations.Tag;
import com.gome.maven.util.xmlb.annotations.Text;

@Tag("vendor")
public class PluginVendor {
    @Attribute("url")
    public String url;

    @Attribute("email")
    public String email;

    @Attribute("logo")
    public String logo;

    @Text
    public String name;
}
