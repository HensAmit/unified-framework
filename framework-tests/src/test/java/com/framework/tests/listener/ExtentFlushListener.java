package com.framework.tests.listener;

import com.framework.common.report.ExtentManager;
import com.framework.common.utils.LogUtils;
import org.apache.logging.log4j.Logger;
import org.testng.ISuite;
import org.testng.ISuiteListener;

/**
 * Flushes the Extent report once, when a suite finishes.
 *
 * <p>Through Phase 6 each scenario's {@code @After} hook called
 * {@code ExtentManager.flush()}. Under parallel execution that means many
 * threads flushing the shared report concurrently — wasteful and a potential
 * write race. Moving the flush here makes it happen once per suite, after all
 * that suite's scenarios have finished, off the scenario threads.
 *
 * <p>{@code ExtentManager} is a singleton that accumulates every test node
 * across both suites, so flushing at the end of each suite progressively writes
 * the complete report; the final suite's flush contains everything.
 *
 * <p>Registered as a {@code <listener>} in the TestNG suite XML.
 */
public class ExtentFlushListener implements ISuiteListener {

    private static final Logger log = LogUtils.getLogger(ExtentFlushListener.class);

    @Override
    public void onFinish(ISuite suite) {
        log.info("Suite finished: {} — flushing Extent report", suite.getName());
        ExtentManager.flush();
    }
}
