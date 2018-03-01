package com.gome.maven.openapi.vcs.actions;

import com.gome.maven.icons.AllIcons;
import com.gome.maven.openapi.localVcs.UpToDateLineNumberProvider;
import com.gome.maven.openapi.vcs.AbstractVcs;
import com.gome.maven.openapi.vcs.annotate.FileAnnotation;
import com.gome.maven.openapi.vcs.history.VcsFileRevision;
import com.gome.maven.openapi.vcs.history.VcsRevisionNumber;
import com.gome.maven.util.containers.HashMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class AnnotatePreviousRevisionAction extends AnnotateRevisionAction {
     private final List<VcsFileRevision> myRevisions;

    public AnnotatePreviousRevisionAction( UpToDateLineNumberProvider getUpToDateLineNumber,
                                           FileAnnotation annotation,  AbstractVcs vcs) {
        super("Annotate Previous Revision", "Annotate successor of selected revision in new tab", AllIcons.Actions.Annotate,
                getUpToDateLineNumber, annotation, vcs);
        List<VcsFileRevision> revisions = annotation.getRevisions();
        if (revisions == null) {
            myRevisions = null;
            return;
        }

        Map<VcsRevisionNumber, VcsFileRevision> map = new HashMap<VcsRevisionNumber, VcsFileRevision>();
        for (int i = 0; i < revisions.size(); i++) {
            VcsFileRevision revision = revisions.get(i);
            VcsFileRevision previousRevision = i + 1 < revisions.size() ? revisions.get(i + 1) : null;
            map.put(revision.getRevisionNumber(), previousRevision);
        }

        myRevisions = new ArrayList<VcsFileRevision>(annotation.getLineCount());
        for (int i = 0; i < annotation.getLineCount(); i++) {
            myRevisions.add(map.get(annotation.getLineRevisionNumber(i)));
        }
    }

    @Override
    
    public List<VcsFileRevision> getRevisions() {
        return myRevisions;
    }
}
