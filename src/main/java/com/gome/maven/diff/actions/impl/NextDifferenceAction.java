/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.gome.maven.diff.actions.impl;

import com.gome.maven.openapi.actionSystem.AnAction;
import com.gome.maven.openapi.actionSystem.EmptyAction;
import com.gome.maven.openapi.actionSystem.IdeActions;
import com.gome.maven.openapi.project.DumbAware;

public abstract class NextDifferenceAction extends AnAction implements DumbAware {
    public NextDifferenceAction() {
        setEnabledInModalContext(true);
        EmptyAction.setupAction(this, IdeActions.ACTION_NEXT_DIFF, null);
    }
}
