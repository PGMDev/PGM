package tc.oc.pgm.ffa;

import com.google.common.collect.Lists;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.events.PlayerPartyChangeEvent;
import tc.oc.pgm.join.GenericJoinResult;
import tc.oc.pgm.join.JoinHandler;
import tc.oc.pgm.join.JoinMatchModule;
import tc.oc.pgm.join.JoinResult;
import tc.oc.pgm.join.QueuedParticipants;
import tc.oc.pgm.start.StartMatchModule;
import tc.oc.pgm.start.UnreadyReason;
import tc.oc.util.bukkit.chat.Sound;
import tc.oc.util.bukkit.component.Component;
import tc.oc.util.bukkit.component.types.PersonalizedText;
import tc.oc.util.bukkit.component.types.PersonalizedTranslatable;

@ListenerScope(MatchScope.LOADED)
public class FreeForAllMatchModule implements MatchModule, Listener, JoinHandler {

  // 10 different colors that tributes are allowed to have
  private static final ChatColor[] COLORS =
      new ChatColor[] {
        ChatColor.RED,
        ChatColor.BLUE,
        ChatColor.GREEN,
        ChatColor.YELLOW,
        ChatColor.LIGHT_PURPLE,
        ChatColor.GOLD,
        ChatColor.DARK_GREEN,
        ChatColor.DARK_AQUA,
        ChatColor.DARK_PURPLE,
        ChatColor.DARK_RED
      };

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
            new PersonalizedText(String.valueOf(players), net.md_5.bungee.api.ChatColor.AQUA));
      } else {
        return new PersonalizedTranslatable(
            "start.needMorePlayers.ffa.plural",
            new PersonalizedText(String.valueOf(players), net.md_5.bungee.api.ChatColor.AQUA));
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

  private final Match match;
  private final FreeForAllOptions options;
  private @Nullable Integer minPlayers, maxPlayers, maxOverfill;
  private int minPlayersNeeded = Integer.MAX_VALUE;
  private final Map<UUID, Tribute> tributes = new HashMap<>();
  private final Deque<ChatColor> colors = new ArrayDeque<>();
  private JoinMatchModule jmm;

  public FreeForAllMatchModule(Match match, FreeForAllOptions options) {
    this.match = match;
    this.options = options;

    final List<ChatColor> colors = Lists.newArrayList(COLORS);
    Collections.shuffle(colors);
    colors.forEach(this.colors::push);
  }

  private JoinMatchModule join() {
    if (jmm == null) {
      jmm = match.needModule(JoinMatchModule.class);
    }
    return jmm;
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
    match.setMaxPlayers(getMaxPlayers());
  }

  @Override
  public void load() {
    match.needModule(JoinMatchModule.class).registerHandler(this);
    match.setMaxPlayers(getMaxPlayers());
    updateReadiness();
  }

  protected void updateReadiness() {
    if (match.isRunning()) return;

    int players = 0;
    for (Competitor competitor : match.getCompetitors()) {
      if (competitor instanceof Tribute) {
        players += competitor.getPlayers().size();
      }
    }

    int playersNeeded = getMinPlayers() - players;

    final StartMatchModule smm = match.needModule(StartMatchModule.class);
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
    Tribute tribute = tributes.get(player.getId());
    if (tribute == null) {
      final ChatColor color = colors.pollFirst();
      if (color != null) colors.addLast(color);
      tribute = new Tribute(player, color);
      tributes.put(player.getId(), tribute);
      match.getLogger().fine("Created " + tribute);
    }
    return tribute;
  }

  protected boolean canPriorityKick(MatchPlayer joining) {
    if (!join().canPriorityKick(joining)) return false;

    for (MatchPlayer player : match.getParticipants()) {
      if (!join().canPriorityKick(player)) return true;
    }

    return false;
  }

  protected boolean priorityKick(MatchPlayer joining) {
    if (!join().canPriorityKick(joining)) return false;

    List<MatchPlayer> kickable = new ArrayList<>();
    for (MatchPlayer player : match.getParticipants()) {
      if (!join().canPriorityKick(player)) kickable.add(player);
    }
    if (kickable.isEmpty()) return false;

    MatchPlayer kickMe = kickable.get(match.getRandom().nextInt(kickable.size()));

    kickMe.sendWarning(new PersonalizedTranslatable("gameplay.ffa.kickedForPremium"), false);
    kickMe.playSound(new Sound("mob.villager.hit"));

    match.setParty(kickMe, match.getDefaultParty());

    return true;
  }

  @Override
  public @Nullable GenericJoinResult queryJoin(
      MatchPlayer joining, @Nullable Competitor chosenParty) {
    if (chosenParty != null) return null;

    if (joining.getParty() instanceof Tribute) {
      return GenericJoinResult.Status.REDUNDANT.toResult();
    }

    int players = match.getParticipants().size();

    if (join().canJoinFull(joining)) {
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

    return match.setParty(joining, getTribute(joining));
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPartyChange(PlayerPartyChangeEvent event) {
    if (event.getNewParty() instanceof Tribute) {
      event.getPlayer().sendMessage(new PersonalizedTranslatable("ffa.join"));
    }
    updateReadiness();
  }
}
