package tc.oc.pgm.tablist;

import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.TranslatableComponent;
import net.kyori.text.format.TextColor;
import net.kyori.text.format.TextDecoration;
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
        TextComponent.builder()
            .append(String.valueOf(match.getParticipants().size()), TextColor.WHITE)
            .append("/", TextColor.DARK_GRAY)
            .append(String.valueOf(match.getMaxPlayers()), TextColor.GRAY)
            .append(" ")
            .append(
                TranslatableComponent.of(
                    "match.info.players", TextColor.YELLOW, TextDecoration.BOLD))
            .build();
    return TextTranslations.toBaseComponentArray(content, view.getViewer());
  }
}
