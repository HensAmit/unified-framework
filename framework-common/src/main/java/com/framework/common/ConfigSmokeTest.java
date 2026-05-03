package com.framework.common;

import com.framework.common.config.AppConfig;
import com.framework.common.config.ConfigManager;
import com.framework.common.utils.LogUtils;
import org.apache.logging.log4j.Logger;

/**
 * Manual verification entry point for Phase 1.
 *
 * <p>Confirms that:
 * <ul>
 *   <li>{@link ConfigManager} loads {@link AppConfig} correctly</li>
 *   <li>The {@code env} system property selects the right properties file</li>
 *   <li>System property overrides win over file-based values</li>
 *   <li>Log4j2 emits to console and file at INFO and DEBUG levels</li>
 * </ul>
 *
 * <p>Run from your IDE (right-click {@code main} → Run), passing
 * {@code -Denv=dev|staging|prod} as a VM argument to switch environments.
 *
 * <p>If you prefer the command line, build once first and use plain {@code java}:
 * <pre>
 * mvn install -pl framework-common -DskipTests
 * java -Denv=staging -cp "framework-common/target/classes:$(cat .cp)" \
 *      com.framework.common.ConfigSmokeTest
 * </pre>
 * (Generate {@code .cp} via {@code mvn dependency:build-classpath -Dmdep.outputFile=.cp}.)
 *
 * <p>This class is not a unit test — it's a deliberate smoke check we'll
 * remove (or convert to a test) when proper test infrastructure exists in Phase 3.
 */
public class ConfigSmokeTest {

    private static final Logger log = LogUtils.getLogger(ConfigSmokeTest.class);

    public static void main(String[] args) {
        LogUtils.banner("Phase 1 smoke test — config + logging");

        AppConfig config = ConfigManager.get();

        log.info("Active environment      : {}", config.env());
        log.info("API base URL            : {}", config.apiBaseUrl());
        log.info("Token URL               : {}", config.tokenUrl());
        log.info("Auth type               : {}", config.authType());
        log.info("Default timeout (ms)    : {}", config.apiTimeout());
        log.info("Retry count             : {}", config.retryCount());

        // Credentials should be empty unless supplied via -D or env vars.
        // We log only whether they were supplied — never the values themselves.
        log.info("Client ID supplied?     : {}", isNonBlank(config.clientId()));
        log.info("Client secret supplied? : {}", isNonBlank(config.clientSecret()));

        log.debug("This DEBUG line only appears if root level is DEBUG in log4j2.xml");

        LogUtils.banner("Smoke test complete — check target/logs/framework.log");
    }

    private static boolean isNonBlank(String s) {
        return s != null && !s.isBlank();
    }
}
