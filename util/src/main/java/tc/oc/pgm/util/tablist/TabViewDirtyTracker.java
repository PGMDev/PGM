package tc.oc.pgm.util.tablist;

public class TabViewDirtyTracker {
  private boolean layout;
  private boolean content;
  private boolean header;
  private boolean footer;

  // Should this view be prioritized?
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

  public boolean isPriority() {
    return priority;
  }

  public boolean isLayout() {
    return layout;
  }

  public boolean isContent() {
    return content;
  }

  public boolean isLayoutOrContent() {
    return layout || content;
  }

  public boolean isHeader() {
    return header;
  }

  public boolean isFooter() {
    return footer;
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
