package com.gome.maven.diagnostic;

import com.gome.maven.openapi.diagnostic.Attachment;
import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.fileEditor.FileDocumentManager;
import com.gome.maven.openapi.fileEditor.impl.LoadTextUtil;
import com.gome.maven.openapi.util.io.FileUtil;
import com.gome.maven.openapi.vfs.VirtualFile;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;

/**
 * @author yole
 */
public class AttachmentFactory {
    private static final String ERROR_MESSAGE_PATTERN = "[[[Can't get file contents: {0}]]]";

    public static Attachment createAttachment(Document document) {
        VirtualFile file = FileDocumentManager.getInstance().getFile(document);
        return new Attachment(file != null ? file.getPath() : "unknown.txt", document.getText());
    }

    public static Attachment createAttachment( VirtualFile file) {
        return new Attachment(file.getPresentableUrl(), getBytes(file),
                file.getFileType().isBinary() ? "File is binary" : LoadTextUtil.loadText(file).toString());
    }

    public static Attachment createAttachment( File file, boolean isBinary) {
        byte[] bytes = getBytes(file);
        return new Attachment(file.getPath(), bytes, isBinary ? "File is binary" : new String(bytes));
    }

    private static byte[] getBytes(File file) {
        try {
            return FileUtil.loadFileBytes(file);
        }
        catch (IOException e) {
            return Attachment.getBytes(MessageFormat.format(ERROR_MESSAGE_PATTERN, e.getMessage()));
        }
    }

    private static byte[] getBytes(VirtualFile file) {
        try {
            return file.contentsToByteArray();
        }
        catch (IOException e) {
            return Attachment.getBytes(MessageFormat.format(ERROR_MESSAGE_PATTERN, e.getMessage()));
        }
    }
}
