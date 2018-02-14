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
package com.gome.maven.openapi.vcs;

import com.gome.maven.openapi.editor.colors.ColorKey;
import com.gome.maven.openapi.editor.colors.EditorColorsManager;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class FileStatusFactory {
    private static final FileStatusFactory ourInstance = new FileStatusFactory();
    private final List<FileStatus> myStatuses = new ArrayList<FileStatus>();

    private FileStatusFactory() {
    }

    public synchronized FileStatus createFileStatus(  String id,  String description, Color color) {
        FileStatusImpl result = new FileStatusImpl(id, ColorKey.createColorKey("FILESTATUS_" + id, color), description);
        myStatuses.add(result);
        return result;
    }

    public synchronized FileStatus createOnlyColorForFileStatus(  String id, final Color color) {
        FileStatus result = new FileStatusImpl.OnlyColorFileStatus(id, ColorKey.createColorKey("FILESTATUS_" + id, color), null);
        myStatuses.add(result);
        return result;
    }

    public synchronized FileStatus[] getAllFileStatuses() {
        return myStatuses.toArray(new FileStatus[myStatuses.size()]);
    }

    public static FileStatusFactory getInstance() {
        return ourInstance;
    }

    /**
     * author: lesya
     */
    private static class FileStatusImpl implements FileStatus {
        private final String myStatus;
        private final ColorKey myColorKey;
        private final String myText;

        public FileStatusImpl( String status,  ColorKey key, String text) {
            myStatus = status;
            myColorKey = key;
            myText = text;
        }

        public String toString() {
            return myStatus;
        }

        @Override
        public String getText() {
            return myText;
        }

        @Override
        public Color getColor() {
            return EditorColorsManager.getInstance().getGlobalScheme().getColor(getColorKey());
        }

        
        @Override
        public ColorKey getColorKey() {
            return myColorKey;
        }

        
        @Override
        public String getId() {
            return myStatus;
        }

        private static class OnlyColorFileStatus extends FileStatusImpl {
            public OnlyColorFileStatus( String status,  ColorKey key, String text) {
                super(status, key, text);
            }

            
            @Override
            public String getId() {
                throw new UnsupportedOperationException();
            }

            @Override
            public String getText() {
                throw new UnsupportedOperationException();
            }
        }
    }
}
