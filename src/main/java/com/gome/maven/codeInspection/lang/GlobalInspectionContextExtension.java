/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

/*
 * User: anna
 * Date: 19-Dec-2007
 */
package com.gome.maven.codeInspection.lang;

import com.gome.maven.codeInspection.GlobalInspectionContext;
import com.gome.maven.codeInspection.ex.InspectionToolWrapper;
import com.gome.maven.codeInspection.ex.Tools;
import com.gome.maven.openapi.util.Key;

import java.util.List;

public interface GlobalInspectionContextExtension<T> {
    
    Key<T> getID();

    void performPreRunActivities( List<Tools> globalTools,
                                  List<Tools> localTools,
                                  GlobalInspectionContext context);
    void performPostRunActivities( List<InspectionToolWrapper> inspections,  GlobalInspectionContext context);

    void cleanup();
}
