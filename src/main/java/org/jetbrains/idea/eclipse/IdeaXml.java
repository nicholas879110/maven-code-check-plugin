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
package org.jetbrains.idea.eclipse;


public interface IdeaXml {
     String COMPONENT_TAG = "component";
     String NAME_ATTR = "name";
     String NEW_MODULE_ROOT_MANAGER_VALUE = "NewModuleRootManager";
     String ORDER_ENTRY_TAG = "orderEntry";
     String TYPE_ATTR = "type";
     String EXPORTED_ATTR = "exported";
     String SOURCE_FOLDER_TYPE = "sourceFolder";
     String SOURCE_FOLDER_TAG = "sourceFolder";
     String CONTENT_ENTRY_TAG = "contentEntry";
     String TEST_FOLDER_TAG = "testFolder";
     String PACKAGE_PREFIX_TAG = "packagePrefix";
     String PACKAGE_PREFIX_VALUE_ATTR = "value";
     String EXCLUDE_FOLDER_TAG = "excludeFolder";
     String FOR_TESTS_ATTR = "forTests";
     String TRUE_VALUE = "true";
     String FALSE_VALUE = "false";
     String CONTENT_TAG = "content";
     String MODULE_LIBRARY_TYPE = "module-library";
     String LIBRARY_TAG = "library";
     String ROOT_TAG = "root";
     String CLASSES_TAG = "CLASSES";
     String SOURCES_TAG = "SOURCES";
     String JAVADOC_TAG = "JAVADOC";
     String JAR_DIR = "jarDirectory";
     String URL_ATTR = "url";
     String LIBRARY_TYPE = "library";
     String LEVEL_ATTR = "level";
     String APPLICATION_LEVEL = "application";
     String PROJECT_LEVEL = "project";
     String ECLIPSE_LIBRARY = "ECLIPSE";
     String MODULE_TYPE = "module";
     String MODULE_NAME_ATTR = "module-name";
     String JDK_TYPE = "jdk";
     String INHERITED_JDK_TYPE = "inheritedJdk";
     String JDK_NAME_ATTR = "jdkName";
     String JDK_TYPE_ATTR = "jdkType";
     String JAVA_SDK_TYPE = "JavaSDK";
     String MODULE_DIR_MACRO = "$MODULE_DIR$";
     String FILE_PREFIX = "file://";
     String JAR_PREFIX = "jar://";
     String JAR_SUFFIX = "!/";
     String JAR_EXT = ".jar";
     String ZIP_EXT = ".zip";
     String EXCLUDE_OUTPUT_TAG = "exclude-output";
     String LANGUAGE_LEVEL_ATTR = "LANGUAGE_LEVEL";
     String INHERIT_COMPILER_OUTPUT_ATTR = "inherit-compiler-output";
     String OUTPUT_TAG = "output";
     String OUTPUT_TEST_TAG = "output-test";
     String IS_TEST_SOURCE_ATTR = "isTestSource";
     String ORDER_ENTRY_PROPERTIES_TAG = "orderEntryProperties";
     String IPR_EXT = ".ipr";
     String PROJECT_MODULE_MANAGER_VALUE = "ProjectModuleManager";
     String MODULES_TAG = "modules";
     String MODULE_TAG = "module";
     String FILEURL_ATTR = "fileurl";
     String FILEPATH_ATTR = "filepath";
     String PROJECT_DIR_MACRO = "$PROJECT_DIR$";
     String PROJECT_PREFIX = FILE_PREFIX + PROJECT_DIR_MACRO + "/";
     String USED_PATH_MACROS_TAG = "UsedPathMacros";
     String MACRO_TAG = "macro";
     String UNNAMED_PROJECT = "unnamed";
     String MODULE_CONTEXT = "module";
     String PROJECT_CONTEXT = "project";
     String CLASSPATH_CONTEXT = "classpath";
     String TEMPLATE_CONTEXT = "template";
     String EXCLUDE_OUTPUT = "exclude-output";
     String IML_EXT = ".iml";
    String JUNIT = "junit";
}
