package com.gome.maven.psi.impl.source.resolve.reference.impl.providers;

import com.gome.maven.psi.PsiFileSystemItem;

public interface FileReferenceResolver {
    
    PsiFileSystemItem resolveFileReference( FileReference reference,  String name);
}