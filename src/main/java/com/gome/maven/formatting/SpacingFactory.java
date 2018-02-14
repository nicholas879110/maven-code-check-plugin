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
package com.gome.maven.formatting;

import com.gome.maven.openapi.util.TextRange;

/**
 * Internal interface for creating spacing instances.
 */

interface SpacingFactory {

    
    Spacing createSpacing(int minSpaces,
                          int maxSpaces,
                          int minLineFeeds,
                          boolean keepLineBreaks,
                          int keepBlankLines);

    
    Spacing getReadOnlySpacing();

    
    Spacing createDependentLFSpacing(int minSpaces,
                                     int maxSpaces,
                                      TextRange dependencyRange,
                                     boolean keepLineBreaks,
                                     int keepBlankLines,
                                      DependentSpacingRule rule);

    
    Spacing createSafeSpacing(boolean keepLineBreaks,
                              int keepBlankLines);

    
    Spacing createKeepingFirstColumnSpacing(final int minSpaces,
                                            final int maxSpaces,
                                            final boolean keepLineBreaks,
                                            final int keepBlankLines);

    
    Spacing createSpacing(final int minSpaces, final int maxSpaces, final int minLineFeeds, final boolean keepLineBreaks,
                          final int keepBlankLines, final int prefLineFeeds);
}
