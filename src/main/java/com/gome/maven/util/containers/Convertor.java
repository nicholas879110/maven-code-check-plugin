package com.gome.maven.util.containers;

/**
 * @author zhangliewei
 * @date 2018/1/2 11:43
 * @opyright(c) gome inc Gome Co.,LTD
 */
public interface Convertor<Src, Dst> {
    Convertor.IntoSelf SELF = new Convertor.IntoSelf();

    Dst convert(Src var1);

    public static class IntoSelf<Src> implements Convertor<Src, Src> {
        public IntoSelf() {
        }

        public Src convert(Src o) {
            return o;
        }
    }
}
