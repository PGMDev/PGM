package tc.oc.pgm.tablist;

import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;

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
        text()
            .append(text(team.getPlayers().size(), NamedTextColor.WHITE))
            .append(text("/", NamedTextColor.DARK_GRAY))
            .append(text(team.getMaxPlayers(), NamedTextColor.GRAY))
            .append(space())
            .append(
                text(
                    team.getShortName(),
                    TextFormatter.convert(team.getColor()),
                    TextDecoration.BOLD))
            .build();

    return TextTranslations.toBaseComponentArray(content, view.getViewer());
  }
}
