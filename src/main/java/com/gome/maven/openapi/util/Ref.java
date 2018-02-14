//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.gome.maven.openapi.util;


public class Ref<T> {
    private T myValue;

    public Ref() {
    }

    public Ref( T value) {
        this.myValue = value;
    }

    public boolean isNull() {
        return this.myValue == null;
    }

    public T get() {
        return this.myValue;
    }

    public void set( T value) {
        this.myValue = value;
    }

    public boolean setIfNull( T value) {
        if (this.myValue == null) {
            this.myValue = value;
            return true;
        } else {
            return false;
        }
    }


    public static <T> Ref<T> create() {
        return new Ref();
    }

    public static <T> Ref<T> create( T value) {
        return new Ref(value);
    }

    public String toString() {
        return String.valueOf(this.myValue);
    }
}
