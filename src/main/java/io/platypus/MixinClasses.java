package io.platypus;

import io.platypus.internal.MixinClassImpl;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

import com.google.common.collect.Sets;

/**
 * Static utility methods for {@link MixinClass} creation.
 *
 * @author rui.figueira@gmail.com (Rui Figueira)
 */
public class MixinClasses {

    public static class Builder<T> {

        private Class<T> intf;
        private Set<Class<?>> others = Sets.newLinkedHashSet();

        public Builder(Class<T> intf) {
            this.intf = intf;
        }

        public Builder<T> addInterfaces(Class<?> ... others) {
            return addInterfaces(Arrays.asList(others));
        }

        public Builder<T> addInterfaces(Collection<Class<?>> others) {
            this.others.addAll(others);
            return this;
        }

        public MixinClass<T> build() {
            return create(intf, others);
        }
    }

    public static <T> Builder<T> builder(Class<T> intf) {
        return new Builder<T>(intf);
    }

    public static <T> MixinClass<T> create(Class<T> intf, Class<?> ... others) {
        return create(intf, Arrays.asList(others));
    }

    public static <T> MixinClass<T> create(Class<T> intf, Collection<Class<?>> others) {
        return new MixinClassImpl<T>(intf, others);
    }
}
