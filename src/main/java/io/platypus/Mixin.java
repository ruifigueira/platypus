package io.platypus;

import com.google.common.base.Preconditions;
import com.google.common.reflect.TypeToken;

public interface Mixin {

    public boolean is(Class<?> clazz);
    public boolean is(TypeToken<?> type);
    public <A> A as(Class<A> clazz);
    public <A> A as(TypeToken<A> type);

    public static class Impl implements Mixin {

        private final Object that;

        public Impl(Object that) {
            this.that = that == null ? this : that;
        }

        @Override
        public boolean is(Class<?> clazz) {
            Preconditions.checkNotNull(clazz);
            return clazz.isInstance(that);
        }

        @Override
        public boolean is(TypeToken<?> type) {
            Preconditions.checkNotNull(type);
            return is(type.getRawType());
        }

        @Override
        @SuppressWarnings("unchecked")
        public <A> A as(Class<A> clazz) {
            Preconditions.checkNotNull(clazz);
            Preconditions.checkState(clazz.isInstance(that), "that is not an instance of %s", clazz);
            return (A) that;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <A> A as(TypeToken<A> type) {
            Preconditions.checkNotNull(type);
            Class<?> clazz = type.getRawType();
            Preconditions.checkState(clazz.isInstance(that), "that is not an instance of %s", clazz);
            return (A) that;
        }
    }
}
