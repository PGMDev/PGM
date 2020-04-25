package tc.oc.pgm.api.setting;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import tc.oc.pgm.api.PGM;

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
   * Set the new {@link SettingValue} for a {@link SettingKey} synchronously.
   *
   * @param key The {@link SettingKey} to change.
   * @param value The {@link SettingValue} to set.
   */
  void setValueSync(SettingKey key, SettingValue value);

  /**
   * Set the new {@link SettingValue} for a {@link SettingKey} asynchronously.
   *
   * @param key The {@link SettingKey} to change.
   * @param value The {@link SettingValue} to set.
   */
  void setValue(SettingKey key, SettingValue value);

  /**
   * Change the {@link SettingKey} to the next possible {@link SettingValue} synchronously.
   *
   * @param key The {@link SettingKey} to toggle.
   */
  default void toggleValueSync(SettingKey key) {
    final SettingValue value = getValue(key);
    final SettingValue[] values = key.getPossibleValues();

    for (int i = 0; i < values.length - 1; i++) {
      if (values[i] == value) {
        setValueSync(key, values[i + 1]);
        return;
      }
    }

    resetValueSync(key);
  }

  /**
   * Change the {@link SettingKey} to the next possible {@link SettingValue} asynchronously.
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
   * Reset the {@link SettingKey} to the default {@link SettingValue} synchronously.
   *
   * @param key The {@link SettingKey} to reset.
   */
  default void resetValueSync(SettingKey key) {
    setValueSync(key, key.getDefaultValue());
  }

  /**
   * Reset the {@link SettingKey} to the default {@link SettingValue} asynchronously.
   *
   * @param key The {@link SettingKey} to reset.
   */
  default void resetValue(SettingKey key) {
    PGM.get().getAsyncExecutor().schedule(() -> resetValueSync(key), 0, TimeUnit.SECONDS);
  }
}
