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
package com.gome.maven.execution.process;

import com.gome.maven.execution.ui.ConsoleViewContentType;
import com.gome.maven.openapi.editor.colors.TextAttributesKey;

/**
 * @author oleg
 */
public class ConsoleHighlighter {
     static final String BLACK_ID = "CONSOLE_BLACK_OUTPUT";
     static final String RED_ID = "CONSOLE_RED_OUTPUT";
     static final String GREEN_ID = "CONSOLE_GREEN_OUTPUT";
     static final String YELLOW_ID = "CONSOLE_YELLOW_OUTPUT";
     static final String BLUE_ID = "CONSOLE_BLUE_OUTPUT";
     static final String MAGENTA_ID = "CONSOLE_MAGENTA_OUTPUT";
     static final String CYAN_ID = "CONSOLE_CYAN_OUTPUT";
     static final String GRAY_ID = "CONSOLE_GRAY_OUTPUT"; //ISO white


     static final String DARKGRAY_ID = "CONSOLE_DARKGRAY_OUTPUT";
     static final String RED_BRIGHT_ID = "CONSOLE_RED_BRIGHT_OUTPUT";
     static final String GREEN_BRIGHT_ID = "CONSOLE_GREEN_BRIGHT_OUTPUT";
     static final String YELLOW_BRIGHT_ID = "CONSOLE_YELLOW_BRIGHT_OUTPUT";
     static final String BLUE_BRIGHT_ID = "CONSOLE_BLUE_BRIGHT_OUTPUT";
     static final String MAGENTA_BRIGHT_ID = "CONSOLE_MAGENTA_BRIGHT_OUTPUT";
     static final String CYAN_BRIGHT_ID = "CONSOLE_CYAN_BRIGHT_OUTPUT";
     static final String WHITE_ID = "CONSOLE_WHITE_OUTPUT"; //ISO bright white

    public static final TextAttributesKey BLACK =
            TextAttributesKey.createTextAttributesKey(BLACK_ID, ConsoleViewContentType.NORMAL_OUTPUT_KEY);
    public static final TextAttributesKey RED = TextAttributesKey.createTextAttributesKey(RED_ID, ConsoleViewContentType.NORMAL_OUTPUT_KEY);
    public static final TextAttributesKey GREEN =
            TextAttributesKey.createTextAttributesKey(GREEN_ID, ConsoleViewContentType.NORMAL_OUTPUT_KEY);
    public static final TextAttributesKey YELLOW =
            TextAttributesKey.createTextAttributesKey(YELLOW_ID, ConsoleViewContentType.NORMAL_OUTPUT_KEY);
    public static final TextAttributesKey BLUE = TextAttributesKey.createTextAttributesKey(BLUE_ID, ConsoleViewContentType.NORMAL_OUTPUT_KEY);
    public static final TextAttributesKey MAGENTA =
            TextAttributesKey.createTextAttributesKey(MAGENTA_ID, ConsoleViewContentType.NORMAL_OUTPUT_KEY);
    public static final TextAttributesKey CYAN = TextAttributesKey.createTextAttributesKey(CYAN_ID, ConsoleViewContentType.NORMAL_OUTPUT_KEY);
    public static final TextAttributesKey GRAY = TextAttributesKey.createTextAttributesKey(GRAY_ID, ConsoleViewContentType.NORMAL_OUTPUT_KEY);


    public static final TextAttributesKey DARKGRAY =
            TextAttributesKey.createTextAttributesKey(DARKGRAY_ID, GRAY);
    public static final TextAttributesKey RED_BRIGHT =
            TextAttributesKey.createTextAttributesKey(RED_BRIGHT_ID, RED);
    public static final TextAttributesKey GREEN_BRIGHT =
            TextAttributesKey.createTextAttributesKey(GREEN_BRIGHT_ID, GREEN);
    public static final TextAttributesKey YELLOW_BRIGHT =
            TextAttributesKey.createTextAttributesKey(YELLOW_BRIGHT_ID, YELLOW);
    public static final TextAttributesKey BLUE_BRIGHT =
            TextAttributesKey.createTextAttributesKey(BLUE_BRIGHT_ID, BLUE);
    public static final TextAttributesKey MAGENTA_BRIGHT =
            TextAttributesKey.createTextAttributesKey(MAGENTA_BRIGHT_ID, MAGENTA);
    public static final TextAttributesKey CYAN_BRIGHT =
            TextAttributesKey.createTextAttributesKey(CYAN_BRIGHT_ID, CYAN);
    public static final TextAttributesKey WHITE =
            TextAttributesKey.createTextAttributesKey(WHITE_ID, ConsoleViewContentType.NORMAL_OUTPUT_KEY);


    private ConsoleHighlighter() {
    }
}