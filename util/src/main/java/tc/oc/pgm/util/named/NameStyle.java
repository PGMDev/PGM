package tc.oc.pgm.util.named;

/**
 * The formatting properties for each different context in which names are displayed. This varies
 * only by context, and is independent of the viewer.
 */
public enum NameStyle {
  PLAIN(false, false, false, false, false, false, false, false, false), // No formatting
  COLOR(true, false, false, false, false, false, false, true, false), // Color only
  FANCY(
      true, true, true, true, true, false, false, true,
      false), // Color, flair, friend status, nick status, vanish status
  TAB(
      true, true, true, true, true, false, true, true,
      true), // Color, flair, friend status, nick status, death status, and vanish status
  VERBOSE(true, true, true, true, true, true, false, true, true), // Fancy plus nickname
  CONCISE(true, true, true, true, true, true, false, false, true); // Verbose, but removes teleport

  public final boolean isColor;
  public final boolean showPrefix;
  public final boolean showSelf; // Bold if self
  public final boolean showFriend; // Italic if friend
  public final boolean showDisguise; // Strikethrough if disguised
  public final boolean showNickname; // Show nickname after real name
  public final boolean showDeath; // Grey out name if dead
  public final boolean teleport; // Click name to teleport
  public final boolean showVanish; // Italic if vanished

  NameStyle(
      boolean isColor,
      boolean showPrefix,
      boolean showSelf,
      boolean showFriend,
      boolean showDisguise,
      boolean showNickname,
      boolean showDeath,
      boolean teleport,
      boolean showVanish) {
    this.isColor = isColor;
    this.showPrefix = showPrefix;
    this.showSelf = showSelf;
    this.showFriend = showFriend;
    this.showDisguise = showDisguise;
    this.showNickname = showNickname;
    this.showDeath = showDeath;
    this.teleport = teleport;
    this.showVanish = showVanish;
  }
}
