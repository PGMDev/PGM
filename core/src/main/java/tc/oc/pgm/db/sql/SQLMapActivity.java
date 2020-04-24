package tc.oc.pgm.db.sql;

import java.sql.SQLException;
import java.util.logging.Level;
import javax.annotation.Nullable;
import tc.oc.pgm.api.map.MapActivity;

public class SQLMapActivity implements MapActivity {
  private final SQLDatabaseDriver driver;
  private final String name;
  private String nextMap;
  private boolean active;

  public SQLMapActivity(
      SQLDatabaseDriver driver, String name, @Nullable String nextMap, boolean active) {
    this.driver = driver;
    this.name = name;
    this.nextMap = nextMap;
    this.active = active;
  }

  @Override
  public String getPoolName() {
    return name;
  }

  @Override
  public String getMapName() {
    return nextMap;
  }

  @Override
  public boolean isActive() {
    return active;
  }

  @Override
  public void update(String nextMap, boolean active) {
    this.nextMap = nextMap;
    this.active = active;

    try {
      driver.updateMapActivity(name, getMapName(), isActive());
    } catch (SQLException e) {
      driver.getLogger().log(Level.WARNING, "Unable to update pool activity for " + name, e);
    }
  }
}
