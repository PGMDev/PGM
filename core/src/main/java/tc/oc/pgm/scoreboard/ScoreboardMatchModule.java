package tc.oc.pgm.scoreboard;

import static tc.oc.pgm.util.Assert.assertNotNull;
import static tc.oc.pgm.util.bukkit.MiscUtils.MISC_UTILS;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.scoreboard.NameTagVisibility;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.match.factory.MatchModuleFactory;
import tc.oc.pgm.api.module.exception.ModuleLoadException;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.party.event.PartyAddEvent;
import tc.oc.pgm.api.party.event.PartyRemoveEvent;
import tc.oc.pgm.api.party.event.PartyRenameEvent;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.events.PlayerPartyChangeEvent;
import tc.oc.pgm.ffa.FreeForAllMatchModule;
import tc.oc.pgm.teams.TeamMatchModule;
import tc.oc.pgm.util.StringUtils;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.text.TextTranslations;

@ListenerScope(MatchScope.LOADED)
public class ScoreboardMatchModule implements MatchModule, Listener {

  public static class Factory implements MatchModuleFactory<ScoreboardMatchModule> {
    @Override
    public Collection<Class<? extends MatchModule>> getWeakDependencies() {
      return ImmutableList.of(TeamMatchModule.class, FreeForAllMatchModule.class);
    }

    @Override
    public ScoreboardMatchModule createMatchModule(Match match) throws ModuleLoadException {
      return new ScoreboardMatchModule(match);
    }
  }

  private final Match match;
  private final Map<Party, Scoreboard> partyScoreboards = new HashMap<>();
  private final Scoreboard hiddenScoreboard;

  private ScoreboardMatchModule(Match match) {
    this.match = match;
    this.hiddenScoreboard = PGM.get().getServer().getScoreboardManager().getNewScoreboard();
  }

  @Override
  public void load() {
    for (Party party : match.getParties()) {
      addPartyScoreboard(party);
    }
  }

  protected String getScoreboardTeamName(@Nullable Party party) {
    return party == null ? null : StringUtils.truncate(party.getDefaultName(), 16);
  }

  protected void updatePartyScoreboardTeam(Party party, Team team, boolean forObservers) {
    match.getLogger().fine("Updating scoreboard team " + toString(team) + " for party " + party);

    team.setDisplayName(
        StringUtils.truncate(TextTranslations.translateLegacy(party.getName(NameStyle.FANCY)), 32));
    team.setPrefix(party.getColor().toString());
    team.setSuffix(ChatColor.WHITE.toString());
    MISC_UTILS.initScoreboardTeam(team, party.getTextColor());

    team.setCanSeeFriendlyInvisibles(true);
    team.setAllowFriendlyFire(match.getFriendlyFire());

    if (!forObservers && party instanceof Competitor) {
      NameTagVisibility nameTags = ((Competitor) party).getNameTagVisibility();

      team.setNameTagVisibility(nameTags);
    } else {
      team.setNameTagVisibility(NameTagVisibility.ALWAYS);
    }
  }

  protected Team createPartyScoreboardTeam(
      Party party, Scoreboard scoreboard, boolean forObservers) {
    match
        .getLogger()
        .fine("Creating team for party " + party + " on scoreboard " + toString(scoreboard));

    Team team = scoreboard.registerNewTeam(getScoreboardTeamName(party));
    updatePartyScoreboardTeam(party, team, forObservers);
    for (MatchPlayer player : party.getPlayers()) {
      team.addEntry(player.getNameLegacy());
    }

    return team;
  }

  protected void addPartyScoreboard(Party newParty) {
    // Create the new party's scoreboard
    Scoreboard newScoreboard = PGM.get().getServer().getScoreboardManager().getNewScoreboard();
    match
        .getLogger()
        .fine("Created scoreboard " + toString(newScoreboard) + " for party " + newParty);

    // Add all previously existing parties to the new scoreboard (but not the new party)
    for (Party oldParty : partyScoreboards.keySet()) {
      createPartyScoreboardTeam(oldParty, newScoreboard, !(newParty instanceof Competitor));
    }

    // Register the new party's scoreboard
    partyScoreboards.put(newParty, newScoreboard);

    // Add the new party to all scoreboards (including its own scoreboard)
    createPartyScoreboardTeam(newParty, hiddenScoreboard, false);
    for (Map.Entry<Party, Scoreboard> entry : partyScoreboards.entrySet()) {
      createPartyScoreboardTeam(
          newParty, entry.getValue(), !(entry.getKey() instanceof Competitor));
    }
  }

  protected void removePartyScoreboard(Party party) {
    // Remove and tear down the leaving party's scoreboard
    Scoreboard scoreboard = partyScoreboards.remove(party);
    if (scoreboard != null) {
      for (Objective objective : scoreboard.getObjectives()) {
        objective.unregister();
      }
      for (Team team : scoreboard.getTeams()) {
        team.unregister();
      }
    }

    match.getLogger().fine("Removed scoreboard " + toString(scoreboard) + " for party " + party);

    // Remove the leaving party from all other scoreboards
    String name = getScoreboardTeamName(party);
    for (Scoreboard otherScoreboard : getScoreboards()) {
      Team team = otherScoreboard.getTeam(name);
      if (team != null) {
        match
            .getLogger()
            .fine("Unregistering team "
                + toString(team)
                + " from scoreboard "
                + toString(otherScoreboard));
        team.unregister();
      }
    }
  }

  protected void changePlayerScoreboard(
      MatchPlayer player, @Nullable Party oldParty, @Nullable Party newParty) {
    // Change the player's team in all scoreboards
    String teamName = getScoreboardTeamName(newParty != null ? newParty : oldParty);
    for (Scoreboard scoreboard : getScoreboards()) {
      if (newParty != null) {
        Team team = scoreboard.getTeam(teamName);
        match
            .getLogger()
            .fine("Adding player "
                + player
                + " to team "
                + toString(team)
                + " on scoreboard "
                + toString(scoreboard));
        team.addEntry(player.getNameLegacy());
      } else if (oldParty != null) {
        Team team = scoreboard.getTeam(teamName);
        match
            .getLogger()
            .fine("Removing player "
                + player
                + " from team "
                + toString(team)
                + " on scoreboard "
                + toString(scoreboard));
        // FIXME: Removing this fixes white tab list entries when cycling
        // team.removePlayer(player.getBukkit());
      }
    }

    // Set the player's scoreboard
    if (newParty != null) {
      updatePlayer(player, newParty, true);
    }
  }

  protected void updatePlayer(MatchPlayer player, Party party, boolean show) {
    if (show) {
      Scoreboard scoreboard = partyScoreboards.get(party);
      match.getLogger().fine("Setting player " + player + " to scoreboard " + toString(scoreboard));
      player.getBukkit().setScoreboard(scoreboard);
    } else {
      match.getLogger().fine("Setting player " + player + " to hidden scoreboard");
      player.getBukkit().setScoreboard(getHiddenScoreboard());
    }
  }

  public Scoreboard getHiddenScoreboard() {
    return hiddenScoreboard;
  }

  public Iterable<Scoreboard> getScoreboards() {
    return Iterables.concat(partyScoreboards.values(), Collections.singleton(hiddenScoreboard));
  }

  public Scoreboard getScoreboard(Party party) {
    return assertNotNull(partyScoreboards.get(party));
  }

  public void updatePartyScoreboardTeam(Party party) {
    String teamName = getScoreboardTeamName(party);
    updatePartyScoreboardTeam(party, hiddenScoreboard.getTeam(teamName), false);
    for (Map.Entry<Party, Scoreboard> entry : partyScoreboards.entrySet()) {
      updatePartyScoreboardTeam(
          party, entry.getValue().getTeam(teamName), !(entry.getKey() instanceof Competitor));
    }
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onPartyAdd(PartyAddEvent event) {
    addPartyScoreboard(event.getParty());
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPartyRemove(PartyRemoveEvent event) {
    removePartyScoreboard(event.getParty());
  }

  @EventHandler
  public void onPartyRename(PartyRenameEvent event) {
    updatePartyScoreboardTeam(event.getParty());
  }

  @EventHandler
  public void onPlayerChangeParty(PlayerPartyChangeEvent event) {
    changePlayerScoreboard(event.getPlayer(), event.getOldParty(), event.getNewParty());
  }

  private static String toString(Scoreboard scoreboard) {
    return scoreboard == null
        ? "null"
        : "bukkit." + scoreboard.getClass().getSimpleName() + "{" + scoreboard.hashCode() + "}";
  }

  private static String toString(Team team) {
    return team == null
        ? "null"
        : "bukkit." + team.getClass().getSimpleName() + "{" + team.getName() + "}";
  }
}
