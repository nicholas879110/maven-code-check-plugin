package com.gome.maven.openapi.vcs.changes.actions.diff;

import com.gome.maven.openapi.actionSystem.AnAction;
import com.gome.maven.openapi.util.UserDataHolderBase;
import com.gome.maven.diff.actions.impl.GoToChangePopupBuilder;
import com.gome.maven.diff.chains.DiffRequestChain;
import com.gome.maven.openapi.vcs.changes.Change;
import com.gome.maven.util.Consumer;
import com.gome.maven.util.Function;
import com.gome.maven.util.containers.ContainerUtil;

import java.util.List;

public class ChangeDiffRequestChain extends UserDataHolderBase implements DiffRequestChain, GoToChangePopupBuilder.Chain {
     private final List<ChangeDiffRequestProducer> myRequests;
    private int myIndex;

    public ChangeDiffRequestChain( List<ChangeDiffRequestProducer> requests) {
        myRequests = requests;
    }

    
    @Override
    public List<? extends ChangeDiffRequestProducer> getRequests() {
        return myRequests;
    }

    @Override
    public int getIndex() {
        return myIndex;
    }

    @Override
    public void setIndex(int index) {
        assert index >= 0 && index < myRequests.size();
        myIndex = index;
    }

    
    @Override
    public AnAction createGoToChangeAction( Consumer<Integer> onSelected) {
        return new ChangeGoToChangePopupAction<ChangeDiffRequestChain>(this, onSelected) {
            @Override
            protected int findSelectedStep( Change change) {
                if (change == null) return -1;
                for (int i = 0; i < myRequests.size(); i++) {
                    Change c = myRequests.get(i).getChange();
                    if (c.equals(change)) return i;
                }
                return -1;
            }

            
            @Override
            protected List<Change> getChanges() {
                return ContainerUtil.mapNotNull(myChain.getRequests(), new Function<ChangeDiffRequestProducer, Change>() {
                    @Override
                    public Change fun(ChangeDiffRequestProducer presentable) {
                        return presentable.getChange();
                    }
                });
            }

            
            @Override
            protected Change getCurrentSelection() {
                return myChain.getRequests().get(myChain.getIndex()).getChange();
            }
        };
    }
}
