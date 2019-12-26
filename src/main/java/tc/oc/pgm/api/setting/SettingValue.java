package tc.oc.pgm.api.setting;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Values of a particular {@link SettingKey}, a toggleable setting.
 *
 * @see SettingKey
 */
public enum SettingValue {
  TEAM("chat", "team"), // Only send to members on the player's team
  GLOBAL("chat", "global"), // Send to all players in the same match
  ADMIN("chat", "admin"); // Send to all server operators

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
}
