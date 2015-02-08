package platypus;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class DeloreanTest {

    public interface Car {
        String drive();
    }

    public interface Aircraft {
        String fly();
    }

    public interface Delorean extends Car, Aircraft { }

    public class CarImpl implements Car {
        @Override
        public String drive() {
            return "It can drive";
        }
    }

    public class AircraftImpl implements Aircraft {
        @Override
        public String fly() {
            return "It can fly";
        }
    }

    @Test
    public void testDelorean() {
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
}
