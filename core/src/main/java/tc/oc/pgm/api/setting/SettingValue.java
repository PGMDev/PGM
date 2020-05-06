package tc.oc.pgm.api.setting;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.kyori.text.Component;
import net.kyori.text.TranslatableComponent;
import net.kyori.text.format.TextColor;
import org.bukkit.Material;
import tc.oc.pgm.util.StringUtils;

/**
 * Values of a particular {@link SettingKey}, a toggleable setting.
 *
 * @see SettingKey
 */
public enum SettingValue {
  CHAT_TEAM(
      "chat", "team", "misc.team", TextColor.BLUE), // Only send to members on the player's team
  CHAT_GLOBAL(
      "chat", "global", "misc.global", TextColor.GOLD), // Send to all players in the same match
  CHAT_ADMIN("chat", "admin", "misc.admin", TextColor.DARK_RED), // Send to all server operators

  DEATH_OWN("death", "own", TextColor.RED), // Only send death messages involving self
  DEATH_ALL(
      "death", "all", "misc.all", TextColor.GREEN), // Send all death messages, highlight your own

  PICKER_AUTO("picker", "auto", TextColor.GOLD), // Display after cycle, or with permissions.
  PICKER_ON("picker", "on", "misc.on", TextColor.GREEN), // Display the picker GUI always
  PICKER_OFF("picker", "off", "misc.off", TextColor.RED), // Never display the picker GUI

  JOIN_ON("join", "on", "misc.all", TextColor.GREEN), // Send all join messages
  JOIN_OFF("join", "off", "misc.none", TextColor.RED), // Never send join messages

  MESSAGE_ON("message", "on", "misc.all", TextColor.GREEN), // Always accept direct messages
  MESSAGE_OFF("message", "off", "misc.none", TextColor.RED), // Never accept direct messages

  OBSERVERS_ON("observers", "on", TextColor.GREEN, Material.EYE_OF_ENDER), // Show observers
  OBSERVERS_OFF("observers", "off", TextColor.RED, Material.ENDER_PEARL), // Hide observers

  SOUNDS_ALL("sounds", "all", "misc.all", TextColor.GREEN), // Play all sounds
  SOUNDS_DM("sounds", "messages", TextColor.GOLD), // Only play DM sounds
  SOUNDS_NONE("sounds", "none", "misc.none", TextColor.RED), // Never play sounds

  VOTE_ON("vote", "on", "misc.on", TextColor.GREEN), // Show the vote book on cycle
  VOTE_OFF("vote", "off", "misc.off", TextColor.RED), // Don't show the vote book on cycle

  STATS_ON("stats", "on", "misc.on", TextColor.GREEN), // Track stats
  STATS_OFF("stats", "off", "misc.off", TextColor.RED), // Don't track stats
  ;

  private static final String SETTING_TRANSLATION_KEY = "setting.";
  private static final String VALUE_KEY = ".value.";

  private final String key;
  private final String name;
  private final String displayNameKey;
  private final TextColor color;
  @Nullable private final Material material;

  SettingValue(String key, String name, TextColor color) {
    this(key, name, SETTING_TRANSLATION_KEY + key.toLowerCase() + VALUE_KEY + name, color, null);
  }

  SettingValue(String key, String name, String displayNameKey, TextColor color) {
    this(key, name, displayNameKey, color, null);
  }

  SettingValue(String key, String name, TextColor color, @Nullable Material material) {
    this(
        key, name, SETTING_TRANSLATION_KEY + key.toLowerCase() + VALUE_KEY + name, color, material);
  }

  SettingValue(
      String key,
      String name,
      String displayNameKey,
      TextColor color,
      @Nullable Material material) {
    this.key = key;
    this.name = checkNotNull(name);
    this.displayNameKey = displayNameKey;
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
    return TranslatableComponent.of(displayNameKey).color(color);
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
