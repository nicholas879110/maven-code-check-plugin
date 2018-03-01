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
package com.gome.maven.diff.tools;

import com.gome.maven.diff.DiffContext;
import com.gome.maven.diff.FrameDiffTool;
import com.gome.maven.diff.requests.DiffRequest;
import com.gome.maven.diff.requests.ErrorDiffRequest;
import com.gome.maven.diff.requests.MessageDiffRequest;
import com.gome.maven.diff.util.DiffUtil;

import javax.swing.*;
import java.awt.*;

public class ErrorDiffTool implements FrameDiffTool {
    public static final ErrorDiffTool INSTANCE = new ErrorDiffTool();

    
    @Override
    public DiffViewer createComponent( DiffContext context,  DiffRequest request) {
        return new MyViewer(context, request);
    }

    @Override
    public boolean canShow( DiffContext context,  DiffRequest request) {
        return true;
    }

    
    @Override
    public String getName() {
        return "Error viewer";
    }

    private static class MyViewer implements DiffViewer {
         private final JPanel myPanel;

        public MyViewer( DiffContext context,  DiffRequest request) {
            myPanel = new JPanel(new BorderLayout());

            JPanel centerPanel = DiffUtil.createMessagePanel(getMessage(request));

            myPanel.add(centerPanel, BorderLayout.CENTER);
        }

        
        private static String getMessage( DiffRequest request) {
            if (request instanceof ErrorDiffRequest) {
                // TODO: explain some of exceptions ?
                return ((ErrorDiffRequest)request).getMessage();
            }
            if (request instanceof MessageDiffRequest) {
                return ((MessageDiffRequest)request).getMessage();
            }

            return "Can't show diff";
        }

        
        @Override
        public JComponent getComponent() {
            return myPanel;
        }

        
        @Override
        public JComponent getPreferredFocusedComponent() {
            return null;
        }

        
        @Override
        public ToolbarComponents init() {
            return new ToolbarComponents();
        }

        @Override
        public void dispose() {
        }
    }
}
