//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.gome.maven.openapi.components;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Storage {
    
    String id() default "default";

    /** @deprecated */
    @Deprecated
    boolean isDefault() default true;

    
    String file() default "";

    StorageScheme scheme() default StorageScheme.DEFAULT;

    boolean deprecated() default false;

    RoamingType roamingType() default RoamingType.PER_USER;

    Class<? extends StateStorage> storageClass() default StateStorage.class;

    Class<? extends StateSplitter> stateSplitter() default StateSplitterEx.class;
}
