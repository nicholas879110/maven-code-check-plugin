package gnu.trove;

import com.gome.maven.openapi.util.text.StringUtil;

/**
 * @author zhangliewei
 * @date 2018/1/2 10:50
 * @opyright(c) gome inc Gome Co.,LTD
 */
public class CaseInsensitiveStringHashingStrategy implements TObjectHashingStrategy<String> {
    public static final CaseInsensitiveStringHashingStrategy INSTANCE = new CaseInsensitiveStringHashingStrategy();

    @Override
    public int computeHashCode(final String s) {
        return StringUtil.stringHashCodeInsensitive(s);
    }

    @Override
    public boolean equals(final String s1, final String s2) {
        return s1.equalsIgnoreCase(s2);
    }
}