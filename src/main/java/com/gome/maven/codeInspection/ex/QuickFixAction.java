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

package com.gome.maven.codeInspection.ex;

import com.gome.maven.codeInsight.FileModificationService;
import com.gome.maven.codeInspection.CommonProblemDescriptor;
import com.gome.maven.codeInspection.InspectionManager;
import com.gome.maven.codeInspection.ProblemDescriptor;
import com.gome.maven.codeInspection.reference.RefElement;
import com.gome.maven.codeInspection.reference.RefEntity;
import com.gome.maven.codeInspection.reference.RefManagerImpl;
import com.gome.maven.codeInspection.ui.InspectionResultsView;
import com.gome.maven.codeInspection.ui.InspectionTree;
import com.gome.maven.icons.AllIcons;
import com.gome.maven.openapi.actionSystem.AnAction;
import com.gome.maven.openapi.actionSystem.AnActionEvent;
import com.gome.maven.openapi.actionSystem.CustomShortcutSet;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.command.CommandProcessor;
import com.gome.maven.openapi.progress.ProgressIndicator;
import com.gome.maven.openapi.progress.ProgressManager;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.vfs.ReadonlyStatusHandler;
import com.gome.maven.openapi.vfs.VfsUtilCore;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.psi.PsiDocumentManager;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.psi.presentation.java.SymbolPresentationUtil;
import com.gome.maven.util.SequentialModalProgressTask;
import com.gome.maven.util.SequentialTask;
import gnu.trove.THashSet;

import javax.swing.*;
import java.util.*;

/**
 * @author max
 */
public class QuickFixAction extends AnAction {
    public static final QuickFixAction[] EMPTY = new QuickFixAction[0];
    protected final InspectionToolWrapper myToolWrapper;

    public static InspectionResultsView getInvoker(AnActionEvent e) {
        return InspectionResultsView.DATA_KEY.getData(e.getDataContext());
    }

    protected QuickFixAction(String text,  InspectionToolWrapper toolWrapper) {
        this(text, AllIcons.Actions.CreateFromUsage, null, toolWrapper);
    }

    protected QuickFixAction(String text, Icon icon, KeyStroke keyStroke,  InspectionToolWrapper toolWrapper) {
        super(text, null, icon);
        myToolWrapper = toolWrapper;
        if (keyStroke != null) {
            registerCustomShortcutSet(new CustomShortcutSet(keyStroke), null);
        }
    }

    @Override
    public void update(AnActionEvent e) {
        final InspectionResultsView view = getInvoker(e);
        if (view == null) {
            e.getPresentation().setEnabled(false);
            return;
        }

        e.getPresentation().setVisible(false);
        e.getPresentation().setEnabled(false);

        final InspectionTree tree = view.getTree();
        final InspectionToolWrapper toolWrapper = tree.getSelectedToolWrapper();
        if (!view.isSingleToolInSelection() || toolWrapper != myToolWrapper) {
            return;
        }

        if (!isProblemDescriptorsAcceptable() && tree.getSelectedElements().length > 0 ||
                isProblemDescriptorsAcceptable() && tree.getSelectedDescriptors().length > 0) {
            e.getPresentation().setVisible(true);
            e.getPresentation().setEnabled(true);
        }
    }

    protected boolean isProblemDescriptorsAcceptable() {
        return false;
    }

    public String getText(RefEntity where) {
        return getTemplatePresentation().getText();
    }

    @Override
    public void actionPerformed(final AnActionEvent e) {
        final InspectionResultsView view = getInvoker(e);
        final InspectionTree tree = view.getTree();
        if (isProblemDescriptorsAcceptable()) {
            final CommonProblemDescriptor[] descriptors = tree.getSelectedDescriptors();
            if (descriptors.length > 0) {
                doApplyFix(view.getProject(), descriptors, tree.getContext());
                return;
            }
        }

        doApplyFix(getSelectedElements(e), view);
    }


    protected void applyFix( Project project,
                             GlobalInspectionContextImpl context,
                             CommonProblemDescriptor[] descriptors,
                             Set<PsiElement> ignoredElements) {
    }

    private void doApplyFix( final Project project,
                             final CommonProblemDescriptor[] descriptors,
                             final GlobalInspectionContextImpl context) {
        final Set<VirtualFile> readOnlyFiles = new THashSet<VirtualFile>();
        for (CommonProblemDescriptor descriptor : descriptors) {
            final PsiElement psiElement = descriptor instanceof ProblemDescriptor ? ((ProblemDescriptor)descriptor).getPsiElement() : null;
            if (psiElement != null && !psiElement.isWritable()) {
                readOnlyFiles.add(psiElement.getContainingFile().getVirtualFile());
            }
        }

        if (!FileModificationService.getInstance().prepareVirtualFilesForWrite(project, readOnlyFiles)) return;

        final RefManagerImpl refManager = (RefManagerImpl)context.getRefManager();

        final boolean initial = refManager.isInProcess();

        refManager.inspectionReadActionFinished();

        try {
            final Set<PsiElement> ignoredElements = new HashSet<PsiElement>();

            CommandProcessor.getInstance().executeCommand(project, new Runnable() {
                @Override
                public void run() {
                    CommandProcessor.getInstance().markCurrentCommandAsGlobal(project);
                    ApplicationManager.getApplication().runWriteAction(new Runnable() {
                        @Override
                        public void run() {
                            final SequentialModalProgressTask progressTask =
                                    new SequentialModalProgressTask(project, getTemplatePresentation().getText(), false);
                            progressTask.setMinIterationTime(200);
                            progressTask.setTask(new PerformFixesTask(project, descriptors, ignoredElements, progressTask, context));
                            ProgressManager.getInstance().run(progressTask);
                        }
                    });
                }
            }, getTemplatePresentation().getText(), null);

            refreshViews(project, ignoredElements, myToolWrapper);
        }
        finally { //to make offline view lazy
            if (initial) refManager.inspectionReadActionStarted();
        }
    }

    public void doApplyFix( final RefEntity[] refElements,  InspectionResultsView view) {
        final RefManagerImpl refManager = (RefManagerImpl)view.getGlobalInspectionContext().getRefManager();

        final boolean initial = refManager.isInProcess();

        refManager.inspectionReadActionFinished();

        try {
            final boolean[] refreshNeeded = {false};
            if (refElements.length > 0) {
                final Project project = refElements[0].getRefManager().getProject();
                CommandProcessor.getInstance().executeCommand(project, new Runnable() {
                    @Override
                    public void run() {
                        CommandProcessor.getInstance().markCurrentCommandAsGlobal(project);
                        ApplicationManager.getApplication().runWriteAction(new Runnable() {
                            @Override
                            public void run() {
                                refreshNeeded[0] = applyFix(refElements);
                            }
                        });
                    }
                }, getTemplatePresentation().getText(), null);
            }
            if (refreshNeeded[0]) {
                refreshViews(view.getProject(), refElements, myToolWrapper);
            }
        }
        finally {  //to make offline view lazy
            if (initial) refManager.inspectionReadActionStarted();
        }
    }

    public static void removeElements( RefEntity[] refElements,  Project project,  InspectionToolWrapper toolWrapper) {
        refreshViews(project, refElements, toolWrapper);
        final ArrayList<RefElement> deletedRefs = new ArrayList<RefElement>(1);
        for (RefEntity refElement : refElements) {
            if (!(refElement instanceof RefElement)) continue;
            refElement.getRefManager().removeRefElement((RefElement)refElement, deletedRefs);
        }
    }

    private static Set<VirtualFile> getReadOnlyFiles( RefEntity[] refElements) {
        Set<VirtualFile> readOnlyFiles = new THashSet<VirtualFile>();
        for (RefEntity refElement : refElements) {
            PsiElement psiElement = refElement instanceof RefElement ? ((RefElement)refElement).getElement() : null;
            if (psiElement == null || psiElement.getContainingFile() == null) continue;
            readOnlyFiles.add(psiElement.getContainingFile().getVirtualFile());
        }
        return readOnlyFiles;
    }

    private static RefEntity[] getSelectedElements(AnActionEvent e) {
        final InspectionResultsView invoker = getInvoker(e);
        if (invoker == null) return new RefElement[0];
        List<RefEntity> selection = new ArrayList<RefEntity>(Arrays.asList(invoker.getTree().getSelectedElements()));
        PsiDocumentManager.getInstance(invoker.getProject()).commitAllDocuments();
        Collections.sort(selection, new Comparator<RefEntity>() {
            @Override
            public int compare(RefEntity o1, RefEntity o2) {
                if (o1 instanceof RefElement && o2 instanceof RefElement) {
                    RefElement r1 = (RefElement)o1;
                    RefElement r2 = (RefElement)o2;
                    final PsiElement element1 = r1.getElement();
                    final PsiElement element2 = r2.getElement();
                    final PsiFile containingFile1 = element1.getContainingFile();
                    final PsiFile containingFile2 = element2.getContainingFile();
                    if (containingFile1 == containingFile2) {
                        int i1 = element1.getTextOffset();
                        int i2 = element2.getTextOffset();
                        if (i1 < i2) {
                            return 1;
                        } else if (i1 > i2){
                            return -1;
                        }
                        return 0;
                    }
                    return containingFile1.getName().compareTo(containingFile2.getName());
                }
                if (o1 instanceof RefElement) {
                    return 1;
                }
                if (o2 instanceof RefElement) {
                    return -1;
                }
                return o1.getName().compareTo(o2.getName());
            }
        });

        return selection.toArray(new RefEntity[selection.size()]);
    }

    private static void refreshViews( Project project,  Set<PsiElement> selectedElements,  InspectionToolWrapper toolWrapper) {
        InspectionManagerEx managerEx = (InspectionManagerEx)InspectionManager.getInstance(project);
        final Set<GlobalInspectionContextImpl> runningContexts = managerEx.getRunningContexts();
        for (GlobalInspectionContextImpl context : runningContexts) {
            for (PsiElement element : selectedElements) {
                context.ignoreElement(toolWrapper.getTool(), element);
            }
            context.refreshViews();
        }
    }

    private static void refreshViews( Project project,  RefEntity[] refElements,  InspectionToolWrapper toolWrapper) {
        final Set<PsiElement> ignoredElements = new HashSet<PsiElement>();
        for (RefEntity element : refElements) {
            final PsiElement psiElement = element instanceof RefElement ? ((RefElement)element).getElement() : null;
            if (psiElement != null && psiElement.isValid()) {
                ignoredElements.add(psiElement);
            }
        }
        refreshViews(project, ignoredElements, toolWrapper);
    }

    /**
     * @return true if immediate UI update needed.
     */
    protected boolean applyFix( RefEntity[] refElements) {
        Set<VirtualFile> readOnlyFiles = getReadOnlyFiles(refElements);
        if (!readOnlyFiles.isEmpty()) {
            final Project project = refElements[0].getRefManager().getProject();
            final ReadonlyStatusHandler.OperationStatus operationStatus = ReadonlyStatusHandler.getInstance(project).ensureFilesWritable(
                    VfsUtilCore.toVirtualFileArray(readOnlyFiles));
            if (operationStatus.hasReadonlyFiles()) return false;
        }
        return true;
    }

    private class PerformFixesTask implements SequentialTask {
        
        private final Project myProject;
        private final CommonProblemDescriptor[] myDescriptors;
        
        private final Set<PsiElement> myIgnoredElements;
        private final SequentialModalProgressTask myTask;
         private final GlobalInspectionContextImpl myContext;
        private int myCount = 0;

        public PerformFixesTask( Project project,
                                 CommonProblemDescriptor[] descriptors,
                                 Set<PsiElement> ignoredElements,
                                 SequentialModalProgressTask task,
                                 GlobalInspectionContextImpl context) {
            myProject = project;
            myDescriptors = descriptors;
            myIgnoredElements = ignoredElements;
            myTask = task;
            myContext = context;
        }

        @Override
        public void prepare() {
        }

        @Override
        public boolean isDone() {
            return myCount > myDescriptors.length - 1;
        }

        @Override
        public boolean iteration() {
            final CommonProblemDescriptor descriptor = myDescriptors[myCount++];
            ProgressIndicator indicator = myTask.getIndicator();
            if (indicator != null) {
                indicator.setFraction((double)myCount / myDescriptors.length);
                if (descriptor instanceof ProblemDescriptor) {
                    final PsiElement psiElement = ((ProblemDescriptor)descriptor).getPsiElement();
                    if (psiElement != null) {
                        indicator.setText("Processing " + SymbolPresentationUtil.getSymbolPresentableText(psiElement));
                    }
                }
            }
            applyFix(myProject, myContext, new CommonProblemDescriptor[]{descriptor}, myIgnoredElements);
            return isDone();
        }

        @Override
        public void stop() {
        }
    }
}
