package tc.oc.pgm.db;

import static tc.oc.pgm.db.SettingsImpl.bitSettings;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import tc.oc.pgm.api.Datastore;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.map.MapActivity;
import tc.oc.pgm.api.player.Username;
import tc.oc.pgm.api.setting.SettingKey;
import tc.oc.pgm.api.setting.SettingValue;
import tc.oc.pgm.api.setting.Settings;
import tc.oc.pgm.util.ClassLogger;

public class SQLDatastore implements Datastore {

  private final Logger logger;
  private final Connection connection;
  private final ExecutorService executorService;

  public SQLDatastore(@Nullable String uri, ExecutorService executorService) throws SQLException {
    this.logger = ClassLogger.get(PGM.get().getLogger(), getClass());
    this.executorService = executorService;
    this.connection = getConnection(uri);

    initUsername();
    initSettings();
    initMapActivity();
  }

  private static Connection getConnection(@Nullable String uri) throws SQLException {
    if (uri == null || uri.isEmpty()) {
      uri = "sqlite:" + new File(PGM.get().getDataFolder(), "pgm.db").getAbsolutePath();
    }

    if (!uri.startsWith("jdbc:")) {
      uri = "jdbc:" + uri;
    }

    try {
      if (uri.startsWith("jdbc:sqlite")) {
        Class.forName("org.sqlite.JDBC");
      } else if (uri.startsWith("jdbc:mysql")) {
        Class.forName("com.mysql.jdbc.Driver");
      }
    } catch (ClassNotFoundException e) {
      throw new SQLException(
          "Unable to load database driver class (your server likely does not support your database)");
    }

    final Connection connection = DriverManager.getConnection(uri);
    connection.setAutoCommit(true);

    return connection;
  }

  private class SQLUsername extends UsernameImpl {

    SQLUsername(UUID id, @Nullable String name) {
      super(id, name);
    }

    @Override
    public void setName(@Nullable String name) {
      super.setName(name);
      if (name == null) return;
      executorService.submit(() -> updateUsername(this));
    }
  }

  @Override
  public Username getUsername(UUID id) {
    final Username username = new SQLUsername(id, null);
    executorService.submit(() -> selectUsername(username));
    return username;
  }

  private void initUsername() throws SQLException {
    try (final Statement statement = getConnection().createStatement()) {

      statement.addBatch(
          "CREATE TABLE IF NOT EXISTS usernames (id VARCHAR(36) PRIMARY KEY, name VARCHAR(16), expires LONG)");

      statement.executeBatch();
    }
  }

  private Username selectUsername(Username username) throws SQLException {
    try (final PreparedStatement statement =
        getConnection()
            .prepareStatement("SELECT name, expires FROM usernames WHERE id = ? LIMIT 1")) {
      statement.setString(1, username.getId().toString());

      try (final ResultSet result = statement.executeQuery()) {
        if (result.next()) {
          final String name = result.getString(1);
          final long expires = result.getLong(2);

          username.setName(name);
          if (expires < System.currentTimeMillis()) {
            username.setName(null); // Asks to refresh the username
          }
        }
      }
    }
    return username;
  }

  private Username updateUsername(Username username) throws SQLException {
    final String name = username.getName();
    if (name == null) return username; // Do not update if there is no name

    try (final PreparedStatement statement =
        getConnection().prepareStatement("REPLACE INTO usernames VALUES (?, ?, ?)")) {
      statement.setString(1, username.getId().toString());
      statement.setString(2, name);
      statement.setLong(3, System.currentTimeMillis() + Duration.ofDays(7).toMillis());

      statement.executeUpdate();
    }

    return username;
  }

  private class SQLSettings extends SettingsImpl {

    SQLSettings(UUID id, long bit) {
      super(id, bit);
    }

    @Override
    public void setValue(SettingKey key, SettingValue value) {
      final long bit = getBit(); // Get bit value before super changes
      super.setValue(key, value);

      if (bit == 0) {
        executorService.submit(
            () -> {
              insertSettings(this);
              return updateSettings(this, key, value);
            });
      } else {
        executorService.submit(() -> updateSettings(this, key, value));
      }
    }
  }

  @Override
  public Settings getSettings(UUID id) {
    final SettingsImpl settings = new SQLSettings(id, 0);
    executorService.submit(() -> selectSettings(settings));
    return settings;
  }

  private void initSettings() throws SQLException {
    try (final Statement statement = getConnection().createStatement()) {

      statement.addBatch(
          "CREATE TABLE IF NOT EXISTS settings (id VARCHAR(36) PRIMARY KEY, bit LONG)");
      statement.addBatch("DELETE FROM settings WHERE bit <= 0");

      statement.executeBatch();
    }
  }

  private Settings selectSettings(SettingsImpl settings) throws SQLException {
    try (final PreparedStatement statement =
        getConnection().prepareStatement("SELECT bit FROM settings WHERE id = ? LIMIT 1")) {
      statement.setString(1, settings.getId().toString());

      try (final ResultSet result = statement.executeQuery()) {
        if (result.next()) {
          settings.setBit(result.getLong(1));
        }
      }
    }
    return settings;
  }

  private void insertSettings(Settings settings) throws SQLException {
    try (final PreparedStatement statement =
        getConnection().prepareStatement("REPLACE INTO settings VALUES (?, ?)")) {
      statement.setString(1, settings.getId().toString());
      statement.setLong(2, 0);

      statement.executeUpdate();
    }
  }

  private Settings updateSettings(Settings settings, SettingKey key, SettingValue value)
      throws SQLException {
    try (final PreparedStatement statement =
        getConnection()
            .prepareStatement("UPDATE settings SET bit = ((bit & ~?) | ?) WHERE id = ?")) {

      statement.setLong(2, bitSettings(value));
      statement.setString(3, settings.getId().toString());

      for (SettingValue unset : key.getPossibleValues()) {
        if (unset == value) continue;
        statement.setLong(1, bitSettings(unset));
        statement.addBatch();
      }

      statement.executeBatch();
    }
    return settings;
  }

  @Override
  public MapActivity getMapActivity(String name) {
    try {
      // FIXME: Unlike other datastore methods, all callers of map activity depend on a blocking
      // response.
      // This breaks the ideal contract of Datastore and can be addressed later.
      return selectMapActivity(name);
    } catch (SQLException e) {
      logger.log(Level.WARNING, "Unable to find map activity: " + name);
      return new SQLMapActivity(name, null, false);
    }
  }

  private class SQLMapActivity extends MapActivityImpl {

    SQLMapActivity(String poolName, @Nullable String mapName, boolean active) {
      super(poolName, mapName, active);
    }

    @Override
    public void update(@Nullable String nextMap, boolean active) {
      super.update(nextMap, active);

      try {
        updateMapActivity(getPoolName(), getMapName(), isActive());
      } catch (SQLException e) {
        logger.log(Level.WARNING, "Unable to update pool activity: " + this, e);
      }
    }
  }

  private void initMapActivity() throws SQLException {
    try (final Statement statement = getConnection().createStatement()) {
      statement.addBatch(
          "CREATE TABLE IF NOT EXISTS pools (name VARCHAR(255) PRIMARY KEY, next_map VARCHAR(255), last_active BOOLEAN)");
      statement.executeBatch();
    }
  }

  private MapActivity selectMapActivity(String poolName) throws SQLException {
    try (final PreparedStatement statement =
        getConnection().prepareStatement("SELECT * FROM pools WHERE name = ? LIMIT 1")) {
      statement.setString(1, poolName);

      try (final ResultSet result = statement.executeQuery()) {
        if (result.next()) {
          final String nextMap = result.getString(2);
          final boolean active = result.getBoolean(3);

          return new SQLMapActivity(poolName, nextMap, active);
        }
      }
    }

    return new SQLMapActivity(poolName, null, false);
  }

  private void updateMapActivity(String name, @Nullable String nextMap, boolean active)
      throws SQLException {
    try (final PreparedStatement statement =
        getConnection().prepareStatement("REPLACE INTO pools VALUES (?, ?, ?)")) {
      statement.setString(1, name);
      statement.setString(2, nextMap);
      statement.setBoolean(3, active);

      statement.executeUpdate();
    }
  }

  private Connection getConnection() throws SQLException {
    if (connection.isClosed()) {
      throw new SQLException("SQL connection is closed");
    }

    return connection;
  }

  @Override
  public void close() {
    try {
      getConnection().close();
    } catch (SQLException e) {
      logger.log(Level.WARNING, "Unable to close SQL connection", e);
    }
  }
}
