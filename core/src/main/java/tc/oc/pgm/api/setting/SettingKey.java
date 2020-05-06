package tc.oc.pgm.api.setting;

import static com.google.common.base.Preconditions.*;
import static tc.oc.pgm.api.setting.SettingValue.*;

import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.util.component.Component;
import tc.oc.pgm.util.component.types.PersonalizedTranslatable;

/**
 * A toggleable setting with various possible {@link SettingValue}s.
 *
 * @see SettingValue
 */
public enum SettingKey {
  CHAT(
      "chat",
      ChatColor.DARK_GREEN,
      Material.BEACON,
      CHAT_TEAM,
      CHAT_GLOBAL,
      CHAT_ADMIN), // Changes the default chat channel
  DEATH(
      Arrays.asList("death", "dms"),
      ChatColor.DARK_AQUA,
      Material.BONE,
      DEATH_ALL,
      DEATH_OWN), // Changes which death messages are seen
  PICKER(
      "picker",
      ChatColor.DARK_PURPLE,
      Material.LEATHER_HELMET,
      PICKER_AUTO,
      PICKER_ON,
      PICKER_OFF), // Changes when the picker is displayed
  JOIN(
      Arrays.asList("join", "jms"),
      ChatColor.GOLD,
      Material.IRON_SWORD,
      JOIN_ON,
      JOIN_OFF), // Changes if join messages are seen
  MESSAGE(
      Arrays.asList("message", "dm"),
      ChatColor.BLUE,
      Material.CHEST,
      MESSAGE_ON,
      MESSAGE_OFF), // Changes if direct messages are accepted
  OBSERVERS(
      Arrays.asList("observers", "obs"),
      ChatColor.GREEN,
      Material.GLASS,
      OBSERVERS_ON,
      OBSERVERS_OFF) {
    @Override
    public void update(MatchPlayer player) {
      player.resetVisibility();
    }
  }, // Changes if observers are visible
  SOUNDS(
      "sounds",
      ChatColor.AQUA,
      Material.JUKEBOX,
      SOUNDS_ALL,
      SOUNDS_DM,
      SOUNDS_NONE), // Changes when sounds are played
  VOTE(
      "vote",
      ChatColor.RED,
      Material.BOOK,
      VOTE_ON,
      VOTE_OFF), // Changes if the vote book is shown on cycle
  STATS(
      Collections.singletonList("stats"),
      "match.stats.overall",
      ChatColor.YELLOW,
      Material.WATCH,
      STATS_ON,
      STATS_OFF), // Changes if stats are tracked
  ;

  private static final String SETTING_TRANSLATION_KEY = "setting.";

  private final List<String> aliases;
  private final String displayNameKey;
  private final SettingValue[] values;
  private final ChatColor color;
  private final Material material;

  SettingKey(String name, ChatColor color, Material material, SettingValue... values) {
    this(
        Collections.singletonList(name),
        SETTING_TRANSLATION_KEY + name.toLowerCase(),
        color,
        material,
        values);
  }

  SettingKey(List<String> aliases, ChatColor color, Material material, SettingValue... values) {
    this(aliases, SETTING_TRANSLATION_KEY + aliases.get(0).toLowerCase(), color, material, values);
  }

  SettingKey(
      List<String> aliases,
      String displayNameKey,
      ChatColor color,
      Material material,
      SettingValue... values) {
    checkArgument(!aliases.isEmpty(), "aliases is empty");
    this.aliases = ImmutableList.copyOf(aliases);
    this.displayNameKey = displayNameKey;
    this.values = values;
    this.color = color;
    this.material = material;
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
   * Get the display name of the {@link SettingKey}.
   *
   * @return The formatted display name.
   */
  public Component getDisplayName() {
    return new PersonalizedTranslatable(displayNameKey)
        .getPersonalizedText()
        .bold(true)
        .color(this.color);
  }

  /**
   * Gets the lore of the setting.
   *
   * @param value the formatted value the setting is currently set to
   * @return the complete lore text for the setting
   */
  public Component getLore(Component value) {
    return new PersonalizedTranslatable(
            SETTING_TRANSLATION_KEY + this.getName().toLowerCase() + ".lore", value)
        .getPersonalizedText()
        .color(ChatColor.GRAY);
  }

  /**
   * Gets the material used to represent the setting.
   *
   * @return the material used to represent the setting.
   */
  public Material getMaterial() {
    return material;
  }

  /**
   * Get all aliases of this {@link SettingKey}. First index is always equal to {@code #getName}.
   *
   * @return An immutable list of all aliases. Never {@code null} or empty.
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

  /**
   * Called whether setting has changed and is ready to be updated internally.
   *
   * @param player owner of the setting
   */
  public void update(MatchPlayer player) {}
}
