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
package com.gome.maven.openapi.actionSystem;

import com.gome.maven.openapi.project.DumbAware;

/**
 * Represents a separator.
 */
public final class Separator extends AnAction implements DumbAware {
    private static final Separator ourInstance = new Separator();

    private String myText;

    public Separator() {
    }

    public Separator( final String text) {
        myText = text;
    }

    public String getText() {
        return myText;
    }

    public static Separator getInstance() {
        return ourInstance;
    }

    @Override
    public String toString() {
        return "Separator (" + myText + ")";
    }

    @Override
    public void actionPerformed(AnActionEvent e){
        throw new UnsupportedOperationException();
    }
}
