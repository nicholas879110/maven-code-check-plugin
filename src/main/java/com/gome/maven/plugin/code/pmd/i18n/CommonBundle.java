package com.gome.maven.plugin.code.pmd.i18n;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.ResourceBundle;

public class CommonBundle extends BundleBase {

    private CommonBundle() {
    }


    public static String message(ResourceBundle bundle, String key, Object... params) {
        return BundleBase.message(bundle, key, params);
    }
}
