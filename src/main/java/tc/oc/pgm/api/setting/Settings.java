package tc.oc.pgm.api.setting;

import java.util.UUID;

/**
 * Feature flags, also know as settings, that players can enable, toggle, or disable.
 *
 * @see SettingValue
 */
public interface Settings {

  /**
   * Get the unique {@link UUID} of the player.
   *
   * @return The unique {@link UUID}.
   */
  UUID getId();

  /**
   * Get the enabled {@link SettingValue} for a {@link SettingKey}.
   *
   * <p>If the {@link SettingKey} is not explicitly defined, the default {@link SettingValue} should
   * be provided.
   *
   * @param key The {@link SettingKey} to lookup.
   * @return The {@link SettingValue}, at least returning the default.
   */
  SettingValue getValue(SettingKey key);

  /**
   * Set the new {@link SettingValue} for a {@link SettingKey}.
   *
   * @param key The {@link SettingKey} to change.
   * @param value The {@link SettingValue} to set.
   */
  void setValue(SettingKey key, SettingValue value);

  /**
   * Change the {@link SettingKey} to the next possible {@link SettingValue}.
   *
   * @param key The {@link SettingKey} to toggle.
   */
  default void toggleValue(SettingKey key) {
    final SettingValue value = getValue(key);
    final SettingValue[] values = key.getPossibleValues();

    for (int i = 0; i < values.length - 1; i++) {
      if (values[i] == value) {
        setValue(key, values[i + 1]);
        return;
      }
    }

    resetValue(key);
  }

  /**
   * Reset the {@link SettingKey} to the default {@link SettingValue}.
   *
   * @param key The {@link SettingKey} to reset.
   */
  default void resetValue(SettingKey key) {
    setValue(key, key.getDefaultValue());
  }
}
