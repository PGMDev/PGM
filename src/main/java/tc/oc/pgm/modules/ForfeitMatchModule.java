package tc.oc.pgm.modules;

import java.util.Optional;
import java.util.stream.Collectors;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import org.bukkit.command.CommandSender;
import tc.oc.component.Component;
import tc.oc.component.types.PersonalizedText;
import tc.oc.component.types.PersonalizedTranslatable;
import tc.oc.named.NameStyle;
import tc.oc.pgm.Config;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.match.MatchModule;
import tc.oc.pgm.match.MatchModuleFactory;
import tc.oc.pgm.module.ModuleLoadException;
import tc.oc.pgm.teams.TeamMatchModule;
import tc.oc.pgm.timelimit.TimeLimitMatchModule;

public class ForfeitMatchModule extends MatchModule {

  public static class Factory implements MatchModuleFactory<ForfeitMatchModule> {
    @Override
    public ForfeitMatchModule createMatchModule(Match match) throws ModuleLoadException {
      return new ForfeitMatchModule(match);
    }
  }

  private boolean enabled;
  private boolean broadcastEnabled;
  private int maxPlayers;

  public ForfeitMatchModule(Match match) {
    super(match);
    enabled = Config.Forfeit.enabled();
    broadcastEnabled = Config.Forfeit.isBroadcastEnabled();
    maxPlayers = Config.Forfeit.getMaxPlayerCount();
  }

  public boolean isEnabled() {
    return enabled;
  }

  public boolean setEnabled(boolean on) {
    enabled = on;
    return isEnabled();
  }

  public int getTotalForfeits() {
    return match.getParticipants().stream()
        .filter(MatchPlayer::hasForfeit)
        .collect(Collectors.counting())
        .intValue();
  }

  public int getMatchPlayerCount() {
    return match.getParticipants().size();
  }

  public void forfeit(MatchPlayer player, CommandSender sender) {
    if (isForfietPossible()) {
      player.setForfeit(!player.hasForfeit());
      broadcastForfeit(player, player.hasForfeit());
      calcForfeit(player);
    } else {
      player.sendWarning(new PersonalizedTranslatable("command.forfeit.notPossible"));
    }
  }

  /*
   * Current Restrictions:
   * 1. If the match has a time limit.
   * 2. If the match does not contain teams.
   * 3. If player count is more than the max-player config.
   */
  private boolean isForfietPossible() {
    Optional<TeamMatchModule> tmm = match.getModule(TeamMatchModule.class);
    Optional<TimeLimitMatchModule> tlmm = match.getModule(TimeLimitMatchModule.class);

    boolean hasTeams = tmm.isPresent();
    boolean hasTimeLimit = tlmm.isPresent() && tlmm.get().hasTimeLimit();
    boolean underPlayerLimit = maxPlayers != 0 ? getMatchPlayerCount() <= maxPlayers : true;

    return isEnabled() && match.isRunning() && !hasTimeLimit && hasTeams && underPlayerLimit;
  }

  private void broadcastForfeit(MatchPlayer voter, boolean forfeit) {
    if (broadcastEnabled) {
      match
          .getParticipants()
          .forEach(p -> p.sendMessage(getForfeitBroadcast(voter, forfeit, p.hasForfeit())));
      match.getObservers().forEach(o -> o.sendMessage(getForfeitBroadcast(voter, forfeit, false)));
    } else {
      voter.sendMessage(getForfeitCommandMessage(forfeit));
    }
  }

  public void calcForfeit(MatchPlayer voter) {
    boolean remaining =
        match.getParticipants().stream()
            .filter(player -> !player.hasForfeit())
            .findAny()
            .isPresent();

    if (!remaining) {
      match.finish(null);
    }
  }

  private Component getForfeitStatusComponent() {
    Component totalForfeits = new PersonalizedText("" + getTotalForfeits()).color(ChatColor.GREEN);
    Component totalNeeded = new PersonalizedText("" + getMatchPlayerCount()).color(ChatColor.RED);
    return new PersonalizedTranslatable("forfeit.broadcast.stats", totalForfeits, totalNeeded);
  }

  private Component getForfeitCommandMessage(boolean forfeit) {
    return new PersonalizedTranslatable(
            forfeit ? "command.forfeit.voted" : "command.forfeit.withdraw",
            getForfeitStatusComponent())
        .getPersonalizedText()
        .color(ChatColor.GRAY);
  }

  private Component getForfeitBroadcast(MatchPlayer voted, boolean forfeit, boolean hasVoted) {
    Component broadcast =
        new PersonalizedTranslatable(
                forfeit ? "forfeit.broadcast.vote" : "forfeit.broadcast.withdraw",
                voted.getStyledName(NameStyle.COLOR),
                getForfeitStatusComponent())
            .getPersonalizedText()
            .color(ChatColor.GRAY);

    if (hasVoted) {
      return broadcast;
    } else {
      return broadcast
          .clickEvent(ClickEvent.Action.RUN_COMMAND, "/forfeit")
          .hoverEvent(
              HoverEvent.Action.SHOW_TEXT,
              new PersonalizedTranslatable("forfeit.broadcast.hover")
                  .getPersonalizedText()
                  .color(ChatColor.GRAY)
                  .render());
    }
  }
}
