package tc.oc.pgm.db;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.sql.*;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.db.Datastore;
import tc.oc.pgm.api.db.Username;
import tc.oc.pgm.util.UsernameResolver;
import tc.oc.util.logging.ClassLogger;

public class DatastoreImpl implements Datastore {

  private final Logger logger;
  private final AtomicReference<Connection> connection;

  public DatastoreImpl(File file) throws SQLException {
    this.logger = ClassLogger.get(PGM.get().getLogger(), DatastoreImpl.class);

    final Connection connection =
        DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());
    connection.setAutoCommit(true);
    connection
        .prepareStatement(
            "CREATE TABLE IF NOT EXISTS usernames (id VARCHAR(36) PRIMARY KEY, name VARCHAR(16), expires DATE);")
        .execute();

    this.connection = new AtomicReference<>(connection);
  }

  @Override
  public Username getUsername(UUID uuid) {
    try {
      final PreparedStatement query =
          connection
              .get()
              .prepareStatement("SELECT name, expires FROM usernames WHERE id = ? LIMIT 1;");
      query.setString(1, uuid.toString());

      final ResultSet result = query.executeQuery();
      if (result.next()) {
        final String name = result.getString(1);
        final Date expires = new Date(result.getLong(2) * 1000);
        result.close();

        return new UsernameImpl(uuid, name, expires);
      }

      final PreparedStatement insert =
          connection
              .get()
              .prepareStatement(
                  "INSERT INTO usernames VALUES (?, NULL, DATE('now', 'localtime'));");
      insert.setString(1, uuid.toString());
      insert.executeUpdate();

      return getUsername(uuid);
    } catch (SQLException e) {
      logger.log(Level.WARNING, "Could not fetch username for " + uuid, e);
      return new UsernameImpl(uuid, null, new java.util.Date());
    }
  }

  private class UsernameImpl implements Username {
    private UUID id;
    private String name;

    private UsernameImpl(UUID id, String name, java.util.Date expires) {
      this.id = checkNotNull(id);
      this.name = name;

      if (name == null || new Date().after(expires)) {
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
        final PreparedStatement statement =
            connection
                .get()
                .prepareStatement(
                    "UPDATE usernames SET name = ?, expires = DATE('now', '+7 day', 'localtime') WHERE id = ?;");
        statement.setString(1, name);
        statement.setString(2, id.toString());

        if (statement.executeUpdate() > 0) {
          this.name = name;
          return true;
        }
      } catch (SQLException e) {
        logger.log(Level.WARNING, "Could not update username for " + id + " to " + name, e);
      }

      return false;
    }
  }
}
