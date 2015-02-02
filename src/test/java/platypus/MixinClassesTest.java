package platypus;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;

import java.lang.reflect.Method;

import org.junit.Test;

import platypus.AbstractMixinInitializer;
import platypus.IncompleteImplementationException;
import platypus.InstanceProvider;
import platypus.InstanceProviders;
import platypus.Mixin;
import platypus.MixinClass;
import platypus.MixinClasses;

import com.google.common.base.Defaults;
import com.google.common.reflect.AbstractInvocationHandler;

public class MixinClassesTest {

    public interface Foo extends Mixin {
        String foo();
    }

    public interface Bar {
        String bar();
    }

    public interface FooBar extends Foo, Bar {
    }

    public class FooImpl extends Mixin.Impl implements Foo {

        public FooImpl() {
        }

        @Override
        public String foo() {
            return "generic foo";
        }

        public Foo getThat() {
            return this.as(Foo.class);
        }
    }

    public class BarImpl implements Bar {
        @Override
        public String bar() {
            return "generic bar";
        }
    }

    public class AltBarImpl implements Bar {
        @Override
        public String bar() {
            return "alternative generic bar";
        }
    }

    @Test
    public void testSpecificImplementation() {
        MixinClass<FooBar> fooBarClass = MixinClasses.create(FooBar.class, Foo.class, Bar.class);

        FooBar fooBar = fooBarClass.newInstance(new AbstractMixinInitializer() {
            @Override
            protected void initialize() {
                implement(Foo.class).with(new FooImpl());
                implement(Bar.class).with(new BarImpl());
            }
        });

        assertThat(fooBar.foo(), equalTo("generic foo"));
        assertThat(fooBar.bar(), equalTo("generic bar"));
    }

    @Test
    public void testRemainers() {
        MixinClass<FooBar> fooBarClass = MixinClasses.create(FooBar.class, Foo.class, Bar.class);

        FooBar fooBar = fooBarClass.newInstance(new AbstractMixinInitializer() {
            @Override
            protected void initialize() {
                implement(Foo.class).with(new FooImpl());
                implementRemainers().with(new BarImpl());
            }
        });

        assertThat(fooBar.foo(), equalTo("generic foo"));
        assertThat(fooBar.bar(), equalTo("generic bar"));
    }

    @Test
    public void testOverride() {
        MixinClass<FooBar> fooBarClass = MixinClasses.create(FooBar.class, Foo.class, Bar.class);

        FooBar fooBar = fooBarClass.newInstance(new AbstractMixinInitializer() {
            @Override
            protected void initialize() {
                implement(Foo.class).with(new FooImpl());
                implement(Bar.class).with(new BarImpl());
                override(Bar.class).with(new AltBarImpl());
            }
        });

        assertThat(fooBar.foo(), equalTo("generic foo"));
        assertThat(fooBar.bar(), equalTo("alternative generic bar"));
    }

    @Test(expected = IncompleteImplementationException.class)
    public void testInterfaceWithNoImplementation() {
        MixinClass<FooBar> fooBarClass = MixinClasses.create(FooBar.class);

        fooBarClass.newInstance(new AbstractMixinInitializer() {
            @Override
            protected void initialize() {
                implement(Foo.class);
                implement(Bar.class).with(new BarImpl());
            }
        });
    }

    @Test(expected = IncompleteImplementationException.class)
    public void testIncompleteImplementation() {
        MixinClass<FooBar> fooBarClass = MixinClasses.create(FooBar.class);

        fooBarClass.newInstance(new AbstractMixinInitializer() {
            @Override
            protected void initialize() {
                implement(Bar.class).with(new BarImpl());
            }
        });
    }

    @Test
    public void testInvocationHandlerImplementation() {
        // given
        MixinClass<FooBar> fooBarClass = MixinClasses.create(FooBar.class);
        class TestInvocationHandler extends AbstractInvocationHandler {

            Object proxy;

            @Override
            protected Object handleInvocation(Object proxy, Method method, Object[] args) throws Throwable {
                this.proxy = proxy;
                return Defaults.defaultValue(method.getReturnType());
            }
        };
        final TestInvocationHandler handler = new TestInvocationHandler();

        // when
        FooBar foobar = fooBarClass.newInstance(new AbstractMixinInitializer() {
            @Override
            protected void initialize() {
                InstanceProvider<Bar> barProxyProvider = InstanceProviders.adapt(handler, Bar.class);
                implement(Foo.class).with(new FooImpl());
                implement(Bar.class).with(barProxyProvider);
            }
        });
        foobar.bar();

        // then
        assertThat(handler.proxy, sameInstance((Object) foobar));
    }

    @Test
    public void testMixinInitialization() {
        // given
        MixinClass<Mixin> mixinClass = MixinClasses.create(Mixin.class, FooBar.class);

        // when
        final Foo foo = new FooImpl();
        // this will initialize foo with the proxy
        Mixin mixin = mixinClass.newInstance(new AbstractMixinInitializer() {
            @Override
            protected void initialize() {
                implement(Object.class).with(new Object());
                implement(Foo.class).with(foo);
                implement(Bar.class).with(new BarImpl());
                assertThat(foo.as(Foo.class), is(foo));
            }
        });
        assertThat((Foo) mixin, not(sameInstance(foo)));
        assertThat(foo.as(Foo.class), sameInstance(mixin));
        assertThat(mixin.as(Foo.class), sameInstance(mixin));
    }

}
