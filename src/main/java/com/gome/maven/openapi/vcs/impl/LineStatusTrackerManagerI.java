/*
 * Copyright 2000-2010 JetBrains s.r.o.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gome.maven.openapi.vcs.impl;

import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.vcs.ex.LineStatusTracker;

/**
 * @author irengrig
 */
public interface LineStatusTrackerManagerI {

    LineStatusTracker getLineStatusTracker(Document document);

    class Dummy implements LineStatusTrackerManagerI {
        private final static Dummy ourInstance = new Dummy();

        public static Dummy getInstance() {
            return ourInstance;
        }

        @Override
        public LineStatusTracker getLineStatusTracker(final Document document) {
            return null;
        }
    }
}
