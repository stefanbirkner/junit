package org.junit.validator;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.junit.runners.model.TestClass;

public class SinglePublicConstructorValidatorTest {
    private final TestClassValidator validator = new SinglePublicConstructorValidator();

    // class must be public in order to have a public default constructor
    public static class DefaultConstructor {
        // has default constructor
    }

    @Test
    public void acceptsClassWithDefaultConstructor() {
        TestClass testClass = new TestClass(DefaultConstructor.class);
        List<Exception> validationErrors = validator
                .validateTestClass(testClass);
        assertThat(validationErrors,
                is(equalTo(Collections.<Exception> emptyList())));
    }

    static class SingleConstructor {
        public SingleConstructor() {
        }
    }

    @Test
    public void acceptsClassWithSingleConstructor() {
        TestClass testClass = new TestClass(SingleConstructor.class);
        List<Exception> validationErrors = validator
                .validateTestClass(testClass);
        assertThat(validationErrors,
                is(equalTo(Collections.<Exception> emptyList())));
    }

    static class PublicAndNonPublicConstructors {
        public PublicAndNonPublicConstructors() {
        }

        private PublicAndNonPublicConstructors(int value) {
        }
    }

    @Test
    public void acceptsClassWithPublicConstructorAndAdditionalNonPublicConstructor() {
        TestClass testClass = new TestClass(
                PublicAndNonPublicConstructors.class);
        List<Exception> validationErrors = validator
                .validateTestClass(testClass);
        assertThat(validationErrors,
                is(equalTo(Collections.<Exception> emptyList())));
    }

    static class NonPublicConstructor {
        NonPublicConstructor() {
        }
    }

    @Test
    public void rejectsClassWithNonPublicConstructorOnly() {
        TestClass testClass = new TestClass(NonPublicConstructor.class);
        List<Exception> validationErrors = validator
                .validateTestClass(testClass);
        assertThat("Wrong number of errors.", validationErrors.size(),
                is(equalTo(1)));
    }
}
