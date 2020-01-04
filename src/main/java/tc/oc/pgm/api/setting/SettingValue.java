package tc.oc.pgm.api.setting;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import tc.oc.util.StringUtils;

/**
 * Values of a particular {@link SettingKey}, a toggleable setting.
 *
 * @see SettingKey
 */
public enum SettingValue {
  CHAT_TEAM("chat", "team"), // Only send to members on the player's team
  CHAT_GLOBAL("chat", "global"), // Send to all players in the same match
  CHAT_ADMIN("chat", "admin"), // Send to all server operators

  DEATH_OWN("death", "own"), // Only send death messages involving self
  DEATH_ALL("death", "all"), // Send all death messages, highlight your own

  PICKER_AUTO("picker", "auto"), // Display after cycle, or with permissions.
  PICKER_ON("picker", "on"), // Display the picker GUI always
  PICKER_OFF("picker", "off"), // Never display the picker GUI

  JOIN_MESSAGES_AUTO("join-messages", "auto"), // Send join messages without spamming
  JOIN_MESSAGES_ALL("join-messages", "all"), // Send all join messages
  JOIN_MESSAGES_NONE("join-messages", "none"), // Never send join messages
  ;

  private final String key;
  private final String name;

  SettingValue(String group, String name) {
    this.key = checkNotNull(group);
    this.name = checkNotNull(name);
  }

  /**
   * Get the parent {@link SettingKey}, which defines its mutual-exclusion members.
   *
   * @return The parent {@link SettingKey}.
   */
  public SettingKey getKey() {
    return SettingKey.valueOf(key.toUpperCase());
  }

  /**
   * Get the name of {@link SettingValue}.
   *
   * @return The name.
   */
  public String getName() {
    return name;
  }

  @Override
  public String toString() {
    return getName();
  }

  public static SettingValue search(SettingKey key, @Nullable String query) {
    final SettingValue value =
        StringUtils.bestFuzzyMatch(
            query,
            Stream.of(SettingValue.values())
                .filter(entry -> entry.getKey().equals(key))
                .collect(Collectors.toMap(SettingValue::getName, Function.identity())),
            0.6);
    if (value == null) {
      return key.getDefaultValue();
    }
    return value;
  }
}
