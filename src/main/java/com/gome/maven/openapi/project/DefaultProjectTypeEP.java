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
package com.gome.maven.openapi.project;

import com.gome.maven.openapi.extensions.ExtensionPointName;
import com.gome.maven.util.xmlb.annotations.Attribute;

/**
 * @author Dmitry Avdeev
 */
public class DefaultProjectTypeEP {

    private final static ExtensionPointName<DefaultProjectTypeEP> EXTENSION_POINT_NAME = ExtensionPointName.create("com.gome.maven.defaultProjectTypeProvider");
    @Attribute("type")
    public String type;

    public static ProjectType getDefaultProjectType() {
        DefaultProjectTypeEP[] extensions = EXTENSION_POINT_NAME.getExtensions();
        return extensions.length > 0 ? new ProjectType(extensions[0].type) : null;
    }
}
