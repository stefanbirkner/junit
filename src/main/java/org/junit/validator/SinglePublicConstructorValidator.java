package org.junit.validator;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import java.util.List;

import org.junit.runners.model.TestClass;

/**
 * Validates that a {@link TestClass} has one and only one public constructor.
 * 
 * @since 4.13
 */
public class SinglePublicConstructorValidator implements TestClassValidator {
    private static final List<Exception> NO_VALIDATION_ERRORS = emptyList();

    public List<Exception> validateTestClass(TestClass testClass) {
        if (testClass.getJavaClass().getConstructors().length == 0) {
            return singletonList(new Exception(
                    "Test class should have exactly one public constructor"));
        } else {
            // number of constructors == 1 otherwise TestClass could not be
            // created
            return NO_VALIDATION_ERRORS;
        }
    }
}
