package com.framework.db.service;

import com.framework.common.config.AppConfig;
import com.framework.common.config.ConfigManager;
import com.framework.common.utils.LogUtils;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Thin JDBC wrapper — the database counterpart to {@code ApiService}.
 *
 * <p>Test code never touches raw JDBC; it goes through this service, which
 * exposes queries as {@code List<Map<String,Object>>} (one map per row,
 * column-name → value) — the JDBC equivalent of treating JSON as data rather
 * than mapping every table to a POJO.
 *
 * <p>Uses only {@code java.sql} plus the JDBC URL from config — no H2-specific
 * imports — so moving from H2 to a real MySQL (via Testcontainers) later changes
 * only the URL, not this class.
 *
 * <p>One {@link Connection} per instance (per scenario via PicoContainer), so it's
 * thread-confined and safe under parallel execution. The schema/data are seeded
 * <strong>once per JVM</strong> using double-checked locking on a static flag —
 * the shared in-memory database is populated by the first scenario to connect,
 * and concurrent scenarios wait for that to finish before querying.
 */
public class DbService {

    private static final Logger log = LogUtils.getLogger(DbService.class);

    private static final String SCHEMA_SCRIPT = "db/schema.sql";
    private static final String DATA_SCRIPT = "db/data.sql";

    // Seed once per JVM, regardless of how many scenarios connect (in parallel).
    private static volatile boolean seeded = false;
    private static final Object SEED_LOCK = new Object();

    private Connection connection;

    /** Opens a connection from config and ensures the schema/data are seeded. */
    public void connect() {
        AppConfig config = ConfigManager.get();
        try {
            connection = DriverManager.getConnection(
                    config.dbUrl(), config.dbUsername(), config.dbPassword());
            log.info("Opened DB connection: {}", config.dbUrl());
        } catch (SQLException e) {
            throw new RuntimeException("Failed to open DB connection: " + config.dbUrl(), e);
        }
        ensureSeeded();
    }

    /**
     * Runs a SELECT and returns the rows as column-name → value maps.
     * {@link LinkedHashMap} preserves column order for readable output.
     */
    public List<Map<String, Object>> query(String sql) {
        List<Map<String, Object>> rows = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {
            ResultSetMetaData meta = rs.getMetaData();
            int columnCount = meta.getColumnCount();
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    row.put(meta.getColumnLabel(i), rs.getObject(i));
                }
                rows.add(row);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Query failed: " + sql, e);
        }
        log.info("Query returned {} row(s): {}", rows.size(), sql);
        return rows;
    }

    /** Closes the connection. Best-effort — a close error must not mask a test failure. */
    public void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                log.warn("Error closing DB connection: {}", e.getMessage());
            }
        }
    }

    // -------------------------------------------------------------------------
    // One-time seeding (double-checked locking)
    // -------------------------------------------------------------------------

    private void ensureSeeded() {
        if (seeded) {
            return;
        }
        synchronized (SEED_LOCK) {
            if (seeded) {
                return;
            }
            log.info("Seeding database (schema + data) — once per JVM");
            runScript(SCHEMA_SCRIPT);
            runScript(DATA_SCRIPT);
            seeded = true;
        }
    }

    private void runScript(String classpathResource) {
        String script = readResource(classpathResource);
        try (Statement statement = connection.createStatement()) {
            for (String raw : script.split(";")) {
                String sql = raw.trim();
                if (!sql.isEmpty()) {
                    statement.execute(sql);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed running script: " + classpathResource, e);
        }
    }

    private String readResource(String classpathResource) {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(classpathResource)) {
            if (is == null) {
                throw new IllegalArgumentException("Script not found on classpath: " + classpathResource);
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    String trimmed = line.trim();
                    if (trimmed.isEmpty() || trimmed.startsWith("--")) {
                        continue;   // skip blank lines and SQL comments
                    }
                    sb.append(line).append('\n');
                }
                return sb.toString();
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed reading script: " + classpathResource, e);
        }
    }
}
