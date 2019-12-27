package tc.oc.pgm.api.setting;

import static tc.oc.pgm.api.setting.SettingValue.*;

/**
 * A toggleable setting with various possible {@link SettingValue}s.
 *
 * @see SettingValue
 */
public enum SettingKey {
  CHAT("chat", CHAT_TEAM, CHAT_GLOBAL, CHAT_ADMIN), // Changes the default chat channel
  DEATH("death", DEATH_OWN, DEATH_ALL); // Changes which death messages are seen

  private final String name;
  private final SettingValue[] values;

  SettingKey(String name, SettingValue... values) {
    this.name = name;
    this.values = values;
  }

  /**
   * Get the name of the {@link SettingKey}.
   *
   * @return The name.
   */
  public String getName() {
    return name;
  }

  /**
   * Get a list of the possible {@link SettingValue}s.
   *
   * @return A array of {@link SettingValue}s, sorted by defined order.
   */
  public SettingValue[] getPossibleValues() {
    return values;
  }

  /**
   * Get the default {@link SettingValue}, which should always be defined first.
   *
   * @return The default {@link SettingValue}.
   */
  public SettingValue getDefaultValue() {
    return getPossibleValues()[0];
  }

  @Override
  public String toString() {
    return getName();
  }
}
