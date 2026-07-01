package com.framework.common.report;

import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import com.framework.common.utils.LogUtils;
import org.apache.logging.log4j.Logger;

/**
 * Static facade for logging to the current scenario's Extent node from anywhere
 * (pages, services, step defs). Reads the per-thread ExtentTest from
 * ExtentTestManager, so it is parallel-safe: each scenario thread logs to its
 * own node. No-ops safely if no test is set (e.g. unit tests, or calls outside
 * a scenario), falling back to Log4j so the message is never silently lost.
 */
public final class ReportLog {

    private static final Logger log = LogUtils.getLogger(ReportLog.class);

    private ReportLog() {}   // static-only

    public static void info(String message) {
        ExtentTest test = ExtentTestManager.get();
        if (test != null) {
            test.log(Status.INFO, message);
        }
        log.info(message);
    }

    public static void pass(String message) {
        ExtentTest test = ExtentTestManager.get();
        if (test != null) {
            test.log(Status.PASS, message);
        }
        log.info(message);
    }

    public static void fail(String message) {
        ExtentTest test = ExtentTestManager.get();
        if (test != null) {
            test.log(Status.FAIL, message);
        }
        log.error(message);
    }
}