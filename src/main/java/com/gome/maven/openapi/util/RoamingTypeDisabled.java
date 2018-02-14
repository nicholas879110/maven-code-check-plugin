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
package com.gome.maven.openapi.util;

/**
 * Label for JDOMExternalizable. If component implements this interface it will
 * not be passed to external StreamProvider components (for example, it will not be stored on IDEAServer)
 * @deprecated use {@link com.gome.maven.openapi.components.RoamingType#DISABLED}
 */
@Deprecated
public interface RoamingTypeDisabled {
}