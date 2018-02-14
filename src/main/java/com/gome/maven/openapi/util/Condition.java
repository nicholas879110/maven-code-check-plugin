package com.gome.maven.openapi.util;

public interface Condition<T> {

    boolean value(T t);

    Condition<Object> NOT_NULL = new Condition<Object>() {
        @Override
        public boolean value(final Object object) {
            return object != null;
        }

        @Override
        public String toString() {
            return "Condition.NOT_NULL";
        }
    };


    Condition TRUE = new Condition() {
        @Override
        public boolean value(final Object object) {
            return true;
        }

        @Override
        public String toString() {
            return "Condition.TRUE";
        }
    };

    Condition FALSE = new Condition() {
        @Override
        public boolean value(final Object object) {
            return false;
        }

        @Override
        public String toString() {
            return "Condition.FALSE";
        }
    };
}
