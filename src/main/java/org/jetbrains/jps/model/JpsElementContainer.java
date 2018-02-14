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
package org.jetbrains.jps.model;



/**
 * @author nik
 */
public interface JpsElementContainer {
  <T extends JpsElement>
  T getChild( JpsElementChildRole<T> role);


  <T extends JpsElement, K extends JpsElementChildRole<T> &JpsElementCreator<T>>
  T setChild( K role);


  <T extends JpsElement, K extends JpsElementChildRole<T> &JpsElementCreator<T>>
  T getOrSetChild( K role);


  <T extends JpsElement, P, K extends JpsElementChildRole<T> &JpsElementParameterizedCreator<T, P>>
  T setChild( K role,  P param);

  <T extends JpsElement>
  T setChild(JpsElementChildRole<T> role, T child);

  <T extends JpsElement>
  void removeChild( JpsElementChildRole<T> role);
}
