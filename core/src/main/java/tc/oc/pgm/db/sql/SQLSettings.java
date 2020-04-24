package tc.oc.pgm.db.sql;

import static com.google.common.base.Preconditions.checkNotNull;

import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Level;
import tc.oc.pgm.api.setting.SettingKey;
import tc.oc.pgm.api.setting.SettingValue;
import tc.oc.pgm.api.setting.Settings;

public class SQLSettings implements Settings {

  private final SQLDatabaseDriver driver;
  private final UUID id;
  private int bit;

  public SQLSettings(SQLDatabaseDriver driver, UUID id, int bit) {
    this.driver = driver;
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
        driver.insertSettings(id, 0);
      }

      driver.updateSettings(id, key, value);

      this.bit = driver.selectSettings(id);
    } catch (SQLException e) {
      driver
          .getLogger()
          .log(
              Level.WARNING, "Could not update settings for " + id + " of " + key + " to " + value);
    }
  }

  public static int bitSettings(SettingValue value) {
    return 1 << (checkNotNull(value).ordinal() + 1);
  }
}
