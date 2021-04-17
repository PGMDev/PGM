package tc.oc.pgm.tablist;

import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import tc.oc.pgm.teams.Team;
import tc.oc.pgm.util.tablist.DynamicTabEntry;
import tc.oc.pgm.util.tablist.TabView;
import tc.oc.pgm.util.text.TextFormatter;

public class TeamTabEntry extends DynamicTabEntry {

  private final Team team;

  protected TeamTabEntry(Team team) {
    this.team = team;
  }

  @Override
  public Component getContent(TabView view) {
    return text()
        .append(text(team.getPlayers().size(), NamedTextColor.WHITE))
        .append(text("/", NamedTextColor.DARK_GRAY))
        .append(text(team.getMaxPlayers(), NamedTextColor.GRAY))
        .append(space())
        .append(
            text(team.getShortName(), TextFormatter.convert(team.getColor()), TextDecoration.BOLD))
        .build();
  }
}
