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
package com.gome.maven.diff.contents;

import com.gome.maven.diff.requests.DiffRequest;
import com.gome.maven.openapi.fileEditor.OpenFileDescriptor;
import com.gome.maven.openapi.fileTypes.FileType;

/**
 * Represents some data that probably can be compared with some other.
 *
 * @see DiffRequest
 */
public interface DiffContent {
    
    FileType getContentType();

    /**
     * Provides a way to open related content in editor
     */
    
    OpenFileDescriptor getOpenFileDescriptor();

    /*
     * @See DiffRequest.onAssigned()
     */
    
    void onAssigned(boolean isAssigned);
}
