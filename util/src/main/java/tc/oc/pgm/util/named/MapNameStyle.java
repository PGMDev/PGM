package tc.oc.pgm.util.named;

/** MapNameStyle Formatting properties related to styling a map name */
public enum MapNameStyle {
  PLAIN(false, false, false), // No formatting for map
  COLOR(true, false, false), // Format with color only, no authors or highlight
  HIGHLIGHT(true, true, false), // Format with color and highlight, no authors
  COLOR_WITH_AUTHORS(true, false, true), // Format with color and authors, no highlight
  HIGHLIGHT_WITH_AUTHORS(true, true, true); // Format with color, authors, and highlight

  public final boolean isColor; // If color formatting should apply
  public final boolean isHighlight; // If map title is highlighted
  public final boolean showAuthors; // If authors are shown

  MapNameStyle(boolean isColor, boolean isHighlight, boolean showAuthors) {
    this.isColor = isColor;
    this.isHighlight = isHighlight;
    this.showAuthors = showAuthors;
  }
}
