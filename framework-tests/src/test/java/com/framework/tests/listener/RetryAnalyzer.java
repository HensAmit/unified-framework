package com.framework.tests.listener;

import com.framework.common.config.ConfigManager;
import com.framework.common.utils.LogUtils;
import org.apache.logging.log4j.Logger;
import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

/**
 * Retries a failed test invocation up to a configured number of times.
 *
 * <p>TestNG calls {@link #retry(ITestResult)} only when an invocation fails;
 * returning {@code true} reruns it, {@code false} accepts the failure. The
 * per-instance {@code attempt} counter tracks how many retries this invocation
 * has already had, capped at {@code retry.count} from config (default 2 -> up to
 * 3 total attempts).
 *
 * <p>This relies on TestNG creating a fresh {@code RetryAnalyzer} instance per
 * test-method invocation (true in TestNG 7.x). Because each parallel scenario is
 * a separate invocation with its own analyzer instance, the counter is naturally
 * isolated -- no thread-safety concern.
 *
 * <p>Retry papers over <em>flakiness</em>, not real bugs: a deterministic failure
 * fails on every attempt and still ends red. It exists so a transient hiccup
 * (a network blip, a momentary UI timing issue) doesn't fail an otherwise-green
 * CI run.
 */
public class RetryAnalyzer implements IRetryAnalyzer {

    private static final Logger log = LogUtils.getLogger(RetryAnalyzer.class);

    private final int maxRetries = ConfigManager.get().retryCount();

    private int attempt = 0;

    @Override
    public boolean retry(ITestResult result) {
        if (attempt < maxRetries) {
            attempt++;
            log.warn("Retrying '{}' (retry {} of {})",
                    result.getName(), attempt, maxRetries);
            return true;
        }
        return false;
    }
}
