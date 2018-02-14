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
package com.gome.maven.codeEditor.printing;

import com.gome.maven.openapi.components.*;
import com.gome.maven.openapi.editor.colors.EditorColorsManager;
import com.gome.maven.util.xmlb.XmlSerializerUtil;

@State(
        name = "PrintSettings",
        storages = {@Storage(file = StoragePathMacros.APP_CONFIG + "/print.xml")}
)
public class PrintSettings implements PersistentStateComponent<PrintSettings> {
     public String PAPER_SIZE = "A4";

    public boolean COLOR_PRINTING = false;
    public boolean SYNTAX_PRINTING = true;
    public boolean PRINT_AS_GRAPHICS = true;

    public boolean PORTRAIT_LAYOUT = true;

     public String FONT_NAME = EditorColorsManager.getInstance().getGlobalScheme().getEditorFontName();
    public int FONT_SIZE = EditorColorsManager.getInstance().getGlobalScheme().getEditorFontSize();

    public boolean PRINT_LINE_NUMBERS = true;

    public boolean WRAP = true;

    public float TOP_MARGIN = 0.5f;
    public float BOTTOM_MARGIN = 1.0f;
    public float LEFT_MARGIN = 1.0f;
    public float RIGHT_MARGIN = 1.0f;

    public boolean DRAW_BORDER = true;

    public String FOOTER_HEADER_TEXT1 = CodeEditorBundle.message("print.header.default.line.1");
    public String FOOTER_HEADER_PLACEMENT1 = HEADER;
    public String FOOTER_HEADER_ALIGNMENT1 = LEFT;
    public String FOOTER_HEADER_TEXT2 = CodeEditorBundle.message("print.header.default.line.2");
    public String FOOTER_HEADER_PLACEMENT2 = FOOTER;
    public String FOOTER_HEADER_ALIGNMENT2 = CENTER;
    public int FOOTER_HEADER_FONT_SIZE = 8;
     public String FOOTER_HEADER_FONT_NAME = "Arial";

    public static final int PRINT_FILE = 1;
    public static final int PRINT_SELECTED_TEXT = 2;
    public static final int PRINT_DIRECTORY = 4;
    private int myPrintScope;
    private boolean myIncludeSubdirectories;

     public static final String HEADER = "Header";
     public static final String FOOTER = "Footer";

     public static final String LEFT = "Left";
     public static final String CENTER = "Center";
     public static final String RIGHT = "Right";

    public static PrintSettings getInstance() {
        return ServiceManager.getService(PrintSettings.class);
    }

    public int getPrintScope() {
        return myPrintScope;
    }

    public void setPrintScope(int printScope) {
        myPrintScope = printScope;
    }

    public boolean isIncludeSubdirectories() {
        return myIncludeSubdirectories;
    }

    public void setIncludeSubdirectories(boolean includeSubdirectories) {
        myIncludeSubdirectories = includeSubdirectories;
    }

    @Override
    public PrintSettings getState() {
        return this;
    }

    @Override
    public void loadState(final PrintSettings state) {
        XmlSerializerUtil.copyBean(state, this);
    }
}