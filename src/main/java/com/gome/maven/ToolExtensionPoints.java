/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.gome.maven;


public interface ToolExtensionPoints {
     String INVALID_PROPERTY_KEY_INSPECTION_TOOL = "com.gome.maven.invalidPropertyKeyInspectionTool";
     String I18N_INSPECTION_TOOL = "com.gome.maven.i18nInspectionTool";
     String JAVA15_INSPECTION_TOOL = "com.gome.maven.java15InspectionTool";


     String INSPECTIONS_GRAPH_ANNOTATOR = "com.gome.maven.refGraphAnnotator";

     String DEAD_CODE_TOOL = "com.gome.maven.deadCode";

     String JAVADOC_LOCAL = "com.gome.maven.javaDocNotNecessary";

     String VISIBLITY_TOOL = "com.gome.maven.visibility";

     String EMPTY_METHOD_TOOL = "com.gome.maven.canBeEmpty";

}
