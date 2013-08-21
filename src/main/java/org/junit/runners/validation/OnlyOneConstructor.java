package org.junit.runners.validation;

import org.junit.runners.model.TestClass;

import java.util.List;

public class OnlyOneConstructor implements TestClassValidator {
    public void validateTestClass(TestClass testClass, List<Throwable> errors) {
        if (testClass.getJavaClass().getConstructors().length != 1) {
            String gripe = "Test class should have exactly one public constructor";
            errors.add(new Exception(gripe));
        }
    }
}
