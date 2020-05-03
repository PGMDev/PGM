package tc.oc.pgm.api.setting;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import tc.oc.pgm.util.StringUtils;
import tc.oc.pgm.util.component.Component;
import tc.oc.pgm.util.component.types.PersonalizedTranslatable;

/**
 * Values of a particular {@link SettingKey}, a toggleable setting.
 *
 * @see SettingKey
 */
public enum SettingValue {
  CHAT_TEAM("chat", "team", ChatColor.BLUE), // Only send to members on the player's team
  CHAT_GLOBAL("chat", "global", ChatColor.GOLD), // Send to all players in the same match
  CHAT_ADMIN("chat", "admin", ChatColor.DARK_RED), // Send to all server operators

  DEATH_OWN("death", "own", ChatColor.RED), // Only send death messages involving self
  DEATH_ALL("death", "all", ChatColor.GREEN), // Send all death messages, highlight your own

  PICKER_AUTO("picker", "auto", ChatColor.GOLD), // Display after cycle, or with permissions.
  PICKER_ON("picker", "on", ChatColor.GREEN), // Display the picker GUI always
  PICKER_OFF("picker", "off", ChatColor.RED), // Never display the picker GUI

  JOIN_ON("join", "on", ChatColor.GREEN), // Send all join messages
  JOIN_OFF("join", "off", ChatColor.RED), // Never send join messages

  MESSAGE_ON("message", "on", ChatColor.GREEN), // Always accept direct messages
  MESSAGE_OFF("message", "off", ChatColor.RED), // Never accept direct messages

  OBSERVERS_ON("observers", "on", ChatColor.GREEN, Material.EYE_OF_ENDER), // Show observers
  OBSERVERS_OFF("observers", "off", ChatColor.RED, Material.ENDER_PEARL), // Hide observers

  SOUNDS_ALL("sounds", "all", ChatColor.GREEN), // Play all sounds
  SOUNDS_DM("sounds", "messages", ChatColor.GOLD), // Only play DM sounds
  SOUNDS_NONE("sounds", "none", ChatColor.RED), // Never play sounds

  VOTE_ON("vote", "on", ChatColor.GREEN), // Show the vote book on cycle
  VOTE_OFF("vote", "off", ChatColor.RED), // Don't show the vote book on cycle

  STATS_ON("stats", "on", ChatColor.GREEN), // Track stats
  STATS_OFF("stats", "off", ChatColor.RED), // Don't track stats
  ;

  private static final String SETTING_TRANSLATION_KEY = "setting.";
  private static final String VALUE_KEY = ".value.";

  private final String key;
  private final String name;
  private final ChatColor color;
  @Nullable private final Material material;

  SettingValue(String key, String name, ChatColor color) {
    this(key, name, color, null);
  }

  SettingValue(String key, String name, ChatColor color, @Nullable Material material) {
    this.key = key;
    this.name = checkNotNull(name);
    this.color = color;
    this.material = material;
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
   * Gets the formatted name of the {@link SettingValue}.
   *
   * @return formatted name of the {@link SettingValue}.
   */
  public Component getDisplayName() {
    return new PersonalizedTranslatable(
            SETTING_TRANSLATION_KEY + key.toLowerCase() + VALUE_KEY + name)
        .getPersonalizedText()
        .color(color);
  }

  /**
   * Gets the material that should be used to represent the {@link SettingValue}.
   *
   * @return the material that should be used to represent the {@link SettingValue}.
   */
  @Nullable
  public Material getMaterial() {
    return material;
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
