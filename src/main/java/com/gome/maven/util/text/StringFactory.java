package com.gome.maven.util.text;

import com.gome.maven.util.ReflectionUtil;
import sun.reflect.ConstructorAccessor;

import java.lang.reflect.Constructor;

/**
 * @author zhangliewei
 * @date 2017/12/29 17:19
 * @opyright(c) gome inc Gome Co.,LTD
 */
public class StringFactory {
    // String(char[], boolean). Works since JDK1.7, earlier JDKs have too slow reflection anyway
    private static final ConstructorAccessor ourConstructorAccessor;

    static {
        ConstructorAccessor constructorAccessor = null;
        try {
            Constructor<String> newC = String.class.getDeclaredConstructor(char[].class, boolean.class);
            constructorAccessor = ReflectionUtil.getConstructorAccessor(newC);
        }
        catch (Exception ignored) {
        }
        ourConstructorAccessor = constructorAccessor;
    }


    /**
     * @return new instance of String which backed by 'chars' array.
     *
     * CAUTION. EXTREMELY DANGEROUS.
     * DO NOT USE THIS METHOD UNLESS YOU ARE TOO DESPERATE
     */
    public static String createShared( char[] chars) {
        if (ourConstructorAccessor != null) {
            return ReflectionUtil.createInstanceViaConstructorAccessor(ourConstructorAccessor, chars, Boolean.TRUE);
        }
        return new String(chars);
    }
}

