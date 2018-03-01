package com.gome.maven.codeInsight.editorActions.enter;

import com.gome.maven.openapi.actionSystem.DataContext;
import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.openapi.editor.actionSystem.EditorActionHandler;
import com.gome.maven.openapi.util.Ref;
import com.gome.maven.psi.PsiFile;

/**
 * @author Denis Zhdanov
 * @since 5/30/11 2:23 PM
 */
public class EnterHandlerDelegateAdapter implements EnterHandlerDelegate {

    @Override
    public Result preprocessEnter( PsiFile file,
                                   Editor editor,
                                   Ref<Integer> caretOffset,
                                   Ref<Integer> caretAdvance,
                                   DataContext dataContext,
                                  EditorActionHandler originalHandler)
    {
        return Result.Continue;
    }

    @Override
    public Result postProcessEnter( PsiFile file,  Editor editor,  DataContext dataContext) {
        return Result.Continue;
    }
}
