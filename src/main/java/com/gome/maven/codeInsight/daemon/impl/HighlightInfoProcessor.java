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
package com.gome.maven.codeInsight.daemon.impl;

import com.gome.maven.openapi.util.TextRange;

import java.util.List;

public abstract class HighlightInfoProcessor {
    public void highlightsInsideVisiblePartAreProduced( HighlightingSession highlightingSession,
                                                        List<HighlightInfo> infos,
                                                        TextRange priorityRange,
                                                        TextRange restrictRange, int groupId) {}
    public void highlightsOutsideVisiblePartAreProduced( HighlightingSession highlightingSession,
                                                         List<HighlightInfo> infos,
                                                         TextRange priorityRange,
                                                         TextRange restrictedRange, int groupId) {}

    public void infoIsAvailable( HighlightingSession highlightingSession,  HighlightInfo info) {}
    public void allHighlightsForRangeAreProduced( HighlightingSession highlightingSession,
                                                  TextRange elementRange,
                                                  List<HighlightInfo> infos){}

    public void progressIsAdvanced( HighlightingSession highlightingSession, double progress){}


    private static final HighlightInfoProcessor EMPTY = new HighlightInfoProcessor() { };
    
    public static HighlightInfoProcessor getEmpty() {
        return EMPTY;
    }
}
