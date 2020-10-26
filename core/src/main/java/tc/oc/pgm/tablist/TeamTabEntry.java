package tc.oc.pgm.tablist;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.md_5.bungee.api.chat.BaseComponent;
import tc.oc.pgm.teams.Team;
import tc.oc.pgm.util.tablist.DynamicTabEntry;
import tc.oc.pgm.util.tablist.TabView;
import tc.oc.pgm.util.text.TextFormatter;
import tc.oc.pgm.util.text.TextTranslations;

public class TeamTabEntry extends DynamicTabEntry {

  private final Team team;

  protected TeamTabEntry(Team team) {
    this.team = team;
  }

  @Override
  public BaseComponent[] getContent(TabView view) {
    Component content =
        Component.text()
            .append(Component.text(String.valueOf(team.getPlayers().size()), NamedTextColor.WHITE))
            .append(Component.text("/", NamedTextColor.DARK_GRAY))
            .append(Component.text(String.valueOf(team.getMaxPlayers()), NamedTextColor.GRAY))
            .append(Component.space())
            .append(
                Component.text(
                    team.getShortName(),
                    TextFormatter.convert(team.getColor()),
                    TextDecoration.BOLD))
            .build();

    return TextTranslations.toBaseComponentArray(content, view.getViewer());
  }
}
