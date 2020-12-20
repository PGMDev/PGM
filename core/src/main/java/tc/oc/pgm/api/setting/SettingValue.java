package tc.oc.pgm.api.setting;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import tc.oc.pgm.util.StringUtils;

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
  DEATH_FRIENDS("death", "friends"), // Only send death messages involving yourself or friends

  PICKER_AUTO("picker", "auto"), // Display after cycle, or with permissions.
  PICKER_ON("picker", "on"), // Display the picker GUI always
  PICKER_OFF("picker", "off"), // Never display the picker GUI

  JOIN_ON("join", "all"), // Send all join messages
  JOIN_OFF("join", "none"), // Never send join messages
  JOIN_FRIENDS("join", "friends"), // Only send friend join messages

  MESSAGE_ON("message", "on"), // Always accept direct messages
  MESSAGE_OFF("message", "off"), // Never accept direct messages

  OBSERVERS_ON("observers", "on"), // Show observers
  OBSERVERS_OFF("observers", "off"), // Hide observers
  OBSERVERS_FRIEND("observers", "friends"), // Only show friend observers

  SOUNDS_ALL("sounds", "all"), // Play all sounds
  SOUNDS_DM("sounds", "messages"), // Only play DM sounds
  SOUNDS_NONE("sounds", "none"), // Never play sounds

  VOTE_ON("vote", "on"), // Show the vote book on cycle
  VOTE_OFF("vote", "off"), // Don't show the vote book on cycle

  STATS_ON("stats", "on"), // Track stats
  STATS_OFF("stats", "off"), // Don't track stats

  EFFECTS_ON("effects", "on"), // Display special particle effects
  EFFECTS_OFF("effects", "off"); // Don't display special particle effects

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
