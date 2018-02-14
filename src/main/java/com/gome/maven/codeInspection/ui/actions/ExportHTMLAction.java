/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

package com.gome.maven.codeInspection.ui.actions;

import com.gome.maven.codeEditor.printing.ExportToHTMLSettings;
import com.gome.maven.codeInspection.InspectionApplication;
import com.gome.maven.codeInspection.InspectionsBundle;
import com.gome.maven.codeInspection.ex.GlobalInspectionContextImpl;
import com.gome.maven.codeInspection.ex.InspectionToolWrapper;
import com.gome.maven.codeInspection.ex.ScopeToolState;
import com.gome.maven.codeInspection.ex.Tools;
import com.gome.maven.codeInspection.export.ExportToHTMLDialog;
import com.gome.maven.codeInspection.export.HTMLExportFrameMaker;
import com.gome.maven.codeInspection.export.HTMLExportUtil;
import com.gome.maven.codeInspection.export.HTMLExporter;
import com.gome.maven.codeInspection.reference.RefEntity;
import com.gome.maven.codeInspection.reference.RefModule;
import com.gome.maven.codeInspection.ui.InspectionNode;
import com.gome.maven.codeInspection.ui.InspectionResultsView;
import com.gome.maven.codeInspection.ui.InspectionToolPresentation;
import com.gome.maven.codeInspection.ui.InspectionTreeNode;
import com.gome.maven.codeInspection.util.RefEntityAlphabeticalComparator;
import com.gome.maven.icons.AllIcons;
import com.gome.maven.ide.BrowserUtil;
import com.gome.maven.openapi.actionSystem.AnAction;
import com.gome.maven.openapi.actionSystem.AnActionEvent;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.application.PathManager;
import com.gome.maven.openapi.components.PathMacroManager;
import com.gome.maven.openapi.progress.ProcessCanceledException;
import com.gome.maven.openapi.progress.ProgressManager;
import com.gome.maven.openapi.project.DumbAware;
import com.gome.maven.openapi.ui.Messages;
import com.gome.maven.openapi.ui.popup.JBPopupFactory;
import com.gome.maven.openapi.ui.popup.ListPopup;
import com.gome.maven.openapi.ui.popup.PopupStep;
import com.gome.maven.openapi.ui.popup.util.BaseListPopupStep;
import com.gome.maven.openapi.util.Comparing;
import com.gome.maven.openapi.util.JDOMUtil;
import com.gome.maven.psi.codeStyle.CodeStyleSettingsManager;
import com.gome.maven.util.ThrowableRunnable;
import com.gome.maven.util.ui.tree.TreeUtil;
import org.jdom.Document;
import org.jdom.Element;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * User: anna
 * Date: 11-Jan-2006
 */
public class ExportHTMLAction extends AnAction implements DumbAware {
    private final InspectionResultsView myView;
     private static final String PROBLEMS = "problems";
     private static final String HTML = "HTML";
     private static final String XML = "XML";

    public ExportHTMLAction(final InspectionResultsView view) {
        super(InspectionsBundle.message("inspection.action.export.html"), null, AllIcons.Actions.Export);
        myView = view;
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        final ListPopup popup = JBPopupFactory.getInstance().createListPopup(
                new BaseListPopupStep<String>(InspectionsBundle.message("inspection.action.export.popup.title"), new String[]{HTML, XML}) {
                    @Override
                    public PopupStep onChosen(final String selectedValue, final boolean finalChoice) {
                        exportHTML(Comparing.strEqual(selectedValue, HTML));
                        return PopupStep.FINAL_CHOICE;
                    }
                });
        InspectionResultsView.showPopup(e, popup);
    }

    private void exportHTML(final boolean exportToHTML) {
        ExportToHTMLDialog exportToHTMLDialog = new ExportToHTMLDialog(myView.getProject(), exportToHTML);
        final ExportToHTMLSettings exportToHTMLSettings = ExportToHTMLSettings.getInstance(myView.getProject());
        if (exportToHTMLSettings.OUTPUT_DIRECTORY == null) {
            exportToHTMLSettings.OUTPUT_DIRECTORY = PathManager.getHomePath() + File.separator + "inspections";
        }
        exportToHTMLDialog.reset();
        if (!exportToHTMLDialog.showAndGet()) {
            return;
        }
        exportToHTMLDialog.apply();

        final String outputDirectoryName = exportToHTMLSettings.OUTPUT_DIRECTORY;
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                final Runnable exportRunnable = new Runnable() {
                    @Override
                    public void run() {
                        ApplicationManager.getApplication().runReadAction(new Runnable() {
                            @Override
                            public void run() {
                                if (!exportToHTML) {
                                    dupm2XML(outputDirectoryName);
                                }
                                else {
                                    final HTMLExportFrameMaker maker = new HTMLExportFrameMaker(outputDirectoryName, myView.getProject());
                                    maker.start();
                                    try {
                                        final InspectionTreeNode root = myView.getTree().getRoot();
                                        TreeUtil.traverse(root, new TreeUtil.Traverse() {
                                            @Override
                                            public boolean accept(final Object node) {
                                                if (node instanceof InspectionNode) {
                                                    exportHTML(maker, (InspectionNode)node);
                                                }
                                                return true;
                                            }
                                        });
                                    }
                                    catch (ProcessCanceledException e) {
                                        // Do nothing here.
                                    }

                                    maker.done();
                                }
                            }
                        });
                    }
                };

                if (!ProgressManager.getInstance().runProcessWithProgressSynchronously(exportRunnable,
                        exportToHTML ? InspectionsBundle
                                .message("inspection.generating.html.progress.title")
                                : InspectionsBundle
                                .message("inspection.generating.xml.progress.title"), true,
                        myView.getProject())) {
                    return;
                }

                if (exportToHTML && exportToHTMLSettings.OPEN_IN_BROWSER) {
                    BrowserUtil.browse(new File(exportToHTMLSettings.OUTPUT_DIRECTORY, "index.html"));
                }
            }
        });
    }

    private void dupm2XML(final String outputDirectoryName) {
        try {
            new File(outputDirectoryName).mkdirs();
            final InspectionTreeNode root = myView.getTree().getRoot();
            final IOException[] ex = new IOException[1];
            TreeUtil.traverse(root, new TreeUtil.Traverse() {
                @Override
                public boolean accept(final Object node) {
                    if (node instanceof InspectionNode) {
                        InspectionNode toolNode = (InspectionNode)node;
                        Element problems = new Element(PROBLEMS);
                        InspectionToolWrapper toolWrapper = toolNode.getToolWrapper();

                        final Set<InspectionToolWrapper> toolWrappers = getWorkedTools(toolNode);
                        for (InspectionToolWrapper wrapper : toolWrappers) {
                            InspectionToolPresentation presentation = myView.getGlobalInspectionContext().getPresentation(wrapper);
                            presentation.exportResults(problems);
                        }
                        PathMacroManager.getInstance(myView.getProject()).collapsePaths(problems);
                        try {
                            JDOMUtil.writeDocument(new Document(problems),
                                    outputDirectoryName + File.separator + toolWrapper.getShortName() + InspectionApplication.XML_EXTENSION,
                                    CodeStyleSettingsManager.getSettings(null).getLineSeparator());
                        }
                        catch (IOException e) {
                            ex[0] = e;
                        }
                    }
                    return true;
                }
            });
            if (ex[0] != null) {
                throw ex[0];
            }
            final Element element = new Element(InspectionApplication.INSPECTIONS_NODE);
            final String profileName = myView.getCurrentProfileName();
            if (profileName != null) {
                element.setAttribute(InspectionApplication.PROFILE, profileName);
            }
            JDOMUtil.writeDocument(new Document(element),
                    outputDirectoryName + File.separator + InspectionApplication.DESCRIPTIONS + InspectionApplication.XML_EXTENSION,
                    CodeStyleSettingsManager.getSettings(null).getLineSeparator());
        }
        catch (final IOException e) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    Messages.showErrorDialog(myView, e.getMessage());
                }
            });
        }
    }

    
    private Set<InspectionToolWrapper> getWorkedTools( InspectionNode node) {
        final Set<InspectionToolWrapper> result = new HashSet<InspectionToolWrapper>();
        final InspectionToolWrapper wrapper = node.getToolWrapper();
        if (myView.getCurrentProfileName() != null){
            result.add(wrapper);
            return result;
        }
        final String shortName = wrapper.getShortName();
        final GlobalInspectionContextImpl context = myView.getGlobalInspectionContext();
        final Tools tools = context.getTools().get(shortName);
        if (tools != null) {   //dummy entry points tool
            for (ScopeToolState state : tools.getTools()) {
                InspectionToolWrapper toolWrapper = state.getTool();
                result.add(toolWrapper);
            }
        }
        return result;
    }

    private void exportHTML(HTMLExportFrameMaker frameMaker, InspectionNode node) {
        final Set<InspectionToolWrapper> toolWrappers = getWorkedTools(node);
        final InspectionToolWrapper toolWrapper = node.getToolWrapper();

        final HTMLExporter exporter =
                new HTMLExporter(frameMaker.getRootFolder() + "/" + toolWrapper.getShortName(), myView.getGlobalInspectionContext().getPresentation(toolWrapper).getComposer());
        frameMaker.startInspection(toolWrapper);
        HTMLExportUtil.runExport(myView.getProject(), new ThrowableRunnable<IOException>() {
            @Override
            public void run() throws IOException {
                exportHTML(toolWrappers, exporter);
                exporter.generateReferencedPages();
            }
        });
    }

    @SuppressWarnings({"HardCodedStringLiteral"})
    private void exportHTML( Set<InspectionToolWrapper> toolWrappers, HTMLExporter exporter) throws IOException {
        StringBuffer packageIndex = new StringBuffer();
        packageIndex.append("<html><body>");

        final Map<String, Set<RefEntity>> content = new HashMap<String, Set<RefEntity>>();

        for (InspectionToolWrapper toolWrapper : toolWrappers) {
            InspectionToolPresentation presentation = myView.getGlobalInspectionContext().getPresentation(toolWrapper);
            final Map<String, Set<RefEntity>> toolContent = presentation.getContent();
            content.putAll(toolContent);
        }

        final Set<RefEntity> defaultPackageEntities = content.remove(null);
        if (defaultPackageEntities != null) {
            content.put("default package" , defaultPackageEntities);
        }

        List<String> packageNames = new ArrayList<String>(content.keySet());

        Collections.sort(packageNames);
        for (String packageName : packageNames) {
            appendPackageReference(packageIndex, packageName);
            List<RefEntity> packageContent = new ArrayList<RefEntity>(content.get(packageName));
            Collections.sort(packageContent, RefEntityAlphabeticalComparator.getInstance());
            StringBuffer contentIndex = new StringBuffer();
            contentIndex.append("<html><body>");
            for (RefEntity refElement : packageContent) {
                refElement = refElement.getRefManager().getRefinedElement(refElement);
                contentIndex.append("<a HREF=\"");
                contentIndex.append(exporter.getURL(refElement));
                contentIndex.append("\" target=\"elementFrame\">");
                contentIndex.append(refElement.getName());
                contentIndex.append("</a><br>");

                exporter.createPage(refElement);
            }

            contentIndex.append("</body></html>");
            HTMLExportUtil.writeFile(exporter.getRootFolder(), packageName + "-index.html", contentIndex, myView.getProject());
        }

        final Set<RefModule> modules = new HashSet<RefModule>();
        for (InspectionToolWrapper toolWrapper : toolWrappers) {
            InspectionToolPresentation presentation = myView.getGlobalInspectionContext().getPresentation(toolWrapper);
            final Set<RefModule> problems = presentation.getModuleProblems();
            modules.addAll(problems);
        }

        final List<RefModule> sortedModules = new ArrayList<RefModule>(modules);
        Collections.sort(sortedModules, RefEntityAlphabeticalComparator.getInstance());
        for (RefModule module : sortedModules) {
            appendPackageReference(packageIndex, module.getName());
            StringBuffer contentIndex = new StringBuffer();
            contentIndex.append("<html><body>");

            contentIndex.append("<a HREF=\"");
            contentIndex.append(exporter.getURL(module));
            contentIndex.append("\" target=\"elementFrame\">");
            contentIndex.append(module.getName());
            contentIndex.append("</a><br>");
            exporter.createPage(module);

            contentIndex.append("</body></html>");
            HTMLExportUtil.writeFile(exporter.getRootFolder(), module.getName() + "-index.html", contentIndex, myView.getProject());
        }

        packageIndex.append("</body></html>");

        HTMLExportUtil.writeFile(exporter.getRootFolder(), "index.html", packageIndex, myView.getProject());
    }

    @SuppressWarnings({"HardCodedStringLiteral"})
    private static void appendPackageReference(StringBuffer packageIndex, String packageName) {
        packageIndex.append("<a HREF=\"");
        packageIndex.append(packageName);
        packageIndex.append("-index.html\" target=\"packageFrame\">");
        packageIndex.append(packageName);
        packageIndex.append("</a><br>");
    }

}
