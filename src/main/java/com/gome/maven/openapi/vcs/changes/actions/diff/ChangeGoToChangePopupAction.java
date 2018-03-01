package com.gome.maven.openapi.vcs.changes.actions.diff;

import com.gome.maven.openapi.actionSystem.AnActionEvent;
import com.gome.maven.openapi.actionSystem.DefaultActionGroup;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.project.ProjectManager;
import com.gome.maven.openapi.ui.popup.JBPopup;
import com.gome.maven.openapi.ui.popup.JBPopupFactory;
import com.gome.maven.openapi.util.Ref;
import com.gome.maven.diff.actions.impl.GoToChangePopupBuilder;
import com.gome.maven.diff.chains.DiffRequestChain;
import com.gome.maven.openapi.vcs.changes.Change;
import com.gome.maven.openapi.vcs.changes.ui.ChangesBrowser;
import com.gome.maven.openapi.wm.IdeFocusManager;
import com.gome.maven.util.Consumer;

import javax.swing.*;
import java.util.Collections;
import java.util.List;

public abstract class ChangeGoToChangePopupAction<Chain extends DiffRequestChain>
        extends GoToChangePopupBuilder.BaseGoToChangePopupAction<Chain>{
    public ChangeGoToChangePopupAction( Chain chain,  Consumer onSelected) {
        super(chain, onSelected);
    }

    
    @Override
    protected JBPopup createPopup( AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) project = ProjectManager.getInstance().getDefaultProject();

        Ref<JBPopup> popup = new Ref<JBPopup>();
        ChangesBrowser cb = new MyChangesBrowser(project, getChanges(), getCurrentSelection(), popup);

        popup.set(JBPopupFactory.getInstance()
                .createComponentPopupBuilder(cb, cb.getPreferredFocusedComponent())
                .setResizable(true)
                .setModalContext(false)
                .setFocusable(true)
                .setRequestFocus(true)
                .setCancelOnWindowDeactivation(true)
                .setCancelOnOtherWindowOpen(true)
                .setMovable(true)
                .setCancelKeyEnabled(true)
                .setCancelOnClickOutside(true)
                .createPopup());

        return popup.get();
    }

    //
    // Abstract
    //

    protected abstract int findSelectedStep( Change change);

    
    protected abstract List<Change> getChanges();

    
    protected abstract Change getCurrentSelection();

    //
    // Helpers
    //

    private class MyChangesBrowser extends ChangesBrowser implements Runnable {
         Ref<JBPopup> myPopup;

        public MyChangesBrowser( Project project,
                                 List<Change> changes,
                                 Change currentChange,
                                 Ref<JBPopup> popup) {
            super(project, null, changes, null, false, false, null, MyUseCase.LOCAL_CHANGES, null);
            setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            setChangesToDisplay(changes);
            if (currentChange != null) select(Collections.singletonList(currentChange));

            myPopup = popup;
        }

        @Override
        protected void buildToolBar(DefaultActionGroup toolBarGroup) {
            // remove diff action
        }

        
        @Override
        protected Runnable getDoubleClickHandler() {
            return this;
        }

        @Override
        public void run() {
            Change change = getSelectedChanges().get(0);
            final int index = findSelectedStep(change);
            myPopup.get().cancel();
            IdeFocusManager.getInstance(myProject).doWhenFocusSettlesDown(new Runnable() {
                @Override
                public void run() {
                    //noinspection unchecked
                    myOnSelected.consume(index);
                }
            });
        }
    }
}
