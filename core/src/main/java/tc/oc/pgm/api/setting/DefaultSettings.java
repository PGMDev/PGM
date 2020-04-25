package tc.oc.pgm.api.setting;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.UUID;

/**
 * Default offline settings implementation. Used for a player's settings in the period where their
 * online settings haven't been fetched yet.
 */
public class DefaultSettings implements Settings {

  private final UUID id;
  private int bit;

  public DefaultSettings(UUID id) {
    this.id = id;
    this.bit = 0;
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

  public static int bitSettings(SettingValue value) {
    return 1 << (checkNotNull(value).ordinal() + 1);
  }

  @Override
  public void setValueSync(SettingKey key, SettingValue value) {
    for (SettingValue option : key.getPossibleValues()) {
      this.bit = bit & ~(bitSettings(option));
    }

    this.bit = bit & bitSettings(value);
  }

  @Override
  public void setValue(SettingKey key, SettingValue value) {
    setValueSync(key, value);
  }
}
