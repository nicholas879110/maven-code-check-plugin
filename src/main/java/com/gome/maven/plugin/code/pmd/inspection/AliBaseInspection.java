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
package com.gome.maven.plugin.code.pmd.inspection;


import com.gome.maven.plugin.code.pmd.util.HighlightDisplayLevel;

/**
 * @author caikang
 * @date 2016/12/08
 */
public interface AliBaseInspection {

    /**
     * ruleName
     *
     * @return ruleName
     */
    public String ruleName();

    /**
     * display info for inspection
     *
     * @return display
     */
    public String getDisplayName();

    /**
     * group display info for inspection
     *
     * @return group display
     */
    public String getGroupDisplayName();

    /**
     * inspection enable by default
     *
     * @return true -> enable
     */
    public boolean isEnabledByDefault();

    /**
     * default inspection level
     *
     * @return level
     */
    public HighlightDisplayLevel getDefaultLevel();

    /**
     * inspection short name
     *
     * @return shor name
     */
    public String getShortName();

    public String GROUP_NAME = "Ali-Check";
}
