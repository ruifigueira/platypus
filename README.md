Platypus [![Build Status](https://travis-ci.org/ruifigueira/platypus.png)](https://travis-ci.org/ruifigueira/platypus)
======================================================================================================================

Platypus is a Java Mixins library. It is being heavily used in [Minium](https://github.com/viltgroup/minium).

A quick example:

```java
public interface Car {
    String drive();
}

public interface Aircraft {
    String fly();
}

public interface Delorean extends Car, Aircraft { }

public class CarImpl implements Car {
    @Override public String drive() {
        return "It can drive";
    }
}

public class AircraftImpl implements Aircraft {
    @Override public String fly() {
        return "It can fly";
    }
}

@Test public void testDelorean() {
	MixinClass<Delorean> deloreanMixinClass = MixinClasses.create(Delorean.class);
	
	Delorean delorean = deloreanMixinClass.newInstance(new AbstractMixinInitializer() {
	    @Override
	    protected void initialize() {
	        implement(Car.class).with(new CarImpl());
	        implement(Aircraft.class).with(new AircraftImpl());
	    }
	});
	
	assertThat(delorean.drive(), equalTo("It can drive"));
	assertThat(delorean.fly(), equalTo("It can fly"));
}
```

Besides, `Mixin` interface and its default implementation, `Mixin.Impl` will allow maximum fun
with complex mixins, where you can chain different interface calls using the `.as(Class<?>)` method.

For instance, check the `delorean.as(Aircraft.class).fly()` in the adaptation of the previous example:

```java
public interface Car implements Mixin {
    String drive();
}

public interface Aircraft implements Mixin {
    String fly();
}

public class CarImpl extends Mixin implements Car {
    @Override public String drive() {
        return "It can drive";
    }
}

public class AircraftImpl extends Mixin implements Aircraft {
    @Override public String fly() {
        return "It can fly";
    }
}

@Test public void testDeloreanCarWithAircrafCapabilities() {
	MixinClass<Car> deloreanMixinClass = MixinClasses.create(Car.class, Aircraft.class);
	
	Car delorean = deloreanMixinClass.newInstance(new AbstractMixinInitializer() {
	    @Override
	    protected void initialize() {
	        implement(Car.class).with(new CarImpl());
	        implement(Aircraft.class).with(new AircraftImpl());
	    }
	});
	
	assertThat(delorean.drive(), equalTo("It can drive"));
	assertThat(delorean.as(Aircraft.class).fly(), equalTo("It can fly"));
}
```

