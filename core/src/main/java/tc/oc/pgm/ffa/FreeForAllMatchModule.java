package tc.oc.pgm.ffa;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;

import com.google.common.collect.Lists;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.events.PlayerPartyChangeEvent;
import tc.oc.pgm.join.JoinHandler;
import tc.oc.pgm.join.JoinMatchModule;
import tc.oc.pgm.join.JoinRequest;
import tc.oc.pgm.join.JoinResult;
import tc.oc.pgm.join.JoinResultOption;
import tc.oc.pgm.match.QueuedParty;
import tc.oc.pgm.start.StartMatchModule;
import tc.oc.pgm.start.UnreadyReason;
import tc.oc.pgm.util.bukkit.Sounds;

@ListenerScope(MatchScope.LOADED)
public class FreeForAllMatchModule implements MatchModule, Listener, JoinHandler {

  // 10 different colors that tributes are allowed to have
  private static final ChatColor[] COLORS = new ChatColor[] {
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

  record NeedMorePlayers(int players) implements UnreadyReason {
    @Override
    public Component getReason() {
      if (players == 1) {
        return translatable("join.wait.singular", text(players, NamedTextColor.AQUA));
      } else {
        return translatable("join.wait.plural", text(players, NamedTextColor.AQUA));
      }
    }

    @Override
    public boolean canForceStart() {
      return true;
    }
  }

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

    if (options.colors) {
      final List<ChatColor> colors = Lists.newArrayList(COLORS);
      Collections.shuffle(colors);
      colors.forEach(this.colors::push);
    } else {
      colors.add(ChatColor.YELLOW);
    }
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
    this.minPlayers = minPlayers == null ? options.minPlayers : minPlayers;
    updateReadiness();
  }

  public void setMaxPlayers(@Nullable Integer maxPlayers, @Nullable Integer maxOverfill) {
    this.maxPlayers = maxPlayers == null ? options.maxPlayers : maxPlayers;
    this.maxOverfill = maxOverfill == null ? options.maxOverfill : maxOverfill;
    match.setMaxPlayers(getMaxPlayers());
  }

  @Override
  public void load() {
    match.needModule(JoinMatchModule.class).setJoinHandler(this);
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

  protected boolean canPriorityKick(JoinRequest request) {
    if (!join().canPriorityKick(request)) return false;

    for (MatchPlayer player : match.getParticipants()) {
      if (join().canBePriorityKicked(player)) return true;
    }

    return false;
  }

  protected boolean priorityKick(JoinRequest request) {
    if (!join().canPriorityKick(request)) return false;

    List<MatchPlayer> kickable = new ArrayList<>();
    for (MatchPlayer player : match.getParticipants()) {
      if (join().canBePriorityKicked(player)) kickable.add(player);
    }
    if (kickable.isEmpty()) return false;

    MatchPlayer kickMe = kickable.get(match.getRandom().nextInt(kickable.size()));

    kickMe.sendWarning(translatable("leave.ok.priorityKick"));
    kickMe.playSound(Sounds.MATCH_KICK);

    match.setParty(kickMe, match.getDefaultParty());

    return true;
  }

  @Override
  public @Nullable JoinResult queryJoin(MatchPlayer joining, JoinRequest request) {
    if (request.getTeam() != null) return null;

    if (joining.getParty() instanceof Tribute) {
      return JoinResultOption.REDUNDANT;
    }

    if (request.has(JoinRequest.Flag.FORCE)) {
      return JoinResultOption.JOINED;
    }

    int players = match.getParticipants().size();

    if (players >= getMaxPlayers()) {
      if (!join().canJoinFull(request)) {
        return JoinResultOption.FULL;
      }
      if (players >= getMaxOverfill() && !canPriorityKick(request)) {
        return JoinResultOption.FULL;
      }
      return new FfaJoinResult(JoinResultOption.JOINED, true);
    }

    return JoinResultOption.JOINED;
  }

  @Override
  public boolean join(MatchPlayer joining, JoinRequest request, JoinResult result) {
    switch (result.getOption()) {
      case FULL:
        joining.sendWarning(translatable("join.err.full"));
        return true;

      case REDUNDANT:
        joining.sendWarning(translatable("join.err.alreadyJoined"));
        return true;
    }

    if (!result.isSuccess()) return false;

    if (!match.setParty(joining, getTribute(joining), request)) {
      return false;
    }

    if (result instanceof FfaJoinResult && ((FfaJoinResult) result).priorityKickRequired()) {
      priorityKick(request);
    }

    return true;
  }

  @Override
  public void queuedJoin(QueuedParty queue) {
    for (MatchPlayer player : queue.getOrderedPlayers()) {
      join(player, JoinRequest.fromPlayer(player, null, JoinRequest.Flag.IGNORE_QUEUE));
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPartyChange(PlayerPartyChangeEvent event) {
    if (event.getNewParty() instanceof Tribute) {
      event.getPlayer().sendMessage(translatable("join.ok"));
    }
    updateReadiness();
  }

  public static class FfaJoinResult implements JoinResult {
    private final JoinResultOption status;
    private final boolean priorityKick;

    public FfaJoinResult(JoinResultOption status, boolean priorityKick) {
      this.status = status;
      this.priorityKick = priorityKick;
    }

    @Override
    public boolean isSuccess() {
      return status.isSuccess();
    }

    @Override
    public JoinResultOption getOption() {
      return status;
    }

    public boolean priorityKickRequired() {
      return priorityKick;
    }
  }
}
