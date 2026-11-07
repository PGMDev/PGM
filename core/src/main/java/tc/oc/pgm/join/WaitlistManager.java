package tc.oc.pgm.join;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;
import static net.kyori.adventure.text.event.ClickEvent.runCommand;
import static net.kyori.adventure.text.event.HoverEvent.showText;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.map.Gamemode;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.blitz.BlitzMatchModule;
import tc.oc.pgm.events.PlayerPartyChangeEvent;
import tc.oc.pgm.events.PlayerWaitlistEvent;
import tc.oc.pgm.match.ObserverParty;
import tc.oc.pgm.util.bukkit.Sounds;

/** Players waiting for a slot in a full match, let in as slots free up. */
public class WaitlistManager {

  private final Match match;
  private final JoinMatchModule join;
  private final List<Entry> waitlist = new ArrayList<>();
  private boolean drainScheduled;

  WaitlistManager(Match match, JoinMatchModule join) {
    this.match = match;
    this.join = join;
  }

  boolean canAdd(MatchPlayer joining, JoinRequest request) {
    BlitzMatchModule blitz = match.getModule(BlitzMatchModule.class);
    return PGM.get().getConfiguration().shouldWaitlistJoin()
        && (blitz == null || blitz.canJoin(joining, request))
        && !match.isFinished()
        && joining.isObserving()
        && request.getTeam() == null
        && request.getPlayerCount() == 1
        && !request.has(JoinRequest.Flag.SQUAD);
  }

  /** Players on the waitlist, in the order they will be let in. */
  public List<Entry> getEntries() {
    return List.copyOf(waitlist);
  }

  /** Removes the player from the waitlist without notifying them. */
  public boolean remove(MatchPlayer player) {
    int index = indexOf(player);
    if (index < 0) return false;
    waitlist.remove(index);
    return true;
  }

  /** Puts a removed entry back in its original place, without a message or event. */
  public boolean restore(Entry entry) {
    MatchPlayer player = match.getPlayer(entry.playerId());
    if (player == null || indexOf(player) >= 0 || !canAdd(player, entry.request())) return false;

    waitlist.add(indexFor(entry), entry);
    scheduleDrain();
    return true;
  }

  /** Waitlists a player who was priority kicked to observers, ahead of everyone else. */
  public void addKicked(MatchPlayer player) {
    JoinRequest request = JoinRequest.fromPlayer(player, null);
    if (canAdd(player, request)) add(player, request, true);
  }

  /** The player's position on the waitlist starting at 1, or 0 if they aren't on it. */
  public int getPosition(MatchPlayer player) {
    return indexOf(player) + 1;
  }

  /** Whether joining without picking a team would put the player on the waitlist. */
  public boolean wouldWaitlist(MatchPlayer player) {
    JoinRequest request = JoinRequest.fromPlayer(player, null);
    return canAdd(player, request)
        && join.queryJoin(player, request).getOption() == JoinResultOption.FULL;
  }

  /** Tells the player their waitlist position, returns false if they aren't on the waitlist. */
  public boolean sendPosition(MatchPlayer player) {
    int index = indexOf(player);
    if (index < 0) return false;
    sendPosition(player, index);
    return true;
  }

  private void sendPosition(MatchPlayer player, int index) {
    Component leave = text("/leave", NamedTextColor.GOLD)
        .hoverEvent(showText(translatable("join.ok.waitlist.hover", NamedTextColor.GRAY)))
        .clickEvent(runCommand("/leave"));
    player.sendMessage(
        translatable("join.ok.waitlist", NamedTextColor.YELLOW, text(index + 1), leave));
  }

  void add(MatchPlayer joining, JoinRequest request, boolean kicked) {
    int index = indexOf(joining);
    boolean added = index < 0;
    if (added) {
      Entry entry = new Entry(joining.getId(), request, Instant.now(), kicked);
      index = indexFor(entry);
      waitlist.add(index, entry);
    }

    sendPosition(joining, index);
    // After the message, as listeners may remove the player
    if (added) match.callEvent(new PlayerWaitlistEvent(joining, request, waitlist.size()));
  }

  private int indexOf(MatchPlayer player) {
    for (int i = 0; i < waitlist.size(); i++) {
      if (waitlist.get(i).playerId().equals(player.getId())) return i;
    }
    return -1;
  }

  // Priority kicked players go first, then premium players, then whoever has waited longest
  private int indexFor(Entry entry) {
    int rank = rank(entry);
    int index = 0;
    for (; index < waitlist.size(); index++) {
      Entry other = waitlist.get(index);
      int otherRank = rank(other);
      if (rank != otherRank ? rank < otherRank : other.since().isAfter(entry.since())) break;
    }
    return index;
  }

  private int rank(Entry entry) {
    return entry.kicked() ? 0 : join.canJoinFull(entry.request()) ? 1 : 2;
  }

  void onPartyChange(PlayerPartyChangeEvent event) {
    if (waitlist.isEmpty()) return;
    if (!(event.getNewParty() instanceof ObserverParty)) remove(event.getPlayer());
    if (event.getOldParty() instanceof Competitor) scheduleDrain();
  }

  void onMatchStart() {
    BlitzMatchModule blitz = match.getModule(BlitzMatchModule.class);
    if (blitz == null) return;
    Component denied =
        translatable("blitz.joinDenied", text(Gamemode.BLITZ.getFullName(), NamedTextColor.AQUA));
    waitlist.removeIf(entry -> {
      MatchPlayer player = match.getPlayer(entry.playerId());
      if (player != null && blitz.canJoin(player, entry.request())) return false;
      if (player != null) player.sendWarning(denied);
      return true;
    });
  }

  void scheduleDrain() {
    if (drainScheduled || waitlist.isEmpty()) return;
    drainScheduled = true;
    match.getExecutor(MatchScope.LOADED).execute(this::drain);
  }

  private void drain() {
    drainScheduled = false;
    if (match.isFinished()) return;

    // Joining can modify the waitlist
    for (Entry entry : List.copyOf(waitlist)) {
      if (!waitlist.contains(entry)) continue;
      MatchPlayer player = match.getPlayer(entry.playerId());

      if (player == null) {
        waitlist.remove(entry);
        continue;
      }

      if (!player.isActive(PGM.get().getConfiguration().getWaitlistAfkTime())) {
        waitlist.remove(entry);
        player.sendWarning(translatable("join.err.waitlist.afk"));
        continue;
      }

      if (join.queryJoin(player, entry.request()).getOption() == JoinResultOption.FULL) continue;

      waitlist.remove(entry);
      join.join(player, entry.request());
      // They may have stopped watching while they waited
      if (player.getParty() instanceof Competitor) player.playSound(Sounds.MATCH_START);
    }
  }

  /** A player waiting for a slot in a full match, and whether they were priority kicked. */
  public record Entry(UUID playerId, JoinRequest request, Instant since, boolean kicked) {}
}
