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
package com.gome.maven.openapi.file.exclude;

import com.gome.maven.icons.AllIcons;
import com.gome.maven.openapi.fileTypes.FileTypeConsumer;
import com.gome.maven.openapi.fileTypes.FileTypeFactory;
import com.gome.maven.openapi.fileTypes.ex.FileTypeIdentifiableByVirtualFile;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.ui.LayeredIcon;
import com.gome.maven.util.PlatformIcons;

import javax.swing.*;

/**
 * Registers text file type for particular virtual files rather than using .txt extension.
 * @author Rustam Vishnyakov
 */
public class EnforcedPlainTextFileTypeFactory extends FileTypeFactory {

    public static final LayeredIcon ENFORCED_PLAIN_TEXT_ICON = new LayeredIcon(2);
    public static final String ENFORCED_PLAIN_TEXT = "Enforced Plain Text";

    static {
        ENFORCED_PLAIN_TEXT_ICON.setIcon(AllIcons.FileTypes.Text, 0);
        ENFORCED_PLAIN_TEXT_ICON.setIcon(PlatformIcons.EXCLUDED_FROM_COMPILE_ICON, 1);
    }


    private final FileTypeIdentifiableByVirtualFile myFileType;


    public EnforcedPlainTextFileTypeFactory() {


        myFileType = new FileTypeIdentifiableByVirtualFile() {

            @Override
            public boolean isMyFileType( VirtualFile file) {
                return isMarkedAsPlainText(file);
            }

            
            @Override
            public String getName() {
                return ENFORCED_PLAIN_TEXT;
            }

            
            @Override
            public String getDescription() {
                return ENFORCED_PLAIN_TEXT;
            }

            
            @Override
            public String getDefaultExtension() {
                return "fakeTxt";
            }

            @Override
            public Icon getIcon() {
                return ENFORCED_PLAIN_TEXT_ICON;
            }

            @Override
            public boolean isBinary() {
                return false;
            }

            @Override
            public boolean isReadOnly() {
                return true;
            }

            @Override
            public String getCharset( VirtualFile file,  byte[] content) {
                return null;
            }
        };
    }

    @Override
    public void createFileTypes(final  FileTypeConsumer consumer) {
        consumer.consume(myFileType, "");
    }

    private static boolean isMarkedAsPlainText(VirtualFile file) {
        EnforcedPlainTextFileTypeManager typeManager = EnforcedPlainTextFileTypeManager.getInstance();
        if (typeManager == null) return false;
        return typeManager.isMarkedAsPlainText(file);
    }

}
