package com.framework.tests.listener;

import org.testng.IAnnotationTransformer;
import org.testng.annotations.ITestAnnotation;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Attaches {@link RetryAnalyzer} to every {@code @Test} method at runtime.
 *
 * <p>The actual test method here is {@code runScenario}, inherited from
 * {@code AbstractTestNGCucumberTests} — we can't edit its annotation to add a
 * {@code retryAnalyzer}. TestNG's {@link IAnnotationTransformer} solves this: it
 * lets us modify each {@code @Test} annotation programmatically as TestNG reads
 * it. Here we set the retry analyzer on every test, so retry applies uniformly
 * without touching the inherited method.
 *
 * <p>Registered as a {@code <listener>} in the TestNG suite XML.
 */
public class RetryTransformer implements IAnnotationTransformer {

    @Override
    public void transform(ITestAnnotation annotation,
                          Class testClass,
                          Constructor testConstructor,
                          Method testMethod) {
        annotation.setRetryAnalyzer(RetryAnalyzer.class);
    }
}
