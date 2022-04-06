package tc.oc.pgm.api.setting;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.bukkit.DyeColor;
import tc.oc.pgm.api.StringUtils;

/**
 * Values of a particular {@link SettingKey}, a toggleable setting.
 *
 * @see SettingKey
 */
public enum SettingValue {
  CHAT_TEAM("chat", "team", DyeColor.GREEN), // Only send to members on the player's team
  CHAT_GLOBAL("chat", "global", DyeColor.ORANGE), // Send to all players in the same match
  CHAT_ADMIN("chat", "admin", DyeColor.RED), // Send to all server operators

  DEATH_OWN("death", "own", DyeColor.RED), // Only send death messages involving self
  DEATH_ALL("death", "all", DyeColor.GREEN), // Send all death messages, highlight your own

  PICKER_AUTO("picker", "auto", DyeColor.ORANGE), // Display after cycle, or with permissions.
  PICKER_ON("picker", "on", DyeColor.GREEN), // Display the picker GUI always
  PICKER_OFF("picker", "off", DyeColor.RED), // Never display the picker GUI

  JOIN_ON("join", "all", DyeColor.ORANGE), // Send all join messages
  JOIN_OFF("join", "none", DyeColor.RED), // Never send join messages

  MESSAGE_ON("message", "on", DyeColor.GREEN), // Always accept direct messages
  MESSAGE_OFF("message", "off", DyeColor.RED), // Never accept direct messages

  OBSERVERS_ON("observers", "on", DyeColor.GREEN), // Show observers
  OBSERVERS_OFF("observers", "off", DyeColor.RED), // Hide observers

  SOUNDS_ALL("sounds", "all", DyeColor.GREEN), // Play all sounds
  SOUNDS_DM("sounds", "messages", DyeColor.ORANGE), // Only play DM sounds
  SOUNDS_NONE("sounds", "none", DyeColor.RED), // Never play sounds

  VOTE_ON("vote", "on", DyeColor.GREEN), // Show the vote book on cycle
  VOTE_OFF("vote", "off", DyeColor.RED), // Don't show the vote book on cycle

  STATS_ON("stats", "on", DyeColor.GREEN), // Track stats
  STATS_OFF("stats", "off", DyeColor.RED), // Don't track stats

  EFFECTS_ON("effects", "on", DyeColor.GREEN), // Display special particle effects
  EFFECTS_OFF("effects", "off", DyeColor.RED), // Don't display special particle effects

  TIME_AUTO("time", "auto", DyeColor.ORANGE), // Player time is in sync
  TIME_DARK("time", "dark", DyeColor.GRAY), // Player time is always set to midday
  TIME_LIGHT("time", "light", DyeColor.WHITE); // Player time is always set to midnight

  private final String key;
  private final String name;
  private final DyeColor color;

  SettingValue(String group, String name, DyeColor color) {
    this.key = checkNotNull(group);
    this.name = checkNotNull(name);
    this.color = checkNotNull(color);
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

  /**
   * Get {@link DyeColor} related to this setting value .
   *
   * @return {@link DyeColor} for this setting value.
   */
  public DyeColor getColor() {
    return color;
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
