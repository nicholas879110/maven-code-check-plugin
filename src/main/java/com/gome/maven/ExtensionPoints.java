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
package com.gome.maven;

import com.gome.maven.openapi.diagnostic.ErrorReportSubmitter;
import com.gome.maven.openapi.extensions.ExtensionPointName;

/**
 * Extension points provided by IDEA core are listed here.
 */
public interface ExtensionPoints extends ToolExtensionPoints {
    /**
     * This extension point should be used instead of former application-components, project-components, module-components.
     * Extension declaration sample is as follows:
     * <pre>
     * &lt;extensions xmlns="com.gome.maven"&gt;
     *   &lt;component area="IDEA_PROJECT"&gt;
     *     &lt;implementation&gt;my.plugin.pagckage.MyProjectComponent&lt;/implementation&gt;
     *   &lt;/component&gt;
     * &lt;/extensions&gt;
     * </pre>
     * <p/>
     * Possible registration areas are IDEA_PROJECT, MODULE_PROJECT which stand for ProjectComponent and ModuleComponent correspondingly.
     * If area attribute is omitted the component will be registered in root area which corresponds to ApplicationComponent.
     */
     String COMPONENT = "com.gome.maven.component";

    /**
     * This extension point allows a plugin vendor to provide the user ability to report fatal errors (aka exceptions) that happened in
     * their plugin code.
     * Extension declaration sample is as follows:
     * <pre>
     * &lt;extensions xmlns="com.gome.maven"&gt;
     *   &lt;errorHandler implementation="my.plugin.package.MyErrorHandler"/&gt;
     * &lt;/extensions&gt;
     * </pre>
     * my.plugin.package.MyErrorHandler class must implement {@link com.gome.maven.openapi.diagnostic.ErrorReportSubmitter} abstract class.
     */
     String ERROR_HANDLER = "com.gome.maven.errorHandler";

    ExtensionPointName<ErrorReportSubmitter> ERROR_HANDLER_EP = ExtensionPointName.create(ERROR_HANDLER);

    /**
     * This extension point allows a plugin vendor to provide patches to junit run/debug configurations
     * Extension declaration sample is as follows:
     * <pre>
     * &lt;extensions xmlns="com.gome.maven"&gt;
     *   &lt;junitPatcher implementation="my.plugin.package.MyJUnitPatcher"/&gt;
     * &lt;/extensions&gt;
     * </pre>
     * my.plugin.package.MyJUnitPatcher class must implement {@link com.gome.maven.execution.JUnitPatcher} abstract class.
     */
    @SuppressWarnings("JavadocReference")  String JUNIT_PATCHER = "com.gome.maven.junitPatcher";

    /**
     * This extensions allows to run custom [command-line] application based on IDEA platform
     * <pre>
     * &lt;extensions xmlns="com.gome.maven"&gt;
     *   &lt;applicationStarter implementation="my.plugin.package.MyApplicationStarter"/&gt;
     * &lt;/extensions&gt;
     * </pre>
     * my.plugin.package.MyApplicationStarter class must implement {@link com.gome.maven.openapi.application.ApplicationStarter} interface.
     */
     String APPLICATION_STARTER = "com.gome.maven.appStarter";

     String ANT_BUILD_GEN = "com.gome.maven.antBuildGen";

    /**
     * Ant custom compiler extension point
     */
     String ANT_CUSTOM_COMPILER = "com.gome.maven.antCustomCompiler";
}
