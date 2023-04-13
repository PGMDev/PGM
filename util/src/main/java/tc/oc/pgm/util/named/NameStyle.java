package tc.oc.pgm.util.named;

import java.util.EnumSet;
import java.util.Set;

/**
 * The formatting properties for each different context in which names are displayed. This varies
 * only by context, and is independent of the viewer.
 */
public enum NameStyle {
  // No formatting
  PLAIN(EnumSet.noneOf(Flag.class)),
  // Simple formatting, just team color. Used for non-clickable places
  SIMPLE_COLOR(EnumSet.of(Flag.COLOR)),
  // Simple formatting, just team color & teleport
  COLOR(EnumSet.of(Flag.COLOR, Flag.TELEPORT)),
  // Fancy formatting, flairs, color and click to teleport
  FANCY(EnumSet.of(Flag.COLOR, Flag.FLAIR, Flag.FRIEND, Flag.DISGUISE, Flag.TELEPORT)),
  // Tab list format, flairs, color, death status, self, etc
  TAB(
      EnumSet.of(
          Flag.COLOR, Flag.FLAIR, Flag.SELF, Flag.FRIEND, Flag.SQUAD, Flag.DISGUISE, Flag.DEATH)),
  // Fancy plus full nickname
  VERBOSE(
      EnumSet.of(Flag.COLOR, Flag.FLAIR, Flag.FRIEND, Flag.DISGUISE, Flag.NICKNAME, Flag.TELEPORT));

  private final Set<Flag> flags;

  NameStyle(Set<Flag> flags) {
    this.flags = flags;
  }

  public boolean has(Flag flag) {
    return flags.contains(flag);
  }

  public enum Flag {
    COLOR, // Color
    FLAIR, // Show flair
    SELF, // Bold if self
    FRIEND, // Italic if friend
    SQUAD, // Underline if in the same squad
    DISGUISE, // Strikethrough if disguised
    NICKNAME, // Show nickname after real name
    DEATH, // Grey out name if dead
    TELEPORT, // Click name to teleport
  }
}
