package com.gome.maven.openapi.vcs.actions;

import com.gome.maven.openapi.actionSystem.AnAction;
import com.gome.maven.openapi.actionSystem.AnActionEvent;
import com.gome.maven.openapi.actionSystem.CommonDataKeys;
import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.openapi.progress.ProgressIndicator;
import com.gome.maven.openapi.progress.ProgressManager;
import com.gome.maven.openapi.progress.Task;
import com.gome.maven.openapi.util.Ref;
import com.gome.maven.openapi.vcs.*;
import com.gome.maven.openapi.vcs.annotate.AnnotationProvider;
import com.gome.maven.openapi.vcs.annotate.FileAnnotation;
import com.gome.maven.openapi.vcs.changes.BackgroundFromStartOption;
import com.gome.maven.openapi.vcs.history.VcsFileRevision;
import com.gome.maven.openapi.vcs.impl.BackgroundableActionEnabledHandler;
import com.gome.maven.openapi.vcs.impl.ProjectLevelVcsManagerImpl;
import com.gome.maven.openapi.vcs.impl.VcsBackgroundableActions;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.util.diff.Diff;
import com.gome.maven.util.diff.FilesTooBigForDiffException;

import javax.swing.*;

public abstract class AnnotateRevisionActionBase extends AnAction {
    public AnnotateRevisionActionBase( String text,  String description,  Icon icon) {
        super(text, description, icon);
    }

    
    protected abstract AbstractVcs getVcs( AnActionEvent e);

    
    protected abstract VirtualFile getFile( AnActionEvent e);

    
    protected abstract VcsFileRevision getFileRevision( AnActionEvent e);

    public void update( AnActionEvent e) {
        e.getPresentation().setEnabled(isEnabled(e));
    }

    public boolean isEnabled( AnActionEvent e) {
        if (e.getProject() == null) return false;

        VcsFileRevision fileRevision = getFileRevision(e);
        if (fileRevision == null) return false;

        VirtualFile file = getFile(e);
        if (file == null || file.isDirectory() || file.getFileType().isBinary()) return false;

        AbstractVcs vcs = getVcs(e);
        if (vcs == null) return false;

        AnnotationProvider provider = vcs.getCachingAnnotationProvider();
        if (provider == null || !provider.isAnnotationValid(fileRevision)) return false;

        final ProjectLevelVcsManagerImpl plVcsManager = (ProjectLevelVcsManagerImpl)ProjectLevelVcsManager.getInstance(vcs.getProject());
        if (plVcsManager.getBackgroundableActionHandler(VcsBackgroundableActions.ANNOTATE).isInProgress(key(file))) return false;

        return true;
    }

    @Override
    public void actionPerformed( final AnActionEvent e) {
        final VcsFileRevision fileRevision = getFileRevision(e);
        final VirtualFile file = getFile(e);
        final AbstractVcs vcs = getVcs(e);
        assert vcs != null;
        assert file != null;
        assert fileRevision != null;

        final Editor editor = e.getData(CommonDataKeys.EDITOR);
        final CharSequence oldContent = editor == null ? null : editor.getDocument().getImmutableCharSequence();
        final int oldLine = editor == null ? 0 : editor.getCaretModel().getLogicalPosition().line;

        final AnnotationProvider annotationProvider = vcs.getCachingAnnotationProvider();
        assert annotationProvider != null;

        final Ref<FileAnnotation> fileAnnotationRef = new Ref<FileAnnotation>();
        final Ref<Integer> newLineRef = new Ref<Integer>();
        final Ref<VcsException> exceptionRef = new Ref<VcsException>();

        final ProjectLevelVcsManagerImpl plVcsManager = (ProjectLevelVcsManagerImpl)ProjectLevelVcsManager.getInstance(vcs.getProject());
        final BackgroundableActionEnabledHandler handler = plVcsManager.getBackgroundableActionHandler(VcsBackgroundableActions.ANNOTATE);
        handler.register(key(file));

        ProgressManager.getInstance().run(new Task.Backgroundable(vcs.getProject(), VcsBundle.message("retrieving.annotations"), true,
                BackgroundFromStartOption.getInstance()) {
            public void run( ProgressIndicator indicator) {
                try {
                    FileAnnotation fileAnnotation = annotationProvider.annotate(file, fileRevision);

                    int newLine = oldLine;
                    if (oldContent != null) {
                        String content = fileAnnotation.getAnnotatedContent();
                        try {
                            newLine = Diff.translateLine(oldContent, content, oldLine, true);
                        }
                        catch (FilesTooBigForDiffException ignore) {
                        }
                    }

                    fileAnnotationRef.set(fileAnnotation);
                    newLineRef.set(newLine);
                }
                catch (VcsException e) {
                    exceptionRef.set(e);
                }
            }

            @Override
            public void onCancel() {
                onSuccess();
            }

            @Override
            public void onSuccess() {
                handler.completed(key(file));

                if (!exceptionRef.isNull()) {
                    AbstractVcsHelper.getInstance(myProject).showError(exceptionRef.get(), VcsBundle.message("operation.name.annotate"));
                }
                if (fileAnnotationRef.isNull()) return;

                AbstractVcsHelper.getInstance(myProject).showAnnotation(fileAnnotationRef.get(), file, vcs, newLineRef.get());
            }
        });
    }

    
    private static String key( VirtualFile vf) {
        return vf.getPath();
    }
}
