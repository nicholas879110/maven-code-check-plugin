
package com.gome.maven.openapi.vcs.changes;

import com.gome.maven.openapi.vcs.FilePath;
import com.gome.maven.openapi.vcs.VcsException;
import com.gome.maven.openapi.vcs.history.VcsRevisionNumber;

/**
 * @author yole
 */
public class SimpleContentRevision implements ContentRevision {
    private final String myContent;
    private final FilePath myNewFilePath;
    private final String myRevision;

    public SimpleContentRevision(final String content, final FilePath newFilePath, final String revision) {
        myContent = content;
        myNewFilePath = newFilePath;
        myRevision = revision;
    }

    
    public String getContent() {
        return myContent;
    }

    
    public FilePath getFile() {
        return myNewFilePath;
    }

    
    public VcsRevisionNumber getRevisionNumber() {
        return new VcsRevisionNumber() {
            public String asString() {
                return myRevision;
            }

            public int compareTo(final VcsRevisionNumber o) {
                return 0;
            }
        };
    }
}