package tc.oc.pgm.tablist;

import java.util.List;
import java.util.stream.Collectors;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import tc.oc.component.Component;
import tc.oc.component.types.PersonalizedText;
import tc.oc.component.types.PersonalizedTranslatable;
import tc.oc.named.NameStyle;
import tc.oc.pgm.map.Contributor;
import tc.oc.pgm.match.Match;
import tc.oc.pgm.util.TranslationUtils;
import tc.oc.tablist.DynamicTabEntry;
import tc.oc.tablist.TabView;
import tc.oc.util.collection.DefaultProvider;

public class MapTabEntry extends DynamicTabEntry {

  public static class Factory implements DefaultProvider<Match, MapTabEntry> {
    @Override
    public MapTabEntry get(Match key) {
      return new MapTabEntry(key);
    }
  }

  private final Match match;

  protected MapTabEntry(Match match) {
    this.match = match;
  }

  @Override
  public BaseComponent getContent(TabView view) {
    Component content =
        new PersonalizedText(match.getMapInfo().name, ChatColor.AQUA, ChatColor.BOLD);

    List<Contributor> authors = match.getMapInfo().getNamedAuthors();
    if (!authors.isEmpty()) {
      content =
          new PersonalizedText(
              new PersonalizedTranslatable(
                  "misc.authorship",
                  content,
                  TranslationUtils.combineComponents(
                      authors.stream()
                          .map(contributor -> contributor.getStyledName(NameStyle.FANCY))
                          .collect(Collectors.toList()))),
              ChatColor.DARK_GRAY);
    }

    return content.render(view.getViewer());
  }
}
