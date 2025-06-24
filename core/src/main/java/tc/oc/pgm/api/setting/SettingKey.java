package tc.oc.pgm.api.setting;

import static tc.oc.pgm.api.setting.SettingValue.*;

import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.modules.PlayerTimeMatchModule;
import tc.oc.pgm.util.Aliased;
import tc.oc.pgm.util.material.Materials;

/**
 * A toggleable setting with various possible {@link SettingValue}s.
 *
 * @see SettingValue
 */
public enum SettingKey implements Aliased {
  CHAT("chat", Materials.SIGN, CHAT_TEAM, CHAT_GLOBAL, CHAT_ADMIN) {
    @Override
    public void update(MatchPlayer player) {
      PGM.get().getChatManager().setChannel(player, player.getSettings().getValue(CHAT));
    }
  }, // Changes the default chat channel
  DEATH(
      Arrays.asList("death", "dms"),
      Materials.SKULL,
      DEATH_ALL,
      DEATH_OWN,
      DEATH_FRIENDS,
      DEATH_SQUAD), // Changes which death messages are seen
  PICKER(
      "picker",
      Material.LEATHER_HELMET,
      PICKER_AUTO,
      PICKER_ON,
      PICKER_OFF,
      PICKER_MANUAL), // Changes when the picker is displayed
  JOIN(
      Arrays.asList("join", "jms"),
      Materials.WOOD_DOOR,
      JOIN_ON,
      JOIN_FRIENDS,
      JOIN_OFF), // Changes if join messages are seen
  MESSAGE(
      Arrays.asList("message", "dm"),
      Materials.BOOK_AND_QUILL,
      MESSAGE_ON,
      MESSAGE_FRIEND,
      MESSAGE_OFF), // Changes if direct messages are accepted
  OBSERVERS(
      Arrays.asList("observers", "obs"),
      Materials.EYE_OF_ENDER,
      OBSERVERS_ON,
      OBSERVERS_FRIEND,
      OBSERVERS_OFF) {
    @Override
    public void update(MatchPlayer player) {
      player.resetVisibility();
    }
  }, // Changes if observers are visible
  SOUNDS(
      "sounds",
      Material.NOTE_BLOCK,
      SOUNDS_ALL,
      SOUNDS_CHAT,
      SOUNDS_DM,
      SOUNDS_NONE), // Changes when sounds are played
  VOTE(
      "vote",
      Material.ENCHANTED_BOOK,
      VOTE_ON,
      VOTE_OFF), // Changes if the vote book is shown on cycle
  STATS("stats", Material.PAPER, STATS_ON, STATS_OFF), // Changes if stats are tracked
  EFFECTS(
      "effects",
      Materials.FIREWORK,
      EFFECTS_ON,
      EFFECTS_OFF), // Changes if special particle effects are shown
  TIME(Arrays.asList("time", "theme"), Materials.WATCH, TIME_AUTO, TIME_DARK, TIME_LIGHT) {
    @Override
    public void update(MatchPlayer player) {
      PlayerTimeMatchModule.updatePlayerTime(player);
    }
  }; // Changes player preference for time of day

  private final List<String> aliases;
  private final SettingValue[] values;
  private final Material icon;

  SettingKey(String name, Material icon, SettingValue... values) {
    this(Collections.singletonList(name), icon, values);
  }

  SettingKey(List<String> aliases, Material icon, SettingValue... values) {
    this.aliases = ImmutableList.copyOf(aliases);
    this.icon = icon;
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
   * Get all aliases of this {@link SettingKey}. First index is always equal to {@code #getName}.
   *
   * @return An immutable list of all aliases. Never {@code null} or empty.
   */
  public List<String> getAliases() {
    return aliases;
  }

  @NotNull
  @Override
  public Iterator<String> iterator() {
    return aliases.iterator();
  }

  /**
   * Get a list of the possible {@link SettingValue}s.
   *
   * @return An array of {@link SettingValue}s, sorted by defined order.
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

  /**
   * Get the {@link Material} used to visually represent this setting in GUI menus.
   *
   * @return {@link Material} to visually represent setting.
   */
  public Material getIconMaterial() {
    return icon;
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
