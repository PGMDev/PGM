package tc.oc.pgm.tablist;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.md_5.bungee.api.chat.BaseComponent;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.util.tablist.DynamicTabEntry;
import tc.oc.pgm.util.tablist.TabView;
import tc.oc.pgm.util.text.TextTranslations;

public class FreeForAllTabEntry extends DynamicTabEntry {

  private final Match match;

  public FreeForAllTabEntry(Match match) {
    this.match = match;
  }

  @Override
  public BaseComponent[] getContent(TabView view) {
    Component content =
        Component.text()
            .append(
                Component.text(
                    String.valueOf(match.getParticipants().size()), NamedTextColor.WHITE))
            .append(Component.text("/", NamedTextColor.DARK_GRAY))
            .append(Component.text(String.valueOf(match.getMaxPlayers()), NamedTextColor.GRAY))
            .append(Component.space())
            .append(
                Component.translatable(
                    "match.info.players", NamedTextColor.YELLOW, TextDecoration.BOLD))
            .build();
    return TextTranslations.toBaseComponentArray(content, view.getViewer());
  }
}
