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
package com.gome.maven.diff.actions.impl;

import com.gome.maven.diff.chains.DiffRequestChain;
import com.gome.maven.diff.chains.DiffRequestProducer;
import com.gome.maven.openapi.actionSystem.AnAction;
import com.gome.maven.openapi.actionSystem.AnActionEvent;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.ui.popup.JBPopup;
import com.gome.maven.openapi.ui.popup.JBPopupFactory;
import com.gome.maven.openapi.ui.popup.PopupStep;
import com.gome.maven.openapi.ui.popup.util.BaseListPopupStep;
import com.gome.maven.ui.awt.RelativePoint;
import com.gome.maven.util.Consumer;

import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;

public class GoToChangePopupBuilder {
    public interface Chain extends DiffRequestChain {
        
        AnAction createGoToChangeAction( Consumer<Integer> onSelected);
    }

    
    public static AnAction create( DiffRequestChain chain,  Consumer<Integer> onSelected) {
        if (chain instanceof Chain) {
            return ((Chain)chain).createGoToChangeAction(onSelected);
        }
        return new SimpleGoToChangePopupAction(chain, onSelected);
    }

    public static abstract class BaseGoToChangePopupAction<Chain extends DiffRequestChain> extends GoToChangePopupAction {
         protected final Chain myChain;
         protected final Consumer<Integer> myOnSelected;

        public BaseGoToChangePopupAction( Chain chain,  Consumer<Integer> onSelected) {
            myChain = chain;
            myOnSelected = onSelected;
        }

        @Override
        public void update( AnActionEvent e) {
            if (myChain.getRequests().size() <= 1) {
                e.getPresentation().setEnabledAndVisible(false);
                return;
            }

            e.getPresentation().setEnabledAndVisible(true);
        }

        @Override
        public void actionPerformed( AnActionEvent e) {
            JBPopup popup = createPopup(e);

            InputEvent event = e.getInputEvent();
            if (event instanceof MouseEvent) {
                popup.show(new RelativePoint((MouseEvent)event));
            }
            else {
                popup.showInBestPositionFor(e.getDataContext());
            }
        }

        
        protected abstract JBPopup createPopup( AnActionEvent e);
    }

    private static class SimpleGoToChangePopupAction extends BaseGoToChangePopupAction {

        public SimpleGoToChangePopupAction( DiffRequestChain chain,  Consumer<Integer> onSelected) {
            super(chain, onSelected);
        }

        
        @Override
        protected JBPopup createPopup( AnActionEvent e) {
            return JBPopupFactory.getInstance().createListPopup(new MyListPopupStep(e.getProject()));
        }

        private class MyListPopupStep extends BaseListPopupStep<DiffRequestProducer> {
            private final Project myProject;

            public MyListPopupStep( Project project) {
                super("Go To Change", myChain.getRequests());
                setDefaultOptionIndex(myChain.getIndex());
                myProject = project;
            }

            
            @Override
            public String getTextFor(DiffRequestProducer value) {
                return value.getName();
            }

            @Override
            public boolean isSpeedSearchEnabled() {
                return true;
            }

            @Override
            public PopupStep onChosen(final DiffRequestProducer selectedValue, boolean finalChoice) {
                return doFinalStep(new Runnable() {
                    @Override
                    public void run() {
                        int index = myChain.getRequests().indexOf(selectedValue);
                        myOnSelected.consume(index);
                    }
                });
            }
        }
    }
}
