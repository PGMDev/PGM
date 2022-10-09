package tc.oc.pgm.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.Datastore;
import tc.oc.pgm.api.map.MapActivity;
import tc.oc.pgm.api.player.Username;
import tc.oc.pgm.api.setting.SettingKey;
import tc.oc.pgm.api.setting.SettingValue;
import tc.oc.pgm.api.setting.Settings;
import tc.oc.pgm.util.concurrent.ThreadSafeConnection;
import tc.oc.pgm.util.skin.Skin;
import tc.oc.pgm.util.text.TextParser;

public class SQLDatastore extends ThreadSafeConnection implements Datastore {

  public SQLDatastore(String uri, int maxConnections) throws SQLException {
    super(() -> TextParser.parseSqlConnection(uri), maxConnections);

    submitQuery(
        () ->
            "CREATE TABLE IF NOT EXISTS usernames (id VARCHAR(36) PRIMARY KEY, name VARCHAR(16), expires LONG)");
    submitQuery(() -> "CREATE TABLE IF NOT EXISTS settings (id VARCHAR(36) PRIMARY KEY, bit LONG)");
    submitQuery(
        () ->
            "CREATE TABLE IF NOT EXISTS pools (name VARCHAR(255) PRIMARY KEY, next_map VARCHAR(255), last_active BOOLEAN)");
  }

  private class SQLUsername extends UsernameImpl {

    private volatile boolean queried;

    SQLUsername(UUID id, @Nullable String name) {
      super(id, name);
    }

    @Override
    public String getNameLegacy() {
      String name = super.getNameLegacy();

      // Since there can be hundreds of names, only query when requested.
      if (!queried && name == null) {
        queried = true;
        submitQuery(new SelectQuery());
      }

      return name;
    }

    @Override
    public void setName(@Nullable String name) {
      super.setName(name);

      if (name != null) {
        submitQuery(new UpdateQuery());
      }
    }

    private class SelectQuery implements Query {
      @Override
      public String getFormat() {
        return "SELECT name, expires FROM usernames WHERE id = ? LIMIT 1";
      }

      @Override
      public void query(PreparedStatement statement) throws SQLException {
        statement.setString(1, getId().toString());

        try (final ResultSet result = statement.executeQuery()) {
          if (!result.next()) return;

          setName(result.getString(1));

          if (result.getLong(2) < System.currentTimeMillis()) {
            setName(null);
          }
        }
      }
    }

    private class UpdateQuery implements Query {
      @Override
      public String getFormat() {
        return "REPLACE INTO usernames VALUES (?, ?, ?)";
      }

      @Override
      public void query(PreparedStatement statement) throws SQLException {
        statement.setString(1, getId().toString());
        statement.setString(2, getNameLegacy());

        // Pick a random expiration time between 1 and 2 weeks
        statement.setLong(
            3,
            System.currentTimeMillis() + Duration.ofDays(7 + (int) (Math.random() * 7)).toMillis());

        statement.executeUpdate();
      }
    }
  }

  @Override
  public Username getUsername(UUID id) {
    return new SQLUsername(id, null);
  }

  private class SQLSettings extends SettingsImpl {

    SQLSettings(UUID id, long bit) {
      super(id, bit);
      submitQuery(new SelectQuery());
    }

    @Override
    public void setValue(SettingKey key, SettingValue value) {
      final long oldBit = getBit();
      super.setValue(key, value);

      if (oldBit == getBit()) return;
      submitQuery(oldBit <= 0 ? new InsertQuery(value) : new UpdateQuery(value));
    }

    private class SelectQuery implements Query {
      @Override
      public String getFormat() {
        return "SELECT bit FROM settings WHERE id = ? LIMIT 1";
      }

      @Override
      public void query(PreparedStatement statement) throws SQLException {
        statement.setString(1, getId().toString());

        try (final ResultSet result = statement.executeQuery()) {
          if (result.next()) {
            setBit(result.getLong(1));
          }
        }
      }
    }

    private class InsertQuery implements Query {

      private final SettingValue value;

      private InsertQuery(SettingValue value) {
        this.value = value;
      }

      @Override
      public String getFormat() {
        return "REPLACE INTO settings VALUES (?, ?)";
      }

      @Override
      public void query(PreparedStatement statement) throws SQLException {
        statement.setString(1, getId().toString());
        statement.setLong(2, bitSettings(value));

        statement.executeUpdate();
      }
    }

    private class UpdateQuery implements Query {

      private final SettingValue value;

      private UpdateQuery(SettingValue value) {
        this.value = value;
      }

      @Override
      public String getFormat() {
        return "UPDATE settings SET bit = ((bit & ~?) | ?) WHERE id = ?";
      }

      @Override
      public void query(PreparedStatement statement) throws SQLException {
        statement.setLong(2, bitSettings(value));
        statement.setString(3, getId().toString());

        for (SettingValue unset : value.getKey().getPossibleValues()) {
          if (unset == value) continue;
          statement.setLong(1, bitSettings(unset));
          statement.addBatch();
        }

        statement.executeBatch();
      }
    }
  }

  @Override
  public Settings getSettings(UUID id) {
    return new SQLSettings(id, 0);
  }

  // Skins are only stored in the cache
  /** @see CacheDatastore */
  @Override
  public void setSkin(UUID uuid, Skin skin) {}

  @Override
  public Skin getSkin(UUID uuid) {
    return Skin.EMPTY;
  }

  @Override
  public MapActivity getMapActivity(String name) {
    return new SQLMapActivity(name, null, false);
  }

  private class SQLMapActivity extends MapActivityImpl {

    SQLMapActivity(String poolName, @Nullable String mapName, boolean active) {
      super(poolName, mapName, active);
      try {
        submitQuery(new SelectQuery()).get();
      } catch (InterruptedException | ExecutionException e) {
        e.printStackTrace();
      }
    }

    @Override
    public void update(@Nullable String nextMap, boolean active) {
      super.update(nextMap, active);
      submitQuery(new UpdateQuery());
    }

    private class SelectQuery implements Query {

      @Override
      public String getFormat() {
        return "SELECT * FROM pools WHERE name = ? LIMIT 1";
      }

      @Override
      public void query(PreparedStatement statement) throws SQLException {
        statement.setString(1, getPoolName());

        try (final ResultSet result = statement.executeQuery()) {
          if (result.next()) {
            update(result.getString(2), result.getBoolean(3));
          } else {
            update(null, false);
          }
        }
      }
    }

    private class UpdateQuery implements Query {

      @Override
      public String getFormat() {
        return "REPLACE INTO pools VALUES (?, ?, ?)";
      }

      @Override
      public void query(PreparedStatement statement) throws SQLException {
        statement.setString(1, getPoolName());
        statement.setString(2, getMapName());
        statement.setBoolean(3, isActive());

        statement.executeUpdate();
      }
    }
  }
}
