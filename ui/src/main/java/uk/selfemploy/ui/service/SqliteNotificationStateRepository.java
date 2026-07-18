package uk.selfemploy.ui.service;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * SQLite-backed {@link NotificationStateStore}. Read/snooze state is upserted per reminder key so it
 * survives restarts, since the reminders themselves are regenerated from deadlines on each launch.
 */
public class SqliteNotificationStateRepository implements NotificationStateStore {

    private final SqliteDataStore dataStore;

    public SqliteNotificationStateRepository() {
        this.dataStore = SqliteDataStore.getInstance();
    }

    @Override
    public void save(String key, boolean read, LocalDateTime snoozeUntil) {
        String sql = "INSERT OR REPLACE INTO notification_state "
            + "(state_key, is_read, snooze_until, updated_at) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = dataStore.connection().prepareStatement(sql)) {
            ps.setString(1, key);
            ps.setInt(2, read ? 1 : 0);
            ps.setString(3, snoozeUntil != null ? snoozeUntil.toString() : null);
            ps.setString(4, Instant.now().toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to persist notification state for " + key, e);
        }
    }

    @Override
    public Map<String, PersistedState> loadAll() {
        Map<String, PersistedState> states = new HashMap<>();
        String sql = "SELECT state_key, is_read, snooze_until FROM notification_state";
        try (PreparedStatement ps = dataStore.connection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                LocalDateTime snoozeUntil = rs.getString("snooze_until") != null
                    ? LocalDateTime.parse(rs.getString("snooze_until")) : null;
                states.put(rs.getString("state_key"),
                    new PersistedState(rs.getInt("is_read") != 0, snoozeUntil));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to load notification state", e);
        }
        return states;
    }
}
