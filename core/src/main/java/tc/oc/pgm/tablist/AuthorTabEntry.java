package tc.oc.pgm.tablist;

import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.TranslatableComponent;
import net.kyori.text.format.TextColor;
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
                    TranslatableComponent.of(
                        authorIdx == 0 ? "misc.by" : "misc.and",
                        TextColor.GRAY,
                        author.getName(NameStyle.FANCY)))
            .orElse(TextComponent.empty()),
        view.getViewer());
  }
}
