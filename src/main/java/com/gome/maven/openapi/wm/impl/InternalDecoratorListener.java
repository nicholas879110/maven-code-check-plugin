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
package com.gome.maven.openapi.wm.impl;

import com.gome.maven.openapi.wm.ToolWindowAnchor;
import com.gome.maven.openapi.wm.ToolWindowContentUiType;
import com.gome.maven.openapi.wm.ToolWindowType;

import java.util.EventListener;

/**
 * @author Vladimir Kondratyev
 */
interface InternalDecoratorListener extends EventListener{

    public void anchorChanged(InternalDecorator source,ToolWindowAnchor anchor);

    public void autoHideChanged(InternalDecorator source,boolean autoHide);

    public void hidden(InternalDecorator source);

    public void hiddenSide(InternalDecorator source);

    public void resized(InternalDecorator source);

    public void activated(InternalDecorator source);

    public void typeChanged(InternalDecorator source,ToolWindowType type);

    public void sideStatusChanged(InternalDecorator source,boolean isSideTool);

    public void contentUiTypeChanges(InternalDecorator sources, ToolWindowContentUiType type);

}
