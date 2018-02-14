package com.gome.maven.openapi.util.text;

import com.gome.maven.openapi.util.SystemInfo;
import com.gome.maven.util.containers.ContainerUtil;
import gnu.trove.CaseInsensitiveStringHashingStrategy;
import gnu.trove.TObjectHashingStrategy;

/**
 * @author zhangliewei
 * @date 2017/12/29 17:25
 * @opyright(c) gome inc Gome Co.,LTD
 */
public class FilePathHashingStrategy {
    private FilePathHashingStrategy() { }

    public static TObjectHashingStrategy<String> create() {
        return create(SystemInfo.isFileSystemCaseSensitive);
    }

    public static TObjectHashingStrategy<String> create(boolean caseSensitive) {
        return caseSensitive ? ContainerUtil.<String>canonicalStrategy() : CaseInsensitiveStringHashingStrategy.INSTANCE;
    }
}
