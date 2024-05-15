package tc.oc.pgm.command.util;

import io.leangen.geantyref.TypeToken;
import org.incendo.cloud.key.CloudKey;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.setting.SettingKey;
import tc.oc.pgm.rotation.pools.MapPoolType;

public class CommandKeys {

  // Keep the current match in a key, avoids excessive lookups, one in each parser/injector.
  public static final CloudKey<Match> MATCH = CloudKey.of("_pgm_match_", new TypeToken<Match>() {});

  public static final CloudKey<SettingKey> SETTING_KEY =
      CloudKey.of("_pgm_setting_key_param_", new TypeToken<SettingKey>() {});

  public static final CloudKey<MapPoolType> POOL_TYPE =
      CloudKey.of("_pgm_pool_type_param_", new TypeToken<MapPoolType>() {});
}
