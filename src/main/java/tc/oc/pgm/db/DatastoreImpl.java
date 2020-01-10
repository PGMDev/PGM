package tc.oc.pgm.db;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import tc.oc.pgm.api.Datastore;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.player.Username;
import tc.oc.pgm.api.setting.SettingKey;
import tc.oc.pgm.api.setting.SettingValue;
import tc.oc.pgm.api.setting.Settings;
import tc.oc.pgm.util.UsernameResolver;
import tc.oc.util.logging.ClassLogger;

public class DatastoreImpl implements Datastore {

  private final Logger logger;
  private final Connection connection;

  public DatastoreImpl(File file) throws SQLException {
    this.logger = ClassLogger.get(PGM.get().getLogger(), DatastoreImpl.class);

    try {
      Class.forName("org.sqlite.JDBC"); // Hint maven to shade this class
    } catch (ClassNotFoundException e) {
      throw new SQLException(
          "Could not find SQLite3 driver class (likely due to a jar shading issue)", e);
    }

    final Connection connection =
        DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());
    connection.setAutoCommit(true);
    this.connection = connection;

    initUsername();
    initSettings();
  }

  @Override
  public Username getUsername(UUID id) {
    Username username = null;
    try {
      username = selectUsername(id);
    } catch (SQLException e) {
      logger.log(Level.WARNING, "Could not get username for " + id, e);
    }
    return username == null ? new UsernameImpl(id, null, null) : username;
  }

  private class UsernameImpl implements Username {
    private UUID id;
    private String name;

    private UsernameImpl(UUID id, String name, Date expires) {
      this.id = checkNotNull(id);
      this.name = name;

      if (name == null || expires == null || new Date().after(expires)) {
        UsernameResolver.resolve(id, this::setName);
      }
    }

    @Override
    public UUID getId() {
      return id;
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public boolean setName(@Nullable String name) {
      if (Objects.equals(this.name, name)) return true;

      try {
        updateUsername(id, name);
        this.name = name;
      } catch (SQLException e) {
        logger.log(Level.WARNING, "Could not update username for " + id + " to " + name, e);
      }

      return false;
    }
  }

  private void initUsername() throws SQLException {
    try (final Statement statement = getConnection().createStatement()) {

      statement.addBatch(
          "CREATE TABLE IF NOT EXISTS usernames (id VARCHAR(36) PRIMARY KEY, name VARCHAR(16), expires DATE)");
      statement.addBatch(
          "DELETE FROM usernames WHERE name IS NULL OR expires IS NULL OR DATE('now', 'localtime') - expires >= 60 * 60 * 24 * 30");

      statement.executeBatch();
    }
  }

  private Username selectUsername(UUID id) throws SQLException {
    try (final PreparedStatement statement =
        getConnection()
            .prepareStatement(
                "SELECT name, strftime('%s', expires) FROM usernames WHERE id = ? LIMIT 1")) {
      statement.setString(1, checkNotNull(id).toString());

      try (final ResultSet result = statement.executeQuery()) {
        if (result.next()) {
          final String name = result.getString(1);
          final Date expires = new Date(result.getLong(2) * 1000);

          return new UsernameImpl(id, name, expires);
        }
      }
    }

    return null;
  }

  private void updateUsername(UUID id, @Nullable String name) throws SQLException {
    try (final PreparedStatement statement =
        getConnection()
            .prepareStatement(
                "INSERT OR REPLACE INTO usernames VALUES (?, ?, DATE('now', '+7 day', 'localtime'))")) {
      statement.setString(1, checkNotNull(id).toString());
      statement.setString(2, name);

      statement.executeUpdate();
    }
  }

  @Override
  public Settings getSettings(UUID id) {
    int bit = 0;

    try {
      bit = selectSettings(id);
    } catch (SQLException e) {
      logger.log(Level.WARNING, "Could not get setting for " + id);
    }

    return new SettingsImpl(id, bit);
  }

  private class SettingsImpl implements Settings {
    private final UUID id;
    private int bit;

    private SettingsImpl(UUID id, int bit) {
      this.id = checkNotNull(id);
      if (bit < 0) bit = 0;
      this.bit = bit;
    }

    @Override
    public UUID getId() {
      return id;
    }

    @Override
    public SettingValue getValue(SettingKey key) {
      for (SettingValue value : key.getPossibleValues()) {
        final int mask = bitSettings(value);
        if ((bit & mask) == mask) {
          return value;
        }
      }
      return key.getDefaultValue();
    }

    @Override
    public void setValue(SettingKey key, SettingValue value) {
      try {
        if (bit == 0) {
          insertSettings(id, 0);
        }

        updateSettings(id, key, value);

        this.bit = selectSettings(id);
      } catch (SQLException e) {
        logger.log(
            Level.WARNING, "Could not update settings for " + id + " of " + key + " to " + value);
      }
    }
  }

  private int bitSettings(SettingValue value) {
    return 1 << (checkNotNull(value).ordinal() + 1);
  }

  private void initSettings() throws SQLException {
    try (final Statement statement = getConnection().createStatement()) {

      statement.addBatch(
          "CREATE TABLE IF NOT EXISTS settings (id VARCHAR(36) PRIMARY KEY, bit INTEGER)");
      statement.addBatch("DELETE FROM settings WHERE bit <= 0");

      statement.executeBatch();
    }
  }

  private int selectSettings(UUID id) throws SQLException {
    try (final PreparedStatement statement =
        getConnection().prepareStatement("SELECT bit FROM settings WHERE id = ? LIMIT 1")) {
      statement.setString(1, checkNotNull(id).toString());

      int bit = 0;
      try (final ResultSet result = statement.executeQuery()) {
        if (result.next()) {
          bit = result.getInt(1);
        }
      }

      return bit;
    }
  }

  private void insertSettings(UUID id, int bit) throws SQLException {
    try (final PreparedStatement statement =
        getConnection().prepareStatement("INSERT OR REPLACE INTO settings VALUES (?, ?)")) {
      statement.setString(1, checkNotNull(id).toString());
      statement.setInt(2, bit);

      statement.executeUpdate();
    }
  }

  private void updateSettings(UUID id, SettingKey key, SettingValue value) throws SQLException {
    try (final PreparedStatement statement =
        getConnection()
            .prepareStatement("UPDATE settings SET bit = ((bit & ~?) | ?) WHERE id = ?")) {

      statement.setInt(2, bitSettings(value));
      statement.setString(3, checkNotNull(id).toString());

      for (SettingValue unset : key.getPossibleValues()) {
        statement.setInt(1, bitSettings(unset));
        statement.addBatch();
      }

      statement.executeBatch();
    }
  }

  private Connection getConnection() {
    return connection;
  }

  @Override
  public void shutdown() {
    try {
      getConnection().close();
    } catch (SQLException e) {
    }
  }
}
