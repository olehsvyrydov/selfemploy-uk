package uk.selfemploy.ui.service.security;

import org.sqlite.mc.SQLiteMCSqlCipherConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Central place for opening SQLCipher-encrypted connections with the willena/sqlite-jdbc-crypt driver.
 * The SQLCipher v4 profile is pinned so the on-disk format is deterministic across every connection
 * (both the store's primary and per-thread connections, and the migrator). The database key is handed
 * to SQLCipher as a hex string, which SQLCipher runs through its own KDF over a per-database salt.
 */
public final class SqlCipherSupport {

    private SqlCipherSupport() {
    }

    /** JDBC connection properties that open/create an SQLCipher-v4 database with the given key. */
    public static Properties keyProperties(DbKey key) {
        return SQLiteMCSqlCipherConfig.getV4Defaults().withKey(key.hex()).build().toProperties();
    }

    /** Opens a keyed connection to {@code jdbcUrl} (an SQLCipher-v4 database). */
    public static Connection openEncrypted(String jdbcUrl, DbKey key) throws SQLException {
        return DriverManager.getConnection(jdbcUrl, keyProperties(key));
    }
}
