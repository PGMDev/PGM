package tc.oc.pgm.ffa;

import java.util.*;
import javax.annotation.Nullable;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Sound;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import tc.oc.component.Component;
import tc.oc.component.types.PersonalizedText;
import tc.oc.component.types.PersonalizedTranslatable;
import tc.oc.identity.PlayerIdentityChangeEvent;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.events.PartyRenameEvent;
import tc.oc.pgm.events.PlayerPartyChangeEvent;
import tc.oc.pgm.join.*;
import tc.oc.pgm.match.*;
import tc.oc.pgm.start.StartMatchModule;
import tc.oc.pgm.start.UnreadyReason;
import tc.oc.server.NullCommandSender;

@ListenerScope(MatchScope.LOADED)
public class FreeForAllMatchModule extends MatchModule implements Listener, JoinHandler {

  class NeedMorePlayers implements UnreadyReason {
    final int players;

    NeedMorePlayers(int players) {
      this.players = players;
    }

    @Override
    public Component getReason() {
      if (players == 1) {
        return new PersonalizedTranslatable(
            "start.needMorePlayers.ffa.singular",
            new PersonalizedText(String.valueOf(players), ChatColor.AQUA));
      } else {
        return new PersonalizedTranslatable(
            "start.needMorePlayers.ffa.plural",
            new PersonalizedText(String.valueOf(players), ChatColor.AQUA));
      }
    }

    @Override
    public boolean canForceStart() {
      return true;
    }

    @Override
    public String toString() {
      return getClass().getSimpleName() + "{players=" + players + "}";
    }
  };

  private final FreeForAllOptions options;
  private @Nullable Integer minPlayers, maxPlayers, maxOverfill;
  private int minPlayersNeeded = Integer.MAX_VALUE;
  private final Map<UUID, Tribute> tributes = new HashMap<>();

  public FreeForAllMatchModule(Match match, FreeForAllOptions options) {
    super(match);
    this.options = options;
  }

  public FreeForAllOptions getOptions() {
    return options;
  }

  public int getMinPlayers() {
    return minPlayers != null ? minPlayers : options.minPlayers;
  }

  public int getMaxPlayers() {
    return maxPlayers != null ? maxPlayers : options.maxPlayers;
  }

  public int getMaxOverfill() {
    return maxOverfill != null ? maxOverfill : options.maxOverfill;
  }

  public void setMinPlayers(@Nullable Integer minPlayers) {
    this.minPlayers = minPlayers;
    updateReadiness();
  }

  public void setMaxPlayers(@Nullable Integer maxPlayers, @Nullable Integer maxOverfill) {
    this.maxPlayers = maxPlayers;
    this.maxOverfill = maxOverfill;
    getMatch().setMaxPlayers(getMaxPlayers());
  }

  @Override
  public void load() {
    super.load();

    getMatch().needMatchModule(JoinMatchModule.class).registerHandler(this);

    getMatch().setMaxPlayers(getMaxPlayers());
    updateReadiness();
  }

  protected void updateReadiness() {
    if (getMatch().hasStarted()) return;

    int players = 0;
    for (Competitor competitor : getMatch().getCompetitors()) {
      if (competitor instanceof Tribute) {
        players += competitor.getPlayers().size();
      }
    }

    int playersNeeded = getMinPlayers() - players;

    final StartMatchModule smm = getMatch().needMatchModule(StartMatchModule.class);
    if (playersNeeded > 0) {
      smm.addUnreadyReason(new NeedMorePlayers(playersNeeded));
    } else {
      smm.removeUnreadyReason(NeedMorePlayers.class);

      // Whenever playersNeeded reaches a new minimum, reset the unready timeout
      if (playersNeeded < minPlayersNeeded) {
        minPlayersNeeded = playersNeeded;
        smm.restartUnreadyTimeout();
      }
    }
  }

  protected Tribute getTribute(MatchPlayer player) {
    Tribute tribute = tributes.get(player.getPlayerId());
    if (tribute == null) {
      tribute = new Tribute(player);
      tributes.put(player.getPlayerId(), tribute);
      logger.fine("Created " + tribute);
    }
    return tribute;
  }

  protected boolean canPriorityKick(MatchPlayer joining) {
    if (!joining.canPriorityKick()) return false;

    for (MatchPlayer player : getMatch().getParticipatingPlayers()) {
      if (!player.canPriorityKick()) return true;
    }

    return false;
  }

  protected boolean priorityKick(MatchPlayer joining) {
    if (!joining.canPriorityKick()) return false;

    List<MatchPlayer> kickable = new ArrayList<>();
    for (MatchPlayer player : getMatch().getParticipatingPlayers()) {
      if (!player.canPriorityKick()) kickable.add(player);
    }
    if (kickable.isEmpty()) return false;

    MatchPlayer kickMe = kickable.get(getMatch().getRandom().nextInt(kickable.size()));

    kickMe.sendWarning(new PersonalizedTranslatable("gameplay.ffa.kickedForPremium"), false);
    kickMe.playSound(Sound.VILLAGER_HIT, kickMe.getBukkit().getLocation(), 1, 1);

    getMatch().setPlayerParty(kickMe, getMatch().getDefaultParty());

    return true;
  }

  @Override
  public @Nullable GenericJoinResult queryJoin(
      MatchPlayer joining, @Nullable Competitor chosenParty) {
    if (chosenParty != null) return null;

    if (joining.getParty() instanceof Tribute) {
      return GenericJoinResult.Status.REDUNDANT.toResult();
    }

    int players = getMatch().getParticipatingPlayers().size();

    if (joining.canJoinFull()) {
      if (players >= getMaxOverfill() && !canPriorityKick(joining)) {
        return GenericJoinResult.Status.FULL.toResult();
      }
    } else {
      if (players >= getMaxPlayers()) {
        return GenericJoinResult.Status.FULL.toResult();
      }
    }

    return GenericJoinResult.Status.JOINED.toResult();
  }

  @Override
  public boolean join(MatchPlayer joining, @Nullable Competitor chosenParty, JoinResult result) {
    if (result instanceof GenericJoinResult) {
      GenericJoinResult genericResult = (GenericJoinResult) result;
      switch (genericResult.getStatus()) {
        case FULL:
          joining.sendWarning(new PersonalizedTranslatable("autoJoin.matchFull"), false);
          return true;

        case REDUNDANT:
          joining.sendWarning(
              new PersonalizedTranslatable("command.gameplay.join.alreadyJoined"), false);
          return true;
      }
    }

    if (!result.isSuccess()) return false;

    if (!forceJoin(joining)) {
      return false;
    }

    if (result instanceof GenericJoinResult
        && ((GenericJoinResult) result).priorityKickRequired()) {
      priorityKick(joining);
    }

    return true;
  }

  @Override
  public void queuedJoin(QueuedParticipants queue) {
    for (MatchPlayer player : queue.getOrderedPlayers()) {
      join(player, null, queryJoin(player, null));
    }
  }

  @Override
  public boolean forceJoin(MatchPlayer joining, @Nullable Competitor forcedParty) {
    return forcedParty == null && forceJoin(joining);
  }

  public boolean forceJoin(MatchPlayer joining) {
    if (joining.getParty() instanceof Tribute) {
      joining.sendWarning(
          new PersonalizedTranslatable("command.gameplay.join.alreadyJoined"), false);
    }

    return getMatch().setPlayerParty(joining, getTribute(joining));
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPartyChange(PlayerPartyChangeEvent event) {
    if (event.getNewParty() instanceof Tribute) {
      event.getPlayer().sendMessage(new PersonalizedTranslatable("ffa.join"));
    }
    updateReadiness();
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onIdentityChange(PlayerIdentityChangeEvent event) {
    MatchPlayer player = getMatch().getPlayer(event.getPlayer());
    if (player != null && player.getParty() instanceof Tribute) {
      getMatch()
          .callEvent(
              new PartyRenameEvent(
                  player.getParty(),
                  event.getOldIdentity().getName(NullCommandSender.INSTANCE),
                  event.getNewIdentity().getName(NullCommandSender.INSTANCE)));
    }
  }
}
