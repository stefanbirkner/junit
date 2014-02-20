package org.junit.internal.reflection;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.junit.Test;

public class ObjectFactoryTest {
    private final ObjectFactory factory = new ObjectFactory();

    public static class ClassWithPublicZeroArgConstructor {
    }

    @Test
    public void createsObject() throws Exception {
        ClassWithPublicZeroArgConstructor object = factory
                .createObjectWithClass(ClassWithPublicZeroArgConstructor.class);
        assertThat(object,
                is(instanceOf(ClassWithPublicZeroArgConstructor.class)));
    }
}
