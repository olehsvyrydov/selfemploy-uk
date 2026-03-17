package uk.selfemploy.persistence.schema;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Schema drift prevention test.
 *
 * <p>Verifies that Flyway migration DDL matches the JPA entity definitions.
 * Catches two classes of drift:</p>
 * <ul>
 *   <li>Entity references a column that doesn't exist in the migrated schema</li>
 *   <li>Migration creates a column that no entity maps (potential dead column or missing mapping)</li>
 * </ul>
 *
 * <p>This is a standalone test (no Quarkus container needed) for fast execution.</p>
 */
@DisplayName("Schema Drift Prevention")
class SchemaDriftTest {

    private static final String JDBC_URL = "jdbc:h2:mem:schema_drift_test;DB_CLOSE_DELAY=-1";
    private static final String ENTITY_PACKAGE = "uk.selfemploy.persistence.entity";

    /**
     * Columns that exist in the database but are intentionally unmapped in JPA entities.
     * Each entry is TABLE_NAME.COLUMN_NAME in uppercase.
     * These are reviewed and accepted gaps - add entries here only with justification.
     */
    private static final Set<String> KNOWN_UNMAPPED_DB_COLUMNS = Set.of(
            // V4 added source/import_batch_id to incomes and expenses for bank import tracking.
            // These columns are read/written via native queries in the repository layer,
            // not through JPA entity fields, because they are import-specific metadata
            // that the domain Income/Expense records do not carry.
            "INCOMES.SOURCE",
            "INCOMES.IMPORT_BATCH_ID",
            "EXPENSES.SOURCE",
            "EXPENSES.IMPORT_BATCH_ID"
    );

    /**
     * Tables created by Flyway that have no corresponding JPA entity.
     * These are managed outside of JPA (e.g., Flyway's own schema_history table,
     * or tables accessed only via native SQL).
     */
    private static final Set<String> EXCLUDED_TABLES = Set.of(
            "FLYWAY_SCHEMA_HISTORY",
            // V20: reconciliation_matches uses VARCHAR(36) IDs and VARCHAR(50) timestamps,
            // designed for lightweight native SQL access via ReconciliationService.
            // No JPA entity needed - records are read/written through JDBC utilities.
            "RECONCILIATION_MATCHES"
    );

    private static Connection connection;

    /** Schema from Flyway migrations: TABLE_NAME -> Set of COLUMN_NAME (all uppercase). */
    private static Map<String, Set<String>> flywaySchema;

    /** Schema from JPA entities: TABLE_NAME -> Set of COLUMN_NAME (all uppercase). */
    private static Map<String, Set<String>> entitySchema;

    @BeforeAll
    static void setUp() throws Exception {
        connection = DriverManager.getConnection(JDBC_URL, "sa", "");

        // Run all Flyway migrations
        Flyway flyway = Flyway.configure()
                .dataSource(JDBC_URL, "sa", "")
                .locations("classpath:db/migration")
                .load();
        flyway.migrate();

        // Extract schemas
        flywaySchema = extractFlywaySchema(connection);
        entitySchema = extractEntitySchema();
    }

    @AfterAll
    static void tearDown() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    @Nested
    @DisplayName("Table existence")
    class TableExistence {

        @Test
        @DisplayName("Every JPA entity table must exist in Flyway schema")
        void entityTablesMustExistInDatabase() {
            Set<String> missingTables = new TreeSet<>();
            for (String entityTable : entitySchema.keySet()) {
                if (!flywaySchema.containsKey(entityTable)) {
                    missingTables.add(entityTable);
                }
            }

            assertThat(missingTables)
                    .as("JPA entity tables not found in Flyway schema (need migration?)")
                    .isEmpty();
        }

        @Test
        @DisplayName("Every Flyway table should have a JPA entity mapping")
        void databaseTablesShouldHaveEntityMapping() {
            Set<String> unmappedTables = new TreeSet<>();
            for (String dbTable : flywaySchema.keySet()) {
                if (!EXCLUDED_TABLES.contains(dbTable) && !entitySchema.containsKey(dbTable)) {
                    unmappedTables.add(dbTable);
                }
            }

            assertThat(unmappedTables)
                    .as("Database tables without JPA entity mapping (need entity class?)")
                    .isEmpty();
        }
    }

    @Nested
    @DisplayName("Column existence")
    class ColumnExistence {

        @Test
        @DisplayName("Every JPA entity column must exist in Flyway schema")
        void entityColumnsMustExistInDatabase() {
            Map<String, List<String>> missingColumns = new TreeMap<>();

            for (var entry : entitySchema.entrySet()) {
                String table = entry.getKey();
                Set<String> entityColumns = entry.getValue();

                if (!flywaySchema.containsKey(table)) {
                    // Table-level mismatch handled by table tests
                    continue;
                }

                Set<String> dbColumns = flywaySchema.get(table);
                for (String col : entityColumns) {
                    if (!dbColumns.contains(col)) {
                        missingColumns.computeIfAbsent(table, k -> new ArrayList<>()).add(col);
                    }
                }
            }

            if (!missingColumns.isEmpty()) {
                StringBuilder message = new StringBuilder();
                message.append("JPA entity columns not found in database (need migration?):\n");
                for (var entry : missingColumns.entrySet()) {
                    for (String col : entry.getValue()) {
                        message.append("  ").append(entry.getKey()).append(".").append(col).append("\n");
                    }
                }
                fail(message.toString());
            }
        }

        @Test
        @DisplayName("Every database column should be mapped in a JPA entity")
        void databaseColumnsShouldBeMappedInEntity() {
            Map<String, List<String>> unmappedColumns = new TreeMap<>();

            for (var entry : flywaySchema.entrySet()) {
                String table = entry.getKey();

                if (EXCLUDED_TABLES.contains(table)) {
                    continue;
                }

                if (!entitySchema.containsKey(table)) {
                    // Table-level mismatch handled by table tests
                    continue;
                }

                Set<String> dbColumns = entry.getValue();
                Set<String> entityColumns = entitySchema.get(table);

                for (String col : dbColumns) {
                    String qualifiedColumn = table + "." + col;
                    if (!entityColumns.contains(col) && !KNOWN_UNMAPPED_DB_COLUMNS.contains(qualifiedColumn)) {
                        unmappedColumns.computeIfAbsent(table, k -> new ArrayList<>()).add(col);
                    }
                }
            }

            if (!unmappedColumns.isEmpty()) {
                StringBuilder message = new StringBuilder();
                message.append("Database columns not mapped in JPA entities ");
                message.append("(add to entity or to KNOWN_UNMAPPED_DB_COLUMNS with justification):\n");
                for (var entry : unmappedColumns.entrySet()) {
                    for (String col : entry.getValue()) {
                        message.append("  ").append(entry.getKey()).append(".").append(col).append("\n");
                    }
                }
                fail(message.toString());
            }
        }
    }

    @Nested
    @DisplayName("Known gaps tracking")
    class KnownGapsTracking {

        @Test
        @DisplayName("Known unmapped columns must actually exist in the database")
        void knownUnmappedColumnsMustExistInDatabase() {
            List<String> staleEntries = new ArrayList<>();

            for (String entry : KNOWN_UNMAPPED_DB_COLUMNS) {
                String[] parts = entry.split("\\.");
                String table = parts[0];
                String column = parts[1];

                Set<String> dbColumns = flywaySchema.get(table);
                if (dbColumns == null || !dbColumns.contains(column)) {
                    staleEntries.add(entry);
                }
            }

            assertThat(staleEntries)
                    .as("Stale entries in KNOWN_UNMAPPED_DB_COLUMNS (column no longer exists in DB)")
                    .isEmpty();
        }

        @Test
        @DisplayName("Known unmapped columns must not be mapped in entities (stale exclusion)")
        void knownUnmappedColumnsMustNotBeMappedInEntities() {
            List<String> staleEntries = new ArrayList<>();

            for (String entry : KNOWN_UNMAPPED_DB_COLUMNS) {
                String[] parts = entry.split("\\.");
                String table = parts[0];
                String column = parts[1];

                Set<String> entityColumns = entitySchema.get(table);
                if (entityColumns != null && entityColumns.contains(column)) {
                    staleEntries.add(entry);
                }
            }

            assertThat(staleEntries)
                    .as("Entries in KNOWN_UNMAPPED_DB_COLUMNS that are now mapped in entities (remove from exclusion list)")
                    .isEmpty();
        }
    }

    @Nested
    @DisplayName("Schema summary")
    class SchemaSummary {

        @Test
        @DisplayName("Should produce schema summary for debugging")
        void shouldProduceSchemaSummary() {
            // This test always passes and prints the schema summary for diagnostics
            System.out.println("\n=== Schema Drift Test Summary ===");
            System.out.println("Flyway tables: " + flywaySchema.keySet().stream()
                    .filter(t -> !EXCLUDED_TABLES.contains(t))
                    .sorted()
                    .collect(Collectors.joining(", ")));
            System.out.println("Entity tables: " + entitySchema.keySet().stream()
                    .sorted()
                    .collect(Collectors.joining(", ")));

            int totalDbColumns = flywaySchema.entrySet().stream()
                    .filter(e -> !EXCLUDED_TABLES.contains(e.getKey()))
                    .mapToInt(e -> e.getValue().size())
                    .sum();
            int totalEntityColumns = entitySchema.values().stream()
                    .mapToInt(Set::size)
                    .sum();

            System.out.println("Total DB columns (excl. Flyway): " + totalDbColumns);
            System.out.println("Total entity columns: " + totalEntityColumns);
            System.out.println("Known unmapped DB columns: " + KNOWN_UNMAPPED_DB_COLUMNS.size());
            System.out.println("=================================\n");
        }
    }

    // ========== Schema extraction methods ==========

    /**
     * Extracts the database schema after Flyway migrations.
     * Returns a map of TABLE_NAME (uppercase) to set of COLUMN_NAME (uppercase).
     */
    private static Map<String, Set<String>> extractFlywaySchema(Connection conn) throws SQLException {
        Map<String, Set<String>> schema = new TreeMap<>();
        DatabaseMetaData metaData = conn.getMetaData();

        // Get all tables in PUBLIC schema
        try (ResultSet tables = metaData.getTables(null, "PUBLIC", "%", new String[]{"TABLE"})) {
            while (tables.next()) {
                String tableName = tables.getString("TABLE_NAME").toUpperCase();
                Set<String> columns = new TreeSet<>();

                try (ResultSet cols = metaData.getColumns(null, "PUBLIC", tableName, "%")) {
                    while (cols.next()) {
                        columns.add(cols.getString("COLUMN_NAME").toUpperCase());
                    }
                }

                schema.put(tableName, columns);
            }
        }

        return schema;
    }

    /**
     * Extracts the JPA entity schema by scanning entity classes via reflection.
     * Returns a map of TABLE_NAME (uppercase) to set of COLUMN_NAME (uppercase).
     *
     * <p>Column name resolution follows JPA conventions:
     * <ol>
     *   <li>Explicit {@code @Column(name = "...")} annotation</li>
     *   <li>Default: field name converted from camelCase to snake_case</li>
     * </ol>
     */
    private static Map<String, Set<String>> extractEntitySchema() throws Exception {
        Map<String, Set<String>> schema = new TreeMap<>();
        List<Class<?>> entityClasses = findEntityClasses();

        for (Class<?> clazz : entityClasses) {
            String tableName = resolveTableName(clazz);
            Set<String> columns = new LinkedHashSet<>();

            // Walk up the class hierarchy to include inherited fields
            Class<?> current = clazz;
            while (current != null && current != Object.class) {
                for (Field field : current.getDeclaredFields()) {
                    if (isTransient(field)) {
                        continue;
                    }
                    String columnName = resolveColumnName(field);
                    if (columnName != null) {
                        columns.add(columnName.toUpperCase());
                    }
                }
                current = current.getSuperclass();
            }

            schema.put(tableName.toUpperCase(), columns);
        }

        return schema;
    }

    /**
     * Finds all classes annotated with {@code @Entity} in the entity package.
     */
    private static List<Class<?>> findEntityClasses() throws Exception {
        List<Class<?>> entityClasses = new ArrayList<>();
        String path = ENTITY_PACKAGE.replace('.', '/');
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Enumeration<URL> resources = classLoader.getResources(path);

        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            if (!"file".equals(resource.getProtocol())) {
                continue;
            }
            File directory = java.nio.file.Paths.get(resource.toURI()).toFile();
            if (directory.exists()) {
                File[] files = directory.listFiles();
                if (files == null) continue;
                for (File file : files) {
                    if (file.getName().endsWith(".class")) {
                        String className = ENTITY_PACKAGE + "." +
                                file.getName().substring(0, file.getName().length() - 6);
                        Class<?> clazz = Class.forName(className);
                        if (clazz.isAnnotationPresent(Entity.class)) {
                            entityClasses.add(clazz);
                        }
                    }
                }
            }
        }

        return entityClasses;
    }

    /**
     * Resolves the table name for a JPA entity class.
     * Uses {@code @Table(name = "...")} if present, otherwise the simple class name.
     */
    private static String resolveTableName(Class<?> clazz) {
        Table tableAnnotation = clazz.getAnnotation(Table.class);
        if (tableAnnotation != null && !tableAnnotation.name().isEmpty()) {
            return tableAnnotation.name();
        }
        // Default JPA table naming: class name as-is (Hibernate would apply naming strategy)
        return camelCaseToSnakeCase(clazz.getSimpleName());
    }

    /**
     * Resolves the column name for a JPA entity field.
     * Returns null for fields that are not persisted (static, transient, non-JPA).
     */
    private static String resolveColumnName(Field field) {
        // Skip static and transient fields
        if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
            return null;
        }
        if (java.lang.reflect.Modifier.isTransient(field.getModifiers())) {
            return null;
        }

        // Check for @jakarta.persistence.Transient
        if (field.isAnnotationPresent(jakarta.persistence.Transient.class)) {
            return null;
        }

        // Check for explicit @Column annotation
        Column columnAnnotation = field.getAnnotation(Column.class);
        if (columnAnnotation != null && !columnAnnotation.name().isEmpty()) {
            return columnAnnotation.name();
        }

        // Check for @Id without @Column - defaults to field name
        if (field.isAnnotationPresent(Id.class)) {
            return camelCaseToSnakeCase(field.getName());
        }

        // Default JPA column naming: camelCase to snake_case
        // Only include if it looks like a persistable field (has JPA annotation or follows naming convention)
        if (columnAnnotation != null) {
            // @Column present but no name - use default
            return camelCaseToSnakeCase(field.getName());
        }

        // Fields without any JPA annotation are still persisted by default in JPA
        // (unless the entity uses property access mode, which these entities don't)
        return camelCaseToSnakeCase(field.getName());
    }

    /**
     * Checks if a field should be treated as transient (not persisted).
     */
    private static boolean isTransient(Field field) {
        return java.lang.reflect.Modifier.isStatic(field.getModifiers())
                || java.lang.reflect.Modifier.isTransient(field.getModifiers())
                || field.isAnnotationPresent(jakarta.persistence.Transient.class);
    }

    /**
     * Converts a camelCase string to snake_case.
     * Example: "businessId" -> "business_id", "accountLastFour" -> "account_last_four"
     */
    static String camelCaseToSnakeCase(String camelCase) {
        if (camelCase == null || camelCase.isEmpty()) {
            return camelCase;
        }
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < camelCase.length(); i++) {
            char ch = camelCase.charAt(i);
            if (Character.isUpperCase(ch)) {
                if (i > 0) {
                    result.append('_');
                }
                result.append(Character.toLowerCase(ch));
            } else {
                result.append(ch);
            }
        }
        return result.toString();
    }
}
