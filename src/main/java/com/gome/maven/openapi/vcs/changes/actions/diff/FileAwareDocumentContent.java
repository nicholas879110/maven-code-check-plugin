package com.gome.maven.openapi.vcs.changes.actions.diff;

import com.gome.maven.diff.contents.DiffContent;
import com.gome.maven.diff.contents.DocumentContentImpl;
import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.editor.EditorFactory;
import com.gome.maven.openapi.fileEditor.OpenFileDescriptor;
import com.gome.maven.openapi.fileTypes.FileType;
import com.gome.maven.openapi.fileTypes.FileTypes;
import com.gome.maven.openapi.fileTypes.PlainTextFileType;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.openapi.vcs.FilePath;
import com.gome.maven.openapi.vfs.LocalFileSystem;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.util.LineSeparator;

import java.nio.charset.Charset;

public class FileAwareDocumentContent extends DocumentContentImpl {
     private final Project myProject;
     private final VirtualFile myLocalFile;

    public FileAwareDocumentContent( Project project,
                                     Document document,
                                     FileType fileType,
                                     VirtualFile localFile,
                                     LineSeparator separator,
                                     Charset charset) {
        super(document, fileType, localFile, separator, charset);
        myProject = project;
        myLocalFile = localFile;
    }

    public OpenFileDescriptor getOpenFileDescriptor(int offset) {
        if (myProject == null || myLocalFile == null) return null;
        return new OpenFileDescriptor(myProject, myLocalFile, offset);
    }

    
    public static DiffContent create( Project project,
                                      String content,
                                      FilePath path) {
        VirtualFile localFile = LocalFileSystem.getInstance().findFileByPath(path.getPath());
        FileType fileType = localFile != null ? localFile.getFileType() : path.getFileType();
        Charset charset = localFile != null ? localFile.getCharset() : path.getCharset(project);
        return create(project, content, fileType, localFile, charset);
    }

    
    public static DiffContent create( Project project,
                                      String content,
                                      VirtualFile file) {
        FileType fileType = file.getFileType();
        Charset charset = file.getCharset();
        return create(project, content, fileType, file, charset);
    }

    
    private static DiffContent create( Project project,
                                       String content,
                                       FileType fileType,
                                       VirtualFile file,
                                       Charset charset) {
        LineSeparator separator = StringUtil.detectSeparators(content);
        Document document = EditorFactory.getInstance().createDocument(StringUtil.convertLineSeparators(content));
        document.setReadOnly(true);
        if (FileTypes.UNKNOWN.equals(fileType)) fileType = PlainTextFileType.INSTANCE;
        return new FileAwareDocumentContent(project, document, fileType, file, separator, charset);
    }
}
