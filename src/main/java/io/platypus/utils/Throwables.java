package io.platypus.utils;

/**
 * Mimics Guava Preconditions
 *
 * @author rui.figueira
 */
public class Throwables {
    public static RuntimeException propagate(Throwable throwable) {
        propagateIfPossible(Preconditions.checkNotNull(throwable));
        throw new RuntimeException(throwable);
    }

    public static void propagateIfPossible(Throwable throwable) {
        propagateIfInstanceOf(throwable, Error.class);
        propagateIfInstanceOf(throwable, RuntimeException.class);
    }

    public static <X extends Throwable> void propagateIfInstanceOf(Throwable throwable, Class<X> declaredType) throws X {
        if (throwable != null && declaredType.isInstance(throwable))
            throw (declaredType.cast(throwable));
    }

}
