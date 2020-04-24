package tc.oc.pgm.db.sql;

import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import tc.oc.pgm.api.map.MapActivity;
import tc.oc.pgm.api.player.Username;
import tc.oc.pgm.api.setting.SettingKey;
import tc.oc.pgm.api.setting.SettingValue;

public interface SQLDatabaseDriver {

  void initUsernames() throws SQLException;

  Username selectUsername(UUID id) throws SQLException;

  void updateUsername(UUID id, String name) throws SQLException;

  void initSettings() throws SQLException;

  int selectSettings(UUID id) throws SQLException;

  void insertSettings(UUID id, int bit) throws SQLException;

  void updateSettings(UUID id, SettingKey key, SettingValue value) throws SQLException;

  void initMapActivity() throws SQLException;

  MapActivity selectMapActivity(String name) throws SQLException;

  void updateMapActivity(String name, @Nullable String nextMap, boolean active) throws SQLException;

  Logger getLogger();
}
