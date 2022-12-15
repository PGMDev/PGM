package tc.oc.pgm.api.setting;

import static tc.oc.pgm.util.Assert.assertNotNull;

import org.bukkit.DyeColor;

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
  DEATH_ALL("death", "all", DyeColor.ORANGE), // Send all death messages, highlight your own
  DEATH_FRIENDS(
      "death", "friends", DyeColor.GREEN), // Only send death messages involving yourself or friends

  PICKER_AUTO("picker", "auto", DyeColor.ORANGE), // Display after cycle, or with permissions.
  PICKER_ON("picker", "on", DyeColor.GREEN), // Display the picker GUI always
  PICKER_OFF("picker", "off", DyeColor.RED), // Never display the picker GUI

  JOIN_ON("join", "all", DyeColor.ORANGE), // Send all join messages
  JOIN_FRIENDS("join", "friends", DyeColor.GREEN), // Only send friend join messages
  JOIN_OFF("join", "none", DyeColor.RED), // Never send join messages

  MESSAGE_ON("message", "all", DyeColor.GREEN), // Always accept direct messages
  MESSAGE_FRIEND("message", "friends", DyeColor.YELLOW), // Only accept friend direct messages
  MESSAGE_OFF("message", "none", DyeColor.RED), // Never accept direct messages

  OBSERVERS_ON("observers", "all", DyeColor.GREEN), // Show observers
  OBSERVERS_FRIEND("observers", "friends", DyeColor.YELLOW), // Only show friend observers
  OBSERVERS_OFF("observers", "none", DyeColor.RED), // Hide observers

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
    this.key = assertNotNull(group);
    this.name = assertNotNull(name);
    this.color = assertNotNull(color);
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
   * @see tc.oc.pgm.settings.SettingsMenu for usage.
   * @return {@link DyeColor} for this setting value.
   */
  public DyeColor getColor() {
    return color;
  }

  @Override
  public String toString() {
    return getName();
  }
}
