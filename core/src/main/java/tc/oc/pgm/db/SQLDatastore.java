package tc.oc.pgm.db;

import static tc.oc.pgm.util.Assert.assertNotNull;
import static tc.oc.pgm.util.player.PlayerComponent.player;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.Datastore;
import tc.oc.pgm.api.map.MapActivity;
import tc.oc.pgm.api.player.Username;
import tc.oc.pgm.api.setting.SettingKey;
import tc.oc.pgm.api.setting.SettingValue;
import tc.oc.pgm.api.setting.Settings;
import tc.oc.pgm.util.concurrent.ThreadSafeConnection;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.nms.NMSHacks;
import tc.oc.pgm.util.skin.Skin;
import tc.oc.pgm.util.text.TextParser;
import tc.oc.pgm.util.usernames.UsernameResolver;
import tc.oc.pgm.util.usernames.UsernameResolvers;

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

  private class SQLUsername implements Username {
    private final Duration ONE_WEEK = Duration.ofDays(7);

    private final UUID id;
    private String name;
    private Instant validUntil;

    SQLUsername(UUID id) {
      this.id = assertNotNull(id, "username id is null");
      // It's better than no name at all until we resolve to something better
      // Note: it appears like offline uuid -> name does nothing in sportpaper
      if (Bukkit.isPrimaryThread()) name = NMSHacks.getPlayerName(id);
      UsernameResolvers.resolve(id).thenAccept(this::setName);
    }

    @Override
    public UUID getId() {
      return id;
    }

    @Override
    public String getNameLegacy() {
      return name;
    }

    @Override
    public Component getName(NameStyle style) {
      return player(Bukkit.getPlayer(id), name, style);
    }

    protected void setName(UsernameResolver.UsernameResponse response) {
      // A name is provided and either we know no name, or it's more recent
      if (response.getUsername() != null
          && (this.name == null || response.getValidUntil().isAfter(this.validUntil))) {
        this.name = response.getUsername();
        this.validUntil = response.getValidUntil();

        // Only update names with about over a week of validity
        if (response.getSource() != SqlUsernameResolver.class
            && validUntil.isAfter(Instant.now().plus(ONE_WEEK))) {
          submitQuery(new UpdateQuery());
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
        statement.setString(1, id.toString());
        statement.setString(2, name);
        statement.setLong(3, validUntil.toEpochMilli());
        statement.executeUpdate();
      }
    }
  }

  @Override
  public Username getUsername(UUID id) {
    return new SQLUsername(id);
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
