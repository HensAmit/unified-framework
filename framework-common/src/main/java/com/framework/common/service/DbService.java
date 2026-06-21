package com.framework.common.service;

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
 * <p>Test code never touches raw JDBC; it goes through this service, which exposes
 * queries as {@code List<Map<String,Object>>}. Uses only {@code java.sql} plus the
 * JDBC URL from config, so the engine is a configuration concern.
 *
 * <p>One {@link Connection} per instance (per scenario via PicoContainer), so it's
 * thread-confined and parallel-safe. {@link #connect()} is idempotent — calling it
 * more than once per scenario (e.g. from multiple hooks) opens at most one
 * connection. Schema/data are seeded once per JVM via double-checked locking.
 */
public class DbService {

    private static final Logger log = LogUtils.getLogger(DbService.class);

    private static final String SCHEMA_SCRIPT = "db/schema.sql";
    private static final String DATA_SCRIPT = "db/data.sql";

    private static volatile boolean seeded = false;
    private static final Object SEED_LOCK = new Object();

    private Connection connection;

    /**
     * Opens a connection from config (if not already open) and ensures the
     * schema/data are seeded. Idempotent: a second call while connected no-ops.
     */
    public void connect() {
        if (connection != null) {
            return;   // already connected for this scenario
        }
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

    /** Closes the connection (best-effort) and clears it so a later connect() can reopen. */
    public void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                log.warn("Error closing DB connection: {}", e.getMessage());
            } finally {
                connection = null;
            }
        }
    }

    // ---- one-time seeding (double-checked locking) ----

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
                        continue;
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
