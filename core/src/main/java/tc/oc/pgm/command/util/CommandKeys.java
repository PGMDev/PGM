package tc.oc.pgm.command.util;

import cloud.commandframework.keys.CloudKey;
import cloud.commandframework.keys.SimpleCloudKey;
import io.leangen.geantyref.TypeToken;
import java.util.LinkedList;
import tc.oc.pgm.api.setting.SettingKey;
import tc.oc.pgm.rotation.pools.MapPoolType;

public class CommandKeys {

  // Variable to access the current input queue
  public static final CloudKey<LinkedList<String>> INPUT_QUEUE =
      SimpleCloudKey.of("_pgm_input_queue_", new TypeToken<LinkedList<String>>() {});

  public static final CloudKey<SettingKey> SETTING_KEY =
      SimpleCloudKey.of("_pgm_setting_key_param_", new TypeToken<SettingKey>() {});

  public static final CloudKey<MapPoolType> POOL_TYPE =
      SimpleCloudKey.of("_pgm_pool_type_param_", new TypeToken<MapPoolType>() {});
}
