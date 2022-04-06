package tc.oc.pgm.tablist;

import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.translatable;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.named.NameStyle;
import tc.oc.pgm.util.tablist.DynamicTabEntry;
import tc.oc.pgm.util.tablist.TabView;

public class AuthorTabEntry extends DynamicTabEntry {

  private final MapInfo map;
  private final int authorIdx;

  public AuthorTabEntry(Match match, int authorIdx) {
    this.map = match.getMap();
    this.authorIdx = authorIdx;
  }

  @Override
  public Component getContent(TabView view) {
    return map.getAuthors().stream()
        .skip(authorIdx)
        .findFirst()
        .<Component>map(
            author ->
                translatable(
                    authorIdx == 0 ? "misc.by" : "misc.and",
                    NamedTextColor.GRAY,
                    author.getName(NameStyle.FANCY)))
        .orElse(empty());
  }
}
