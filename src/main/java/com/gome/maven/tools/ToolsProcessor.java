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

package com.gome.maven.tools;

import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.components.PathMacroManager;
import com.gome.maven.openapi.options.BaseSchemeProcessor;
import com.gome.maven.openapi.util.InvalidDataException;
import com.gome.maven.openapi.util.WriteExternalException;
import com.gome.maven.openapi.util.text.StringUtil;
import org.jdom.Element;
import org.jdom.JDOMException;

import java.io.File;
import java.io.IOException;
import java.util.List;


abstract public class ToolsProcessor<T extends Tool> extends BaseSchemeProcessor<ToolsGroup<T>> {
     private static final String TOOL_SET = "toolSet";
     private static final String TOOL = "tool";
     private static final String ATTRIBUTE_NAME = "name";
     private static final String NAME = ATTRIBUTE_NAME;
     private static final String DESCRIPTION = "description";
     private static final String SHOW_IN_MAIN_MENU = "showInMainMenu";
     private static final String SHOW_IN_EDITOR = "showInEditor";
     private static final String SHOW_IN_PROJECT = "showInProject";
     private static final String SHOW_IN_SEARCH_POPUP = "showInSearchPopup";
     private static final String DISABLED = "disabled";
     private static final String USE_CONSOLE = "useConsole";
     private static final String SHOW_CONSOLE_ON_STDOUT = "showConsoleOnStdOut";
     private static final String SHOW_CONSOLE_ON_STDERR = "showConsoleOnStdErr";
     private static final String SYNCHRONIZE_AFTER_EXECUTION = "synchronizeAfterRun";
     private static final String EXEC = "exec";
     private static final String WORKING_DIRECTORY = "WORKING_DIRECTORY";
     private static final String COMMAND = "COMMAND";
     private static final String PARAMETERS = "PARAMETERS";
     private static final String FILTER = "filter";
     private static final String ELEMENT_OPTION = "option";
     private static final String ATTRIBUTE_VALUE = "value";

    
    @Override
    public ToolsGroup<T> readScheme( Element root) throws InvalidDataException, IOException, JDOMException {
        if (!TOOL_SET.equals(root.getName())) {
            throw new InvalidDataException();
        }

        String attrName = root.getAttributeValue(ATTRIBUTE_NAME);
        String groupName = StringUtil.isEmpty(attrName)? Tool.DEFAULT_GROUP_NAME : attrName;
        ToolsGroup<T> result = createToolsGroup(groupName);

        final PathMacroManager macroManager = PathMacroManager.getInstance(ApplicationManager.getApplication());

        for (Element element : (List<Element>)root.getChildren(TOOL)) {
            T tool = createTool();

            readToolAttributes(element, tool);

            Element exec = element.getChild(EXEC);
            if (exec != null) {
                for (final Object o1 : exec.getChildren(ELEMENT_OPTION)) {
                    Element optionElement = (Element)o1;

                    String name = optionElement.getAttributeValue(ATTRIBUTE_NAME);
                    String value = optionElement.getAttributeValue(ATTRIBUTE_VALUE);

                    if (WORKING_DIRECTORY.equals(name)) {
                        if (value != null) {
                            final String replace = macroManager.expandPath(value).replace('/', File.separatorChar);
                            tool.setWorkingDirectory(replace);
                        }
                    }
                    if (COMMAND.equals(name)) {
                        tool.setProgram(macroManager.expandPath(BaseToolManager.convertString(value)));
                    }
                    if (PARAMETERS.equals(name)) {
                        tool.setParameters(macroManager.expandPath(BaseToolManager.convertString(value)));
                    }
                }
            }

            for (final Object o2 : element.getChildren(FILTER)) {
                Element childNode = (Element)o2;

                FilterInfo filterInfo = new FilterInfo();
                filterInfo.readExternal(childNode);
                tool.addOutputFilter(filterInfo);
            }

            tool.setGroup(groupName);
            result.addElement(tool);
        }

        return result;
    }

    protected void readToolAttributes(Element element, T tool) {
        tool.setName(BaseToolManager.convertString(element.getAttributeValue(NAME)));
        tool.setDescription(BaseToolManager.convertString(element.getAttributeValue(DESCRIPTION)));
        tool.setShownInMainMenu(Boolean.valueOf(element.getAttributeValue(SHOW_IN_MAIN_MENU)).booleanValue());
        tool.setShownInEditor(Boolean.valueOf(element.getAttributeValue(SHOW_IN_EDITOR)).booleanValue());
        tool.setShownInProjectViews(Boolean.valueOf(element.getAttributeValue(SHOW_IN_PROJECT)).booleanValue());
        tool.setShownInSearchResultsPopup(Boolean.valueOf(element.getAttributeValue(SHOW_IN_SEARCH_POPUP)).booleanValue());
        tool.setEnabled(!Boolean.valueOf(element.getAttributeValue(DISABLED)).booleanValue());
        tool.setUseConsole(Boolean.valueOf(element.getAttributeValue(USE_CONSOLE)).booleanValue());
        tool.setShowConsoleOnStdOut(Boolean.valueOf(element.getAttributeValue(SHOW_CONSOLE_ON_STDOUT)).booleanValue());
        tool.setShowConsoleOnStdErr(Boolean.valueOf(element.getAttributeValue(SHOW_CONSOLE_ON_STDERR)).booleanValue());
        tool.setFilesSynchronizedAfterRun(Boolean.valueOf(element.getAttributeValue(SYNCHRONIZE_AFTER_EXECUTION)).booleanValue());
    }

    protected abstract ToolsGroup<T> createToolsGroup(String groupName);

    protected abstract T createTool();

    @Override
    public Element writeScheme( final ToolsGroup<T> scheme) throws WriteExternalException {
        Element groupElement = new Element(TOOL_SET);
        groupElement.setAttribute(ATTRIBUTE_NAME, scheme.getName());

        for (T tool : scheme.getElements()) {
            saveTool(tool, groupElement);
        }

        return groupElement;
    }

    private void saveTool(T tool, Element groupElement) {
        Element element = new Element(TOOL);
        if (tool.getName() != null) {
            element.setAttribute(NAME, tool.getName());
        }
        if (tool.getDescription() != null) {
            element.setAttribute(DESCRIPTION, tool.getDescription());
        }

        saveToolAttributes(tool, element);

        Element taskElement = new Element(EXEC);

        final PathMacroManager macroManager = PathMacroManager.getInstance(ApplicationManager.getApplication());

        Element option = new Element(ELEMENT_OPTION);
        taskElement.addContent(option);
        option.setAttribute(ATTRIBUTE_NAME, COMMAND);
        if (tool.getProgram() != null) {
            option.setAttribute(ATTRIBUTE_VALUE, macroManager.collapsePath(tool.getProgram()));
        }

        option = new Element(ELEMENT_OPTION);
        taskElement.addContent(option);
        option.setAttribute(ATTRIBUTE_NAME, PARAMETERS);
        if (tool.getParameters() != null) {
            option.setAttribute(ATTRIBUTE_VALUE, macroManager.collapsePath(tool.getParameters()));
        }

        option = new Element(ELEMENT_OPTION);
        taskElement.addContent(option);
        option.setAttribute(ATTRIBUTE_NAME, WORKING_DIRECTORY);
        if (tool.getWorkingDirectory() != null) {
            option.setAttribute(ATTRIBUTE_VALUE, macroManager.collapsePath(tool.getWorkingDirectory()).replace(File.separatorChar, '/'));
        }

        element.addContent(taskElement);

        FilterInfo[] filters = tool.getOutputFilters();
        for (FilterInfo filter : filters) {
            Element filterElement = new Element(FILTER);
            filter.writeExternal(filterElement);
            element.addContent(filterElement);
        }

        groupElement.addContent(element);
    }

    protected void saveToolAttributes(T tool, Element element) {
        element.setAttribute(SHOW_IN_MAIN_MENU, Boolean.toString(tool.isShownInMainMenu()));
        element.setAttribute(SHOW_IN_EDITOR, Boolean.toString(tool.isShownInEditor()));
        element.setAttribute(SHOW_IN_PROJECT, Boolean.toString(tool.isShownInProjectViews()));
        element.setAttribute(SHOW_IN_SEARCH_POPUP, Boolean.toString(tool.isShownInSearchResultsPopup()));
        element.setAttribute(DISABLED, Boolean.toString(!tool.isEnabled()));
        element.setAttribute(USE_CONSOLE, Boolean.toString(tool.isUseConsole()));
        element.setAttribute(SHOW_CONSOLE_ON_STDOUT, Boolean.toString(tool.isShowConsoleOnStdOut()));
        element.setAttribute(SHOW_CONSOLE_ON_STDERR, Boolean.toString(tool.isShowConsoleOnStdErr()));
        element.setAttribute(SYNCHRONIZE_AFTER_EXECUTION, Boolean.toString(tool.synchronizeAfterExecution()));
    }
}
