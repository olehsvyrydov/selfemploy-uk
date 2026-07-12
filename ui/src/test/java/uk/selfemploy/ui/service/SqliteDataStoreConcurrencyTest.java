package uk.selfemploy.ui.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the file-mode per-thread connection model: each thread gets its own SQLite connection so
 * concurrent read/write (e.g. a background CSV-import thread writing while the JavaFX thread reads)
 * never issues statements on a shared, non-thread-safe connection. Uses a real temporary database
 * file via the package-private path constructor, since in-memory mode deliberately keeps a single
 * shared connection.
 */
@DisplayName("SqliteDataStore file-mode concurrency")
class SqliteDataStoreConcurrencyTest {

    @Test
    @DisplayName("connection() returns a distinct connection per thread, stable within a thread")
    void connectionIsPerThread(@TempDir Path dir) throws Exception {
        SqliteDataStore store = new SqliteDataStore(dir.resolve("per-thread.db"));
        try {
            Connection main1 = store.connection();
            Connection main2 = store.connection();
            assertThat(main2).isSameAs(main1); // stable within the calling thread

            AtomicReference<Connection> otherThread = new AtomicReference<>();
            Thread t = new Thread(() -> otherThread.set(store.connection()));
            t.start();
            t.join();

            assertThat(otherThread.get()).isNotNull();
            assertThat(otherThread.get()).isNotSameAs(main1); // a different thread → a different connection
        } finally {
            store.close();
        }
    }

    @Test
    @DisplayName("file mode uses WAL journaling")
    void fileModeUsesWal(@TempDir Path dir) throws Exception {
        SqliteDataStore store = new SqliteDataStore(dir.resolve("wal.db"));
        try {
            assertThat(store.getJournalMode()).isEqualToIgnoringCase("wal");
        } finally {
            store.close();
        }
    }

    @Test
    @DisplayName("concurrent writer and reader on their own connections never fail")
    void concurrentReadWriteDoesNotFail(@TempDir Path dir) throws Exception {
        SqliteDataStore store = new SqliteDataStore(dir.resolve("concurrent.db"));
        try {
            UUID business = UUID.randomUUID();
            store.ensureBusinessExists(business);

            int iterations = 400;
            List<Throwable> failures = new CopyOnWriteArrayList<>();
            CountDownLatch start = new CountDownLatch(1);

            // Writer: inserts businesses on its own connection, mirroring the background import thread.
            Thread writer = new Thread(() -> {
                Connection conn = store.connection();
                try {
                    start.await();
                    for (int i = 0; i < iterations; i++) {
                        try (PreparedStatement ps =
                                 conn.prepareStatement("INSERT OR IGNORE INTO business (id) VALUES (?)")) {
                            ps.setString(1, UUID.randomUUID().toString());
                            ps.executeUpdate();
                        }
                    }
                } catch (Throwable t) {
                    failures.add(t);
                }
            }, "writer");

            // Reader: queries on its own connection, mirroring the JavaFX thread reading concurrently.
            Thread reader = new Thread(() -> {
                Connection conn = store.connection();
                try {
                    start.await();
                    for (int i = 0; i < iterations; i++) {
                        try (Statement st = conn.createStatement();
                             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM business")) {
                            rs.next();
                            rs.getLong(1);
                        }
                    }
                } catch (Throwable t) {
                    failures.add(t);
                }
            }, "reader");

            writer.setDaemon(true);
            reader.setDaemon(true);
            writer.start();
            reader.start();
            start.countDown();
            writer.join(30_000);
            reader.join(30_000);

            assertThat(writer.isAlive()).as("writer thread finished within the timeout").isFalse();
            assertThat(reader.isAlive()).as("reader thread finished within the timeout").isFalse();
            assertThat(failures).isEmpty();
            // Writer's inserts committed (autocommit) and are visible: at least the seed business plus the inserts.
            try (Statement st = store.connection().createStatement();
                 ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM business")) {
                rs.next();
                assertThat(rs.getLong(1)).isGreaterThan(iterations);
            }
        } finally {
            store.close();
        }
    }
}
