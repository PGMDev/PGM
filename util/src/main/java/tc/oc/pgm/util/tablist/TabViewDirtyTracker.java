package tc.oc.pgm.util.tablist;

import lombok.Getter;

public class TabViewDirtyTracker {
  @Getter
  private boolean layout;

  @Getter
  private boolean content;

  @Getter
  private boolean header;

  @Getter
  private boolean footer;

  // Should this view be prioritized?
  @Getter
  private boolean priority;

  private TabManagerDirtyTracker parent;

  private void propagate() {
    if (parent != null) parent.update(this);
  }

  public void enable(TabManagerDirtyTracker parent) {
    this.parent = parent;
    // Invalidate everything
    layout = content = header = footer = true;
    this.priority = true;
    this.propagate();
  }

  public boolean isDirty() {
    return layout || content || header || footer;
  }

  public boolean isLayoutOrContent() {
    return layout || content;
  }

  public boolean isHeaderOrFooter() {
    return header || footer;
  }

  public void invalidateLayout() {
    if (!this.layout) {
      this.layout = true;
      this.propagate();
    }
  }

  public void invalidateContent() {
    if (!this.content) {
      this.content = true;
      this.propagate();
    }
  }

  public void invalidateLayoutAndContent() {
    if (!layout || !content) {
      layout = content = true;
      this.propagate();
    }
  }

  public void invalidateHeader() {
    if (!this.header) {
      this.header = true;
      this.propagate();
    }
  }

  public void invalidateFooter() {
    if (!this.footer) {
      this.footer = true;
      this.propagate();
    }
  }

  public void prioritize() {
    if (!priority) {
      this.priority = true;
      // Avoid propagating if nothing is dirty
      // Will propagate once anything else makes it becomes dirty
      if (isDirty()) this.propagate();
    }
  }

  public void validateLayout() {
    this.layout = false;
  }

  public void validateContent() {
    this.content = false;
  }

  public void validateHeader() {
    this.header = false;
  }

  public void validateFooter() {
    this.footer = false;
  }

  public void validatePriority() {
    priority = false;
  }
}
