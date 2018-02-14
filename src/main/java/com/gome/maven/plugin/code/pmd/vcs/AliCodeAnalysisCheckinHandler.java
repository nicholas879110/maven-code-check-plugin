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
package com.gome.maven.plugin.code.pmd.vcs;

import com.gome.maven.plugin.code.pmd.inspection.AliLocalInspectionToolProvider;
import com.gome.maven.plugin.code.pmd.inspection.LocalInspectionTool;
import com.gome.maven.util.ReflectionUtil;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;

;import java.util.List;

/**
 * @author yaohui.wyh
 * @author caikang
 * @date 2017/03/21
 * @date 2017/05/04
 */
public class AliCodeAnalysisCheckinHandler implements CheckinHandler {
    private static Log log = new SystemStreamLog();


    AliCodeAnalysisCheckinHandler() {}

    public void doAnalysis() {
        List<Class<?>> tools= AliLocalInspectionToolProvider.getInspectionClasses();
        for (final Class aClass : tools) {
            LocalInspectionTool tool= (LocalInspectionTool)instantiateTool(aClass);
//            tool.checkFile();
        }

//        val tools = Inspections.aliInspections(project) { it.tool is AliBaseInspection }

    }

    static Object instantiateTool(Class<?> toolClass) {
        try {
            return ReflectionUtil.newInstance(toolClass,new Class[0]);
        }
        catch (RuntimeException e) {
            e.printStackTrace();
        }

        return null;
    }

}
