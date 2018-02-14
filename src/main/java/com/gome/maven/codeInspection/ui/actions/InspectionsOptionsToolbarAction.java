package com.gome.maven.codeInspection.ui.actions;

import com.gome.maven.CommonBundle;
import com.gome.maven.codeInsight.daemon.DaemonCodeAnalyzer;
import com.gome.maven.codeInsight.daemon.HighlightDisplayKey;
import com.gome.maven.codeInspection.InspectionsBundle;
import com.gome.maven.codeInspection.ModifiableModel;
import com.gome.maven.codeInspection.actions.RunInspectionIntention;
import com.gome.maven.codeInspection.ex.DisableInspectionToolAction;
import com.gome.maven.codeInspection.ex.InspectionProfileImpl;
import com.gome.maven.codeInspection.ex.InspectionToolWrapper;
import com.gome.maven.codeInspection.reference.RefElement;
import com.gome.maven.codeInspection.reference.RefEntity;
import com.gome.maven.codeInspection.ui.InspectionResultsView;
import com.gome.maven.codeInspection.ui.InspectionTree;
import com.gome.maven.icons.AllIcons;
import com.gome.maven.openapi.actionSystem.AnAction;
import com.gome.maven.openapi.actionSystem.AnActionEvent;
import com.gome.maven.openapi.actionSystem.DataContext;
import com.gome.maven.openapi.actionSystem.DefaultActionGroup;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.ui.Messages;
import com.gome.maven.openapi.ui.popup.JBPopupFactory;
import com.gome.maven.openapi.ui.popup.ListPopup;
import com.gome.maven.profile.codeInspection.InspectionProjectProfileManager;
import com.gome.maven.psi.PsiElement;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * User: anna
 * Date: 11-Jan-2006
 */
public class InspectionsOptionsToolbarAction extends AnAction {
    private final InspectionResultsView myView;

    public InspectionsOptionsToolbarAction(final InspectionResultsView view) {
        super(getToolOptions(null), getToolOptions(null), AllIcons.General.InspectionsOff);
        myView = view;
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        final DefaultActionGroup options = new DefaultActionGroup();
        final List<AnAction> actions = createActions();
        for (AnAction action : actions) {
            options.add(action);
        }
        final DataContext dataContext = e.getDataContext();
        final ListPopup popup = JBPopupFactory.getInstance()
                .createActionGroupPopup(getSelectedToolWrapper().getDisplayName(), options, dataContext,
                        JBPopupFactory.ActionSelectionAid.SPEEDSEARCH, false);
        InspectionResultsView.showPopup(e, popup);
    }

    
    private InspectionToolWrapper getSelectedToolWrapper() {
        return myView.getTree().getSelectedToolWrapper();
    }

    @Override
    public void update(AnActionEvent e) {
        if (!myView.isSingleToolInSelection()) {
            e.getPresentation().setEnabled(false);
            return;
        }
        InspectionToolWrapper toolWrapper = getSelectedToolWrapper();
        assert toolWrapper != null;
        final HighlightDisplayKey key = HighlightDisplayKey.find(toolWrapper.getShortName());
        if (key == null) {
            e.getPresentation().setEnabled(false);
        }
        e.getPresentation().setEnabled(true);
        final String text = getToolOptions(toolWrapper);
        e.getPresentation().setText(text);
        e.getPresentation().setDescription(text);
    }

    
    private static String getToolOptions( final InspectionToolWrapper toolWrapper) {
        return InspectionsBundle.message("inspections.view.options.title", toolWrapper != null ? toolWrapper.getDisplayName() : "");
    }

    public List<AnAction> createActions() {
        final List<AnAction> result = new ArrayList<AnAction>();
        final InspectionTree tree = myView.getTree();
        final InspectionToolWrapper toolWrapper = tree.getSelectedToolWrapper();
        if (toolWrapper == null) return result;
        final HighlightDisplayKey key = HighlightDisplayKey.find(toolWrapper.getShortName());
        if (key == null) return result;

        result.add(new DisableInspectionAction(key));

        result.add(new AnAction(InspectionsBundle.message("run.inspection.on.file.intention.text")) {
            @Override
            public void actionPerformed(final AnActionEvent e) {
                final PsiElement psiElement = getPsiElement(tree);
                assert psiElement != null;
                new RunInspectionIntention(toolWrapper).invoke(myView.getProject(), null, psiElement.getContainingFile());
            }

            @Override
            public void update(AnActionEvent e) {
                e.getPresentation().setEnabled(getPsiElement(tree) != null);
            }

            
            private PsiElement getPsiElement(InspectionTree tree) {
                final RefEntity[] selectedElements = tree.getSelectedElements();

                final PsiElement psiElement;
                if (selectedElements.length > 0 && selectedElements[0] instanceof RefElement) {
                    psiElement = ((RefElement)selectedElements[0]).getElement();
                }
                else {
                    psiElement = null;
                }
                return psiElement;
            }
        });

        result.add(new SuppressActionWrapper(myView.getProject(), toolWrapper, tree.getSelectionPaths()));


        return result;
    }

    private class DisableInspectionAction extends AnAction {
        private final HighlightDisplayKey myKey;

        public DisableInspectionAction(final HighlightDisplayKey key) {
            super(DisableInspectionToolAction.NAME);
            myKey = key;
        }

        @Override
        public void actionPerformed(final AnActionEvent e) {
            try {
                if (myView.isProfileDefined()) {
                    final ModifiableModel model = myView.getCurrentProfile().getModifiableModel();
                    model.disableTool(myKey.toString(), myView.getProject());
                    model.commit();
                    myView.updateCurrentProfile();
                } else {
                    final RefEntity[] selectedElements = myView.getTree().getSelectedElements();
                    final Set<PsiElement> files = new HashSet<PsiElement>();
                    final Project project = myView.getProject();
                    final InspectionProjectProfileManager profileManager = InspectionProjectProfileManager.getInstance(project);
                    for (RefEntity selectedElement : selectedElements) {
                        if (selectedElement instanceof RefElement) {
                            final PsiElement element = ((RefElement)selectedElement).getElement();
                            files.add(element);
                        }
                    }
                    ModifiableModel model = ((InspectionProfileImpl)profileManager.getProjectProfileImpl()).getModifiableModel();
                    for (PsiElement element : files) {
                        model.disableTool(myKey.toString(), element);
                    }
                    model.commit();
                    DaemonCodeAnalyzer.getInstance(project).restart();
                }
            }
            catch (IOException e1) {
                Messages.showErrorDialog(myView.getProject(), e1.getMessage(), CommonBundle.getErrorTitle());
            }
        }
    }
}
