package tc.oc.pgm.db.sql;

import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import tc.oc.pgm.api.Datastore;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.map.MapActivity;
import tc.oc.pgm.api.player.Username;
import tc.oc.pgm.api.setting.Settings;
import tc.oc.pgm.util.ClassLogger;

public class SQLDatastore implements Datastore {

  private final Logger logger;
  private final SQLDatabaseDriver driver;

  public SQLDatastore(SQLDatabaseDriver driver) throws SQLException {
    this.logger = ClassLogger.get(PGM.get().getLogger(), SQLDatastore.class);
    this.driver = driver;

    driver.initUsernames();
    driver.initSettings();
    driver.initMapActivity();
  }

  @Override
  public Username getUsername(UUID id) {
    Username username = null;
    try {
      username = driver.selectUsername(id);
    } catch (SQLException e) {
      logger.log(Level.WARNING, "Could not get username for " + id, e);
    }

    return username == null ? new SQLUsername(driver, id, null, null) : username;
  }

  @Override
  public Settings getSettings(UUID id) {
    int bit = 0;

    try {
      bit = driver.selectSettings(id);
    } catch (SQLException e) {
      logger.log(Level.WARNING, "Could not get setting for " + id);
    }

    return new SQLSettings(driver, id, bit);
  }

  @Override
  public MapActivity getMapActivity(String name) {
    MapActivity activity = null;

    try {
      activity = driver.selectMapActivity(name);
    } catch (SQLException e) {
      logger.log(
          Level.WARNING,
          "Could not find pool activity for " + name + ". Creating default data now",
          e);
    }

    if (activity == null) {
      activity = new SQLMapActivity(driver, name, null, false);
    }

    return activity;
  }
}
