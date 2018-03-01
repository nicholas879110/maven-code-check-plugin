package com.gome.maven.openapi.vcs.actions;

import com.gome.maven.openapi.actionSystem.AnActionEvent;
import com.gome.maven.openapi.fileTypes.FileType;
import com.gome.maven.openapi.localVcs.UpToDateLineNumberProvider;
import com.gome.maven.openapi.project.DumbAware;
import com.gome.maven.openapi.vcs.AbstractVcs;
import com.gome.maven.openapi.vcs.FilePath;
import com.gome.maven.openapi.vcs.FilePathImpl;
import com.gome.maven.openapi.vcs.annotate.FileAnnotation;
import com.gome.maven.openapi.vcs.annotate.LineNumberListener;
import com.gome.maven.openapi.vcs.history.VcsFileRevision;
import com.gome.maven.openapi.vcs.history.VcsFileRevisionEx;
import com.gome.maven.openapi.vcs.vfs.VcsFileSystem;
import com.gome.maven.openapi.vcs.vfs.VcsVirtualFile;
import com.gome.maven.openapi.vfs.VirtualFile;

import javax.swing.*;
import java.util.List;

abstract class AnnotateRevisionAction extends AnnotateRevisionActionBase implements DumbAware, LineNumberListener {
    private final UpToDateLineNumberProvider myGetUpToDateLineNumber;

     private final FileAnnotation myAnnotation;
     private final AbstractVcs myVcs;

    private int currentLine;

    public AnnotateRevisionAction( String text,  String description,  Icon icon,
                                   UpToDateLineNumberProvider getUpToDateLineNumber,
                                   FileAnnotation annotation,  AbstractVcs vcs) {
        super(text, description, icon);
        myGetUpToDateLineNumber = getUpToDateLineNumber;
        myAnnotation = annotation;
        myVcs = vcs;
    }

    @Override
    public void update( AnActionEvent e) {
        if (getRevisions() == null) {
            e.getPresentation().setEnabledAndVisible(false);
            return;
        }
        e.getPresentation().setVisible(true);

        super.update(e);
    }

    
    protected abstract List<VcsFileRevision> getRevisions();

    
    protected AbstractVcs getVcs( AnActionEvent e) {
        return myVcs;
    }

    
    @Override
    protected VirtualFile getFile( AnActionEvent e) {
        VcsFileRevision revision = getFileRevision(e);
        if (revision == null) return null;

        final FileType currentFileType = myAnnotation.getFile().getFileType();
        FilePath filePath =
                (revision instanceof VcsFileRevisionEx ? ((VcsFileRevisionEx)revision).getPath() : new FilePathImpl(myAnnotation.getFile()));
        return new VcsVirtualFile(filePath.getPath(), revision, VcsFileSystem.getInstance()) {
            
            @Override
            public FileType getFileType() {
                FileType type = super.getFileType();
                return type.isBinary() ? currentFileType : type;
            }
        };
    }

    
    @Override
    protected VcsFileRevision getFileRevision( AnActionEvent e) {
        List<VcsFileRevision> revisions = getRevisions();
        assert getRevisions() != null;

        if (currentLine < 0) return null;
        int corrected = myGetUpToDateLineNumber.getLineNumber(currentLine);

        if (corrected < 0 || corrected >= revisions.size()) return null;
        return revisions.get(corrected);
    }

    @Override
    public void consume(Integer integer) {
        currentLine = integer;
    }
}
