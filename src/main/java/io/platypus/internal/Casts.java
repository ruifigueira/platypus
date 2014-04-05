package io.platypus.internal;

public class Casts {

    @SuppressWarnings("unchecked")
    public static <V> V unsafeCast(Object obj) {
        return (V) obj;
    }
}
