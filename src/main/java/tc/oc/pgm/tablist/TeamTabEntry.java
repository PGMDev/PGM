package tc.oc.pgm.tablist;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import tc.oc.component.types.PersonalizedText;
import tc.oc.pgm.teams.Team;
import tc.oc.tablist.DynamicTabEntry;
import tc.oc.tablist.TabView;
import tc.oc.util.collection.DefaultProvider;
import tc.oc.util.components.ComponentUtils;

public class TeamTabEntry extends DynamicTabEntry {

  public static class Factory implements DefaultProvider<Team, TeamTabEntry> {
    @Override
    public TeamTabEntry get(Team key) {
      return new TeamTabEntry(key);
    }
  }

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
