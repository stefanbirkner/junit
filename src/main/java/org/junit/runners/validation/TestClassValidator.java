package org.junit.runners.validation;

import org.junit.runners.model.TestClass;

import java.util.List;

public interface TestClassValidator {
    void validateTestClass(TestClass testClass, List<Throwable> errors);
}
