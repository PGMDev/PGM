package tc.oc.pgm.db.sql;

import static com.google.common.base.Preconditions.checkNotNull;

import java.sql.SQLException;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;
import javax.annotation.Nullable;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.player.Username;
import tc.oc.pgm.util.UsernameResolver;

public class SQLUsername implements Username {
  private final SQLDatabaseDriver driver;
  private final UUID id;
  private String name;

  public SQLUsername(SQLDatabaseDriver driver, UUID id, String name, Date expires) {
    this.driver = driver;
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
  public boolean setNameSync(@Nullable String name) {
    if (Objects.equals(this.name, name)) return true;

    try {
      driver.updateUsername(id, name);
      this.name = name;
    } catch (SQLException e) {
      driver
          .getLogger()
          .log(Level.WARNING, "Could not update username for " + id + " to " + name, e);
    }

    return false;
  }

  @Override
  public void setName(@Nullable String name, @Nullable Consumer<Boolean> callback) {
    if (Objects.equals(this.name, name)) {
      callback.accept(true);
      return;
    }

    this.name = name;
    PGM.get()
        .getAsyncExecutor()
        .schedule(
            () -> {
              try {
                driver.updateUsername(id, name);
              } catch (SQLException e) {
                driver
                    .getLogger()
                    .log(Level.WARNING, "Could not update username for " + id + " to " + name, e);
              }
            },
            0,
            TimeUnit.SECONDS);
  }
}
