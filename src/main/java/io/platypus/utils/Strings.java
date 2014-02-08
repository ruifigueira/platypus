package io.platypus.utils;

import java.util.Collection;

public class Strings {

    public static String join(Collection<?> objs, String separator) {
        return join(objs.toArray(new Object[objs.size()]), separator);
    }

    public static String join(Object[] objs, String separator) {
        StringBuilder buf = new StringBuilder();

        if (objs != null && objs.length > 0) {
            buf.append(objs[0]);
        }

        for (int i = 1; i < objs.length; i++) {
            buf.append(separator);
            buf.append(objs[i]);
        }

        return buf.toString();
    }
}
