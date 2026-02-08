package tc.oc.pgm.util.tablist;

import lombok.Getter;

public class TabManagerDirtyTracker {
  @Getter
  private boolean layoutOrContent;

  @Getter
  private boolean headerOrFooter;

  // Is any child view prioritized?
  @Getter
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

  public boolean isDirty() {
    return layoutOrContent || headerOrFooter;
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
