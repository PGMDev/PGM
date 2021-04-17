package tc.oc.pgm.util.tablist;

import net.kyori.adventure.text.Component;

public class StaticTabEntry extends SimpleTabEntry {

  private final Component content;

  public StaticTabEntry(Component content) {
    this.content = content;
  }

  @Override
  public void addToView(TabView view) {}

  @Override
  public void removeFromView(TabView view) {}

  @Override
  public boolean isDirty(TabView view) {
    return false;
  }

  @Override
  public void markClean(TabView view) {}

  @Override
  public Component getContent(TabView view) {
    return content;
  }
}
