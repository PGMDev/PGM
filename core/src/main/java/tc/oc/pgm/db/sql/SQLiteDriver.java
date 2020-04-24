package tc.oc.pgm.db.sql;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.UUID;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.map.MapActivity;
import tc.oc.pgm.api.player.Username;
import tc.oc.pgm.api.setting.SettingKey;
import tc.oc.pgm.api.setting.SettingValue;
import tc.oc.pgm.util.ClassLogger;

public class SQLiteDriver implements SQLDatabaseDriver {

  private final Logger logger;
  private final Connection connection;

  public SQLiteDriver(File file) throws SQLException {
    this.logger = ClassLogger.get(PGM.get().getLogger(), SQLiteDriver.class);

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

    initUsernames();
    initSettings();
    initMapActivity();
  }

  @Override
  public void initUsernames() throws SQLException {
    try (final Statement statement = getConnection().createStatement()) {

      statement.addBatch(
          "CREATE TABLE IF NOT EXISTS usernames (id VARCHAR(36) PRIMARY KEY, name VARCHAR(16), expires DATE)");
      statement.addBatch(
          "DELETE FROM usernames WHERE name IS NULL OR expires IS NULL OR DATE('now', 'localtime') - expires >= 60 * 60 * 24 * 30");

      statement.executeBatch();
    }
  }

  @Override
  public Username selectUsername(UUID id) throws SQLException {
    try (final PreparedStatement statement =
        getConnection()
            .prepareStatement(
                "SELECT name, strftime('%s', expires) FROM usernames WHERE id = ? LIMIT 1")) {
      statement.setString(1, checkNotNull(id).toString());

      try (final ResultSet result = statement.executeQuery()) {
        if (result.next()) {
          final String name = result.getString(1);
          final Date expires = new Date(result.getLong(2) * 1000);

          return new SQLUsername(this, id, name, expires);
        }
      }
    }

    return null;
  }

  @Override
  public void updateUsername(UUID id, String name) throws SQLException {
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
  public void initSettings() throws SQLException {
    try (final Statement statement = getConnection().createStatement()) {

      statement.addBatch(
          "CREATE TABLE IF NOT EXISTS settings (id VARCHAR(36) PRIMARY KEY, bit INTEGER)");
      statement.addBatch("DELETE FROM settings WHERE bit <= 0");

      statement.executeBatch();
    }
  }

  @Override
  public int selectSettings(UUID id) throws SQLException {
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

  @Override
  public void insertSettings(UUID id, int bit) throws SQLException {
    try (final PreparedStatement statement =
        getConnection().prepareStatement("INSERT OR REPLACE INTO settings VALUES (?, ?)")) {
      statement.setString(1, checkNotNull(id).toString());
      statement.setInt(2, bit);

      statement.executeUpdate();
    }
  }

  @Override
  public void updateSettings(UUID id, SettingKey key, SettingValue value) throws SQLException {
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

  private int bitSettings(SettingValue value) {
    return 1 << (checkNotNull(value).ordinal() + 1);
  }

  @Override
  public void initMapActivity() throws SQLException {
    try (final Statement statement = getConnection().createStatement()) {
      statement.addBatch(
          "CREATE TABLE IF NOT EXISTS pools (name VARCHAR(255) PRIMARY KEY, next_map VARCHAR(255), last_active BOOLEAN)");
      statement.executeBatch();
    }
  }

  @Override
  public MapActivity selectMapActivity(String name) throws SQLException {
    MapActivity activity = null;

    try (final PreparedStatement statement =
        getConnection().prepareStatement("SELECT * FROM pools WHERE name = ? LIMIT 1")) {
      statement.setString(1, checkNotNull(name));

      try (final ResultSet result = statement.executeQuery()) {
        if (result.next()) {
          final String next_map = result.getString(2);
          final boolean active = result.getBoolean(3);

          return new SQLMapActivity(this, name, next_map, active);
        }
      }
    }
    return activity;
  }

  @Override
  public void updateMapActivity(String name, @Nullable String nextMap, boolean active)
      throws SQLException {
    try (final PreparedStatement statement =
        getConnection().prepareStatement("INSERT OR REPLACE INTO pools VALUES (?, ?, ?)")) {
      statement.setString(1, name);
      statement.setString(2, nextMap);
      statement.setBoolean(3, active);
      statement.executeUpdate();
    }
  }

  @Override
  public Logger getLogger() {
    return logger;
  }

  private Connection getConnection() {
    return connection;
  }
}
