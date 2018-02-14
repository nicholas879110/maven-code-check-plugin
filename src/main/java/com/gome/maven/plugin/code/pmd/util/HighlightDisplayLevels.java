/*
 * Copyright 1999-2017 Alibaba Group.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gome.maven.plugin.code.pmd.util;

import net.sourceforge.pmd.RulePriority;

/**
 *
 *
 * @author caikang
 * @date 2017/02/04
 */
public class HighlightDisplayLevels {
    public static HighlightDisplayLevel BLOCKER =new  HighlightDisplayLevel(HighlightDisplayLevel.BLOCKER, HighlightDisplayLevel.BLOCKER_LEVEL);
    public static HighlightDisplayLevel CRITICAL =new HighlightDisplayLevel(HighlightDisplayLevel.CRITICAL, HighlightDisplayLevel.CRITICAL_LEVEL);
    public static HighlightDisplayLevel MAJOR = new HighlightDisplayLevel(HighlightDisplayLevel.MAJOR, HighlightDisplayLevel.MAJOR_LEVEL);

    public static HighlightDisplayLevel getHighlightDisplayLevel(RulePriority rulePriority){
        if (rulePriority.equals( RulePriority.HIGH)){
            return HighlightDisplayLevels.BLOCKER;
        }else if (rulePriority.equals(RulePriority.MEDIUM_HIGH)){
            return HighlightDisplayLevels.CRITICAL;
        }else {
            return HighlightDisplayLevels.MAJOR;
        }
    }
}
