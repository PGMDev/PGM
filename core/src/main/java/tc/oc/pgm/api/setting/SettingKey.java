package tc.oc.pgm.api.setting;

import static com.google.common.base.Preconditions.*;
import static tc.oc.pgm.api.setting.SettingValue.*;

import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import tc.oc.pgm.api.player.MatchPlayer;

/**
 * A toggleable setting with various possible {@link SettingValue}s.
 *
 * @see SettingValue
 */
public enum SettingKey {
  CHAT("chat", CHAT_TEAM, CHAT_GLOBAL, CHAT_ADMIN), // Changes the default chat channel
  DEATH(
      Arrays.asList("death", "dms"), DEATH_ALL, DEATH_OWN), // Changes which death messages are seen
  PICKER("picker", PICKER_AUTO, PICKER_ON, PICKER_OFF), // Changes when the picker is displayed
  JOIN(Arrays.asList("join", "jms"), JOIN_ON, JOIN_OFF), // Changes if join messages are seen
  MESSAGE(
      Arrays.asList("message", "dm"),
      MESSAGE_ON,
      MESSAGE_OFF), // Changes if direct messages are accepted
  OBSERVERS(Arrays.asList("observers", "obs"), OBSERVERS_ON, OBSERVERS_OFF) {
    @Override
    public void update(MatchPlayer player) {
      player.resetVisibility();
    }
  }, // Changes if observers are visible
  SOUNDS("sounds", SOUNDS_ALL, SOUNDS_DM, SOUNDS_NONE), // Changes when sounds are played
  VOTE("vote", VOTE_ON, VOTE_OFF), // Changes if the vote book is shown on cycle
  ;

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
