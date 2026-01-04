package tc.oc.pgm.util.named;

/**
 * MapNameStyle Formatting properties related to styling a map name
 *
 * <p>Note: Unused styles may be in use in Community
 */
public enum MapNameStyle {
  PLAIN(false, false, false, false), // No formatting for map
  COLOR(true, false, false, false), // Format with color only, no authors or highlight
  HIGHLIGHT(true, true, false, false), // Format with color and highlight, no authors
  COLOR_WITH_AUTHORS(true, false, true, false), // Format with color and authors, no highlight
  HIGHLIGHT_WITH_AUTHORS(true, true, true, false), // Format with color, authors, and highlight
  COLOR_INTERACTIVE(true, false, false, true), // Format with color, hover, and click
  COLOR_WITH_AUTHORS_INTERACTIVE(
      true, false, true, true), // Format with color, authors, hover, and click
  HIGHLIGHT_WITH_AUTHORS_INTERACTIVE(
      true, true, true, true); // Format with color, highlight, authors, hover, and click

  public final boolean isColor; // If color formatting should apply
  public final boolean isHighlight; // If map title is highlighted
  public final boolean showAuthors; // If authors are shown
  public final boolean isInteractive; // If hover/click events should be added

  MapNameStyle(boolean isColor, boolean isHighlight, boolean showAuthors, boolean isInteractive) {
    this.isColor = isColor;
    this.isHighlight = isHighlight;
    this.showAuthors = showAuthors;
    this.isInteractive = isInteractive;
  }
}
