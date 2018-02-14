package com.gome.maven.psi.stubs;

import com.gome.maven.diagnostic.LogMessageEx;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.application.ModalityState;
import com.gome.maven.openapi.diagnostic.Attachment;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.psi.PsiCompiledElement;
import com.gome.maven.psi.impl.DebugUtil;
import com.gome.maven.psi.impl.source.PsiFileWithStubSupport;
import com.gome.maven.util.indexing.FileBasedIndex;

/**
 * Author: dmitrylomov
 */
public class StubProcessingHelper extends StubProcessingHelperBase {
    private final FileBasedIndex myFileBasedIndex;

    public StubProcessingHelper(FileBasedIndex fileBasedIndex) {
        myFileBasedIndex = fileBasedIndex;
    }

    @Override
    protected void onInternalError(final VirtualFile file) {
        // requestReindex() may want to acquire write lock (for indices not requiring content loading)
        // thus, because here we are under read lock, need to use invoke later
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                myFileBasedIndex.requestReindex(file);
            }
        }, ModalityState.NON_MODAL);
    }


    @Override
    protected Object stubTreeAndIndexDoNotMatch(StubTree stubTree, PsiFileWithStubSupport psiFile) {
        final VirtualFile virtualFile = psiFile.getVirtualFile();
        StubTree stubTreeFromIndex = (StubTree)StubTreeLoader.getInstance().readFromVFile(psiFile.getProject(), virtualFile);
        String details = "Please report the problem to JetBrains with the file attached";
        details += "\npsiFile" + psiFile;
        details += "\npsiFile.class" + psiFile.getClass();
        details += "\npsiFile.lang" + psiFile.getLanguage();
        details += "\n" + DebugUtil.currentStackTrace();
        String fileText = psiFile instanceof PsiCompiledElement ? "compiled" : psiFile.getText();
        return LogMessageEx.createEvent("PSI and index do not match",
                details,
                new Attachment(virtualFile != null ? virtualFile.getPath() + "_file.txt" : "vFile.txt", fileText),
                new Attachment("stubTree.txt", ((PsiFileStubImpl)stubTree.getRoot()).printTree()),
                new Attachment("stubTreeFromIndex.txt", stubTreeFromIndex == null
                        ? "null"
                        : ((PsiFileStubImpl)stubTreeFromIndex.getRoot()).printTree()));
    }
}
