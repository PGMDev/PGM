package tc.oc.pgm.tablist;

import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.util.tablist.DynamicTabEntry;
import tc.oc.pgm.util.tablist.TabView;

public class FreeForAllTabEntry extends DynamicTabEntry {

  private final Match match;

  public FreeForAllTabEntry(Match match) {
    this.match = match;
  }

  @Override
  public Component getContent(TabView view) {
    return text()
        .append(text(match.getParticipants().size(), NamedTextColor.WHITE))
        .append(text("/", NamedTextColor.DARK_GRAY))
        .append(text(match.getMaxPlayers(), NamedTextColor.GRAY))
        .append(space())
        .append(translatable("match.info.players", NamedTextColor.YELLOW, TextDecoration.BOLD))
        .build();
  }
}
