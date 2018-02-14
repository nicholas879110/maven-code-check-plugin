//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.gome.maven.openapi.components;

import com.gome.maven.openapi.util.Getter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface State {
    String name();

    /** @deprecated */
    @Deprecated
    RoamingType roamingType() default RoamingType.PER_USER;

    /** @deprecated */
    Storage[] storages();

    Class<? extends StateStorageChooser> storageChooser() default StateStorageChooser.class;

    boolean reloadable() default true;

    boolean defaultStateAsResource() default false;

    String additionalExportFile() default "";

    Class<? extends State.NameGetter> presentableName() default State.NameGetter.class;

    public abstract static class NameGetter implements Getter<String> {
        public NameGetter() {
        }
    }
}
