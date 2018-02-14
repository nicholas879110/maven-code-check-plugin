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
package com.gome.maven.openapi.project;

/**
 * A marker interface for the things that are allowed to run in dumb mode (when indices are in background update).
 * Implementors must take care of handling and/or not calling non-DumbAware parts of system.
 * <p/>
 * Known implementors are:
 * <li> {@link com.gome.maven.openapi.actionSystem.AnAction}s
 * <li> {@link com.gome.maven.openapi.fileEditor.FileEditorProvider}s
 * <li> post-startup activities ({@link com.gome.maven.openapi.startup.StartupManager#registerPostStartupActivity(Runnable)})
 * <li> Stacktrace {@link com.gome.maven.execution.filters.Filter}s
 * <li> {@link com.gome.maven.ide.SelectInTarget}s
 * <li> {@link com.gome.maven.ide.IconProvider}s
 * <li> {@link com.gome.maven.codeInsight.completion.CompletionContributor}s
 * <li> {@link com.gome.maven.lang.annotation.Annotator}s
 * <li> {@link com.gome.maven.codeInsight.daemon.LineMarkerProvider}s
 * <li> {@link com.gome.maven.codeHighlighting.TextEditorHighlightingPass}es
 * <li> {@link com.gome.maven.codeInspection.LocalInspectionTool}s
 * <li> {@link com.gome.maven.openapi.wm.ToolWindowFactory}s
 * <li> {@link com.gome.maven.lang.injection.MultiHostInjector}s
 *
 * @author peter
 * @see com.gome.maven.openapi.project.DumbService
 * @see com.gome.maven.openapi.project.DumbAwareRunnable
 * @see PossiblyDumbAware
 */
@SuppressWarnings("JavadocReference")
public interface DumbAware {
}
