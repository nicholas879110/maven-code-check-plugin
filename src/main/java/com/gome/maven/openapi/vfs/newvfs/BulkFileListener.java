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
package com.gome.maven.openapi.vfs.newvfs;

import com.gome.maven.openapi.vfs.newvfs.events.VFileEvent;

import java.util.List;

/**
 * @author max
 */
public interface BulkFileListener {
    class Adapter implements BulkFileListener {
        @Override public void before( List<? extends VFileEvent> events) { }
        @Override public void after( List<? extends VFileEvent> events) { }
    }

    void before( List<? extends VFileEvent> events);
    void after( List<? extends VFileEvent> events);
}