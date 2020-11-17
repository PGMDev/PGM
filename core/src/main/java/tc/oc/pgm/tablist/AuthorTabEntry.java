package tc.oc.pgm.tablist;

import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.translatable;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.md_5.bungee.api.chat.BaseComponent;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.tablist.DynamicTabEntry;
import tc.oc.pgm.util.tablist.TabView;
import tc.oc.pgm.util.text.TextTranslations;

public class AuthorTabEntry extends DynamicTabEntry {

  private final MapInfo map;
  private final int authorIdx;

  public AuthorTabEntry(Match match, int authorIdx) {
    this.map = match.getMap();
    this.authorIdx = authorIdx;
  }

  @Override
  public BaseComponent[] getContent(TabView view) {
    return TextTranslations.toBaseComponentArray(
        map.getAuthors().stream()
            .skip(authorIdx)
            .findFirst()
            .<Component>map(
                author ->
                    translatable(
                        authorIdx == 0 ? "misc.by" : "misc.and",
                        NamedTextColor.GRAY,
                        author.getName(NameStyle.FANCY)))
            .orElse(empty()),
        view.getViewer());
  }
}
