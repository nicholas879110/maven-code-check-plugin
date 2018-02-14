package com.gome.maven.ide.browsers;

import com.gome.maven.openapi.application.AccessToken;
import com.gome.maven.openapi.application.ReadAction;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.util.Url;

import java.util.Collection;

public abstract class OpenInBrowserRequest {
    private Collection<Url> result;
    protected PsiFile file;

    public OpenInBrowserRequest( PsiFile file) {
        this.file = file;
    }

    public OpenInBrowserRequest() {
    }

    
    public static OpenInBrowserRequest create( final PsiElement element) {
        PsiFile psiFile;
        AccessToken token = ReadAction.start();
        try {
            psiFile = element.isValid() ? element.getContainingFile() : null;
            if (psiFile == null || psiFile.getVirtualFile() == null) {
                return null;
            }
        }
        finally {
            token.finish();
        }

        return new OpenInBrowserRequest(psiFile) {
            
            @Override
            public PsiElement getElement() {
                return element;
            }
        };
    }

    
    public PsiFile getFile() {
        return file;
    }

    
    public VirtualFile getVirtualFile() {
        return file.getVirtualFile();
    }

    
    public Project getProject() {
        return file.getProject();
    }

    
    public abstract PsiElement getElement();

    public void setResult( Collection<Url> result) {
        this.result = result;
    }

    
    public Collection<Url> getResult() {
        return result;
    }
}