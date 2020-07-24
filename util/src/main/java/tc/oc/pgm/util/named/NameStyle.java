package tc.oc.pgm.util.named;

/**
 * The formatting properties for each different context in which names are displayed. This varies
 * only by context, and is independent of the viewer.
 */
public enum NameStyle {
  PLAIN(false, false, false, false, false), // No formatting
  COLOR(true, false, false, true, false), // Color and teleport
  FANCY(true, true, false, true, false), // Color, flair, and teleport
  TAB(true, true, true, false, true), // Color, flair, death status, and vanish
  LEGACY_TAB(true, true, false, false, true), // Color, flair, and vanish (crossed out)
  VERBOSE(true, true, false, true, true), // Color, flair, teleport, and vanish
  CONCISE(true, true, false, false, true); // Color, flair, and vanish (offline)

  public final boolean isColor;
  public final boolean showPrefix;
  public final boolean showDeath; // Grey out name if dead
  public final boolean teleport; // Click name to teleport
  public final boolean showVanish; // Whether to render name as online or not

  NameStyle(
      boolean isColor,
      boolean showPrefix,
      boolean showDeath,
      boolean teleport,
      boolean showVanish) {
    this.isColor = isColor;
    this.showPrefix = showPrefix;
    this.showDeath = showDeath;
    this.teleport = teleport;
    this.showVanish = showVanish;
  }
}
