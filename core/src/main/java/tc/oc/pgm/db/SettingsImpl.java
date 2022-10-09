package tc.oc.pgm.db;

import static tc.oc.pgm.util.Assert.assertNotNull;

import java.util.UUID;
import tc.oc.pgm.api.setting.SettingKey;
import tc.oc.pgm.api.setting.SettingValue;
import tc.oc.pgm.api.setting.Settings;

class SettingsImpl implements Settings {

  private final UUID id;
  private long bit;

  SettingsImpl(UUID id, long bit) {
    this.id = assertNotNull(id, "settings id is null");
    this.bit = bit;
  }

  public static long bitSettings(SettingValue value) {
    return 1 << (assertNotNull(value, "setting value is null").ordinal() + 1);
  }

  protected final long getBit() {
    return bit;
  }

  protected final void setBit(long bit) {
    this.bit = bit;
  }

  @Override
  public final UUID getId() {
    return id;
  }

  @Override
  public final SettingValue getValue(SettingKey key) {
    for (SettingValue value : key.getPossibleValues()) {
      final long mask = bitSettings(value);
      if ((bit & mask) == mask) {
        return value;
      }
    }
    return key.getDefaultValue();
  }

  @Override
  public void setValue(SettingKey key, SettingValue value) {
    final long mask = bitSettings(value);
    for (SettingValue other : key.getPossibleValues()) {
      if (other == value) continue;
      this.bit = (bit & ~bitSettings(other)) | mask;
    }
  }

  @Override
  public int hashCode() {
    return id.hashCode();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Settings)) return false;
    return getId().equals(((Settings) o).getId());
  }

  @Override
  public String toString() {
    return id.toString() + " (" + bit + ")";
  }
}
