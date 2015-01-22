package io.platypus;

import java.util.Collection;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

public class MixinInitializers {

    private static final class CompositeMixinInitializer implements MixinInitializer {
        private final Set<? extends MixinInitializer> initializers;

        private CompositeMixinInitializer(Collection<? extends MixinInitializer> initializers) {
            this.initializers = ImmutableSet.copyOf(initializers);
        }

        @Override
        public void initialize(MixinImplementor mixinImplementor) {
            for (MixinInitializer mixinInitializer : initializers) {
                mixinInitializer.initialize(mixinImplementor);
            }
        }
    }

    public static final MixinInitializer NULL = new MixinInitializer() {
        @Override
        public void initialize(MixinImplementor mixinImplementor) {
            // do nothing
        }
    };

    public static MixinInitializer combine(MixinInitializer ... initializers) {
        return combine(ImmutableSet.copyOf(initializers));
    }

    public static MixinInitializer combine(final Collection<? extends MixinInitializer> initializers) {
        return new CompositeMixinInitializer(initializers);
    }
}
