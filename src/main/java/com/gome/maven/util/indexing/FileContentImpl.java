/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.gome.maven.util.indexing;

import com.gome.maven.lang.Language;
import com.gome.maven.lang.LighterAST;
import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.fileEditor.FileDocumentManager;
import com.gome.maven.openapi.fileEditor.impl.LoadTextUtil;
import com.gome.maven.openapi.fileTypes.FileType;
import com.gome.maven.openapi.fileTypes.LanguageFileType;
import com.gome.maven.openapi.project.DefaultProjectFactory;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.Key;
import com.gome.maven.openapi.util.UserDataHolderBase;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.psi.LanguageSubstitutors;
import com.gome.maven.psi.PsiDocumentManager;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.psi.PsiFileFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

/**
 * @author nik
 */
public final class FileContentImpl extends UserDataHolderBase implements FileContent {
    private final VirtualFile myFile;
    private final String myFileName;
    private final FileType myFileType;
    private final Charset myCharset;
    private byte[] myContent;
    private CharSequence myContentAsText;
    private final long myStamp;
    private byte[] myHash;

    @Override
    public Project getProject() {
        return getUserData(IndexingDataKeys.PROJECT);
    }

    private static final Key<PsiFile> CACHED_PSI = Key.create("cached psi from content");

    /**
     * @return psiFile associated with the content. If the file was not set on FileContentCreation, it will be created on the spot
     */
    
    @Override
    public PsiFile getPsiFile() {
        PsiFile psi = getUserData(IndexingDataKeys.PSI_FILE);

        if (psi == null) {
            psi = getUserData(CACHED_PSI);
        }

        if (psi == null) {
            psi = createFileFromText(getContentAsText());
            psi.putUserData(IndexingDataKeys.VIRTUAL_FILE, getFile());
            putUserData(CACHED_PSI, psi);
        }
        return psi;
    }

    public  LighterAST getLighterASTForPsiDependentIndex() {
        LighterAST lighterAST = getUserData(IndexingDataKeys.LIGHTER_AST_NODE_KEY);
        if (lighterAST == null) {
            lighterAST = getPsiFileForPsiDependentIndex().getNode().getLighterAST();
            assert lighterAST != null;
            putUserData(IndexingDataKeys.LIGHTER_AST_NODE_KEY, lighterAST);
        }
        return lighterAST;
    }

    public PsiFile createFileFromText( CharSequence text) {
        Project project = getProject();
        if (project == null) {
            project = DefaultProjectFactory.getInstance().getDefaultProject();
        }
        final Language language = ((LanguageFileType)getFileTypeWithoutSubstitution()).getLanguage();
        final VirtualFile file = getFile();
        final Language substitutedLanguage = LanguageSubstitutors.INSTANCE.substituteLanguage(language, file, project);
        return PsiFileFactory.getInstance(project).createFileFromText(getFileName(), substitutedLanguage, text, false, false, true, file);
    }

    public static class IllegalDataException extends RuntimeException {
        public IllegalDataException(final String message) {
            super(message);
        }
    }

    public FileContentImpl( final VirtualFile file,  final CharSequence contentAsText, final Charset charset) {
        this(file, contentAsText, null, charset, -1);
    }

    public FileContentImpl( final VirtualFile file,  final CharSequence contentAsText, final Charset charset, long documentStamp) {
        this(file, contentAsText, null, charset, documentStamp);
    }

    public FileContentImpl( final VirtualFile file,  final byte[] content) {
        this(file, null, content, LoadTextUtil.detectCharsetAndSetBOM(file, content), -1);
    }

    public FileContentImpl( final VirtualFile file) {
        this(file, null, null, null, -1);
    }

    private FileContentImpl( VirtualFile file,
                            CharSequence contentAsText,
                            byte[] content,
                            Charset charset,
                            long stamp
    ) {
        myFile = file;
        myContentAsText = contentAsText;
        myContent = content;
        myCharset = charset;
        myFileType = file.getFileType();
        // remember name explicitly because the file could be renamed afterwards
        myFileName = file.getName();
        myStamp = stamp;
    }

    
    public FileType getSubstitutedFileType() {
        return SubstitutedFileType.substituteFileType(myFile, myFileType, getProject());
    }

    public static FileContent createByFile( VirtualFile file) {
        try {
            return new FileContentImpl(file, file.contentsToByteArray());
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public FileType getFileTypeWithoutSubstitution() {
        return myFileType;
    }

    
    @Override
    public FileType getFileType() {
        return getSubstitutedFileType();
    }

    
    @Override
    public VirtualFile getFile() {
        return myFile;
    }

    
    @Override
    public String getFileName() {
        return myFileName;
    }

    public Charset getCharset() {
        return myCharset;
    }

    public long getStamp() {
        return myStamp;
    }

    @Override
    public byte[] getContent() {
        if (myContent == null) {
            if (myContentAsText != null) {
                try {
                    myContent = myCharset != null ? myContentAsText.toString().getBytes(myCharset.name()) : myContentAsText.toString().getBytes();
                }
                catch (UnsupportedEncodingException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return myContent;
    }

    
    @Override
    public CharSequence getContentAsText() {
        if (myFileType.isBinary()) {
            throw new IllegalDataException("Cannot obtain text for binary file type : " + myFileType.getDescription());
        }
        final CharSequence content = getUserData(IndexingDataKeys.FILE_TEXT_CONTENT_KEY);
        if (content != null) {
            return content;
        }
        if (myContentAsText == null) {
            if (myContent != null) {
                myContentAsText = LoadTextUtil.getTextByBinaryPresentation(myContent, myCharset);
                myContent = null; // help gc, indices are expected to use bytes or chars but not both
            }
        }
        return myContentAsText;
    }

    @Override
    public String toString() {
        return myFileName;
    }

    public  byte[] getHash() {
        return myHash;
    }

    public void setHash(byte[] hash) {
        myHash = hash;
    }

    public PsiFile getPsiFileForPsiDependentIndex() {
        Document document = FileDocumentManager.getInstance().getCachedDocument(getFile());
        PsiFile psi = null;
        if (document != null) {
            PsiDocumentManager psiDocumentManager = PsiDocumentManager.getInstance(getProject());
            if (psiDocumentManager.isUncommited(document)) {
                PsiFile existingPsi = psiDocumentManager.getPsiFile(document);
                if(existingPsi != null) {
                    psi = existingPsi;
                }
            }
        }
        if (psi == null) {
            psi = getPsiFile();
        }
        return psi;
    }

}
