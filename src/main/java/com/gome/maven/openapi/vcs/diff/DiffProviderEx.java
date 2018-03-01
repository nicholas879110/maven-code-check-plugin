package com.gome.maven.openapi.vcs.diff;

import com.gome.maven.openapi.vcs.history.VcsRevisionNumber;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.util.containers.ContainerUtil;

import java.util.Map;

/**
 * @author peter
 */
public abstract class DiffProviderEx implements DiffProvider {
    public Map<VirtualFile, VcsRevisionNumber> getCurrentRevisions(Iterable<VirtualFile> files) {
        return getCurrentRevisions(files, this);
    }

    public static Map<VirtualFile, VcsRevisionNumber> getCurrentRevisions(Iterable<VirtualFile> file, DiffProvider provider) {
        Map<VirtualFile, VcsRevisionNumber> result = ContainerUtil.newHashMap();
        for (VirtualFile virtualFile : file) {
            result.put(virtualFile, provider.getCurrentRevision(virtualFile));
        }
        return result;
    }
}
