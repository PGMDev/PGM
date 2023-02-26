package tc.oc.pgm.util.tablist;

public class TabManagerDirtyTracker {
  private boolean layoutOrContent;
  private boolean headerOrFooter;

  // Is any child view prioritized?
  private boolean priority;

  private final Runnable callback;

  public TabManagerDirtyTracker(Runnable callback) {
    this.callback = callback;
  }

  protected void update(TabViewDirtyTracker tab) {
    // If anything can be propagated upwards, do so
    if ((tab.isLayoutOrContent() && !layoutOrContent)
        || (tab.isHeaderOrFooter() && !headerOrFooter)
        || (tab.isPriority() && !priority)) {

      layoutOrContent |= tab.isLayoutOrContent();
      headerOrFooter |= tab.isHeaderOrFooter();
      priority |= tab.isPriority();
      callback.run();
    }
  }

  public boolean isLayoutOrContent() {
    return layoutOrContent;
  }

  public boolean isHeaderOrFooter() {
    return headerOrFooter;
  }

  public boolean isDirty() {
    return layoutOrContent || headerOrFooter;
  }

  public boolean isPriority() {
    return priority;
  }

  public void validateHeaderAndFooter() {
    this.headerOrFooter = false;
  }

  public void validatePriority() {
    this.priority = false;
  }

  public void validate() {
    layoutOrContent = headerOrFooter = priority = false;
  }
}
