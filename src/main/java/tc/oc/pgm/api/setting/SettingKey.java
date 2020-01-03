package tc.oc.pgm.api.setting;

import static com.google.common.base.Preconditions.*;
import static tc.oc.pgm.api.setting.SettingValue.*;

import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * A toggleable setting with various possible {@link SettingValue}s.
 *
 * @see SettingValue
 */
public enum SettingKey {
  CHAT("chat", CHAT_TEAM, CHAT_GLOBAL, CHAT_ADMIN), // Changes the default chat channel
  DEATH(
      Arrays.asList("death", "dms"), DEATH_ALL, DEATH_OWN), // Changes which death messages are seen
  PICKER("picker", PICKER_AUTO, PICKER_ON, PICKER_OFF); // Changes when the picker is displayed

  private final List<String> aliases;
  private final SettingValue[] values;

  SettingKey(String name, SettingValue... values) {
    this(Collections.singletonList(name), values);
  }

  SettingKey(List<String> aliases, SettingValue... values) {
    checkArgument(!aliases.isEmpty(), "aliases is empty");
    this.aliases = ImmutableList.copyOf(aliases);
    this.values = values;
  }

  /**
   * Get the name of the {@link SettingKey}.
   *
   * @return The name.
   */
  public String getName() {
    return aliases.get(0);
  }

  /**
   * Get all aliases of this {@link SettingKey}.
   *
   * @return An immutable list of all aliases.
   */
  public List<String> getAliases() {
    return aliases;
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
