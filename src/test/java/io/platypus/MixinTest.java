package io.platypus;

import org.junit.Test;

public class MixinTest {

    public interface Foo { }
    public interface Bar {
    }
    public class FooImpl implements Foo {}
    public class BarImpl implements Bar {
        public BarImpl(Foo foo) {
        }
    }

    public interface BarFactory {
        public Bar create(Foo foo);
    }

    public class BarFactoryImpl {

        private MixinFactoryBuilder that;

        public BarFactoryImpl(MixinFactoryBuilder that) {
            this.that = that;
        }

        public Bar create(final Foo foo) {
            return that.implement(Bar.class).with(new InstanceProvider<Bar>() {
                @Override
                public Bar provide(Object proxy) {
                    return new BarImpl(foo);
                }
            }).build().newInstance(Bar.class);
        }
    }

    @Test
    public void test() {

    }
}
