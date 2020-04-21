package tc.oc.pgm.tablist;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import tc.oc.pgm.teams.Team;
import tc.oc.pgm.util.component.ComponentUtils;
import tc.oc.pgm.util.component.types.PersonalizedText;
import tc.oc.pgm.util.tablist.DynamicTabEntry;
import tc.oc.pgm.util.tablist.TabView;

public class TeamTabEntry extends DynamicTabEntry {

  private final Team team;

  protected TeamTabEntry(Team team) {
    this.team = team;
  }

  @Override
  public BaseComponent getContent(TabView view) {
    return new PersonalizedText(
            new PersonalizedText(String.valueOf(team.getPlayers().size()), ChatColor.WHITE),
            new PersonalizedText("/", ChatColor.DARK_GRAY),
            new PersonalizedText(String.valueOf(team.getMaxPlayers()), ChatColor.GRAY),
            new PersonalizedText(
                " " + team.getShortName(), ComponentUtils.convert(team.getColor()), ChatColor.BOLD))
        .render(view.getViewer());
  }
}
