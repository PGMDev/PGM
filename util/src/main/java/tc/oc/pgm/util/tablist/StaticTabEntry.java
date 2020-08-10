package tc.oc.pgm.util.tablist;

import net.kyori.text.Component;
import net.md_5.bungee.api.chat.BaseComponent;
import tc.oc.pgm.util.text.TextTranslations;

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
  public BaseComponent[] getContent(TabView view) {
    return TextTranslations.toBaseComponentArray(content, view.getViewer());
  }
}
