package tc.oc.pgm.rotation.vote;

import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.newline;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;
import static net.kyori.adventure.title.Title.title;
import static tc.oc.pgm.util.Assert.assertNotNull;
import static tc.oc.pgm.util.TimeUtils.fromTicks;
import static tc.oc.pgm.util.text.TextException.exception;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Random;
import java.util.SequencedMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.stream.Collectors;
import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.match.event.MatchVoteFinishEvent;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.setting.SettingKey;
import tc.oc.pgm.api.setting.SettingValue;
import tc.oc.pgm.rotation.pools.VotingPool;
import tc.oc.pgm.rotation.vote.book.PollIntegration;
import tc.oc.pgm.rotation.vote.book.VotingBookCreator;
import tc.oc.pgm.rotation.vote.events.MatchPlayerVoteEvent;
import tc.oc.pgm.util.inventory.tag.ItemTag;
import tc.oc.pgm.util.named.MapNameStyle;
import tc.oc.pgm.util.text.TextException;
import tc.oc.pgm.util.text.TextTranslations;

/** Represents a polling process, with a set of options. */
public class MapPoll {
  private static final String SYMBOL_IGNORE = "\u2715"; // ✕
  private static final String SYMBOL_VOTED = "\u2714"; // ✔
  private static final int TITLE_LENGTH_CUTOFF = 15;
  private static final int VOTE_DIVISOR = 60; // Perfectly divisible by 1..5
  private static final Random RANDOM = new Random();

  private static final Component VOTE_BOOK_AUTHOR = text("PGM");
  private static final Component VOTE_HEADER =
      translatable("vote.header.map", NamedTextColor.DARK_PURPLE);
  private static final Component VOTE_BOOK_TITLE =
      translatable("vote.title.map", NamedTextColor.GOLD, TextDecoration.BOLD);

  // Workaround: ItemStacks can't have invisible metadata, so these are used as a replacement.
  static final ItemTag<Boolean> VOTE_BOOK_TAG = ItemTag.newBoolean("vote_book");

  private final WeakReference<Match> match;
  private final ElectoralSystem system;
  private final double minThreshold;
  private final double maxThreshold;
  private final SequencedMap<MapInfo, Set<UUID>> votes;

  private final AtomicReference<PollState> state = new AtomicReference<>(PollState.RUNNING);
  private Tally tally;
  private Set<MapInfo> candidates;
  private MapInfo winner;

  private enum PollState {
    RUNNING,
    ENDING,
    ENDED,
    CANCELLED
  }

  public MapPoll(Match match, VotingPool.VoteConstants constants, List<MapInfo> maps) {
    this.match = new WeakReference<>(match);
    this.system = constants.system();
    this.minThreshold = constants.minThreshold();
    this.maxThreshold = constants.maxThreshold();
    this.votes = new LinkedHashMap<>();
    maps.forEach(m -> votes.put(m, new HashSet<>()));

    match.addListener(new VotingBookListener(this, match), MatchScope.LOADED);
    match.getPlayers().forEach(viewer -> sendBook(viewer, false));
  }

  @Deprecated(forRemoval = true)
  public static void setVotingBookCreator(VotingBookCreator bookCreator) {
    PollIntegration.setVotingBookCreator(assertNotNull(bookCreator));
  }

  @Deprecated(forRemoval = true)
  public VotingBookCreator getVotingBookCreator() {
    return PollIntegration.getVotingBookCreator();
  }

  public void sendBook(MatchPlayer viewer, boolean forceOpen) {
    if (!isRunning()) throw exception("vote.noVote");

    if (viewer.isLegacy()) {
      // Must use separate sendMessages, since 1.7 clients do not like the newline character
      viewer.sendMessage(VOTE_HEADER);
      for (MapInfo pgmMap : votes.keySet()) {
        boolean voted = votes.get(pgmMap).contains(viewer.getId());
        viewer.sendMessage(PollIntegration.getMapBookComponent(viewer, pgmMap, voted));
      }
      return;
    }

    var inv = viewer.getInventory();
    if (!Boolean.TRUE.equals(VOTE_BOOK_TAG.get(inv.getItemInHand()))) {
      inv.setHeldItemSlot(2);
      inv.setItemInHand(createVoteBookItem(viewer));
    }

    if (!forceOpen && viewer.getSettings().getValue(SettingKey.VOTE) != SettingValue.VOTE_ON)
      return;

    TextComponent.Builder content = text();
    content.append(VOTE_HEADER);
    content.append(newline());

    for (MapInfo pgmMap : votes.keySet()) {
      boolean voted = votes.get(pgmMap).contains(viewer.getId());
      content.append(newline());
      content.append(PollIntegration.getMapBookComponent(viewer, pgmMap, voted));
    }

    Component footer = PollIntegration.getMapBookFooter(viewer);
    if (footer != null) {
      content.append(footer);
    }

    Book book = Book.builder().author(VOTE_BOOK_AUTHOR).pages(content.build()).build();
    viewer.openBook(book);
  }

  private ItemStack createVoteBookItem(MatchPlayer viewer) {
    ItemStack votebook = new ItemStack(Material.ENCHANTED_BOOK);
    VOTE_BOOK_TAG.set(votebook, true);
    ItemMeta meta = votebook.getItemMeta();
    meta.setDisplayName(TextTranslations.translateLegacy(VOTE_BOOK_TITLE, viewer));
    votebook.setItemMeta(meta);
    return votebook;
  }

  /**
   * Toggle the vote of a user for a certain map. Player is allowed to vote for several maps.
   *
   * @param vote The map to vote for/against
   * @param player The {@link MatchPlayer} voting
   * @return true if the player is now voting for the map, false otherwise
   * @throws tc.oc.pgm.util.text.TextException If the map is not an option or poll isn't running
   */
  public boolean toggleVote(MapInfo vote, MatchPlayer player) throws TextException {
    if (!isRunning()) throw exception("vote.noVote");

    Set<UUID> votes = this.votes.get(vote);
    if (votes == null) throw exception("map.notFound");

    boolean added = votes.add(player.getId());
    if (!added) votes.remove(player.getId());

    player.getMatch().callEvent(new MatchPlayerVoteEvent(player, vote, added));

    return added;
  }

  public boolean isRunning() {
    return state.get() == PollState.RUNNING;
  }

  public void cancel() {
    // Cannot cancel a non-running poll, but avoid throwing as this may require being called as a
    // last resort.
    if (!state.compareAndSet(PollState.RUNNING, PollState.CANCELLED)) {
      PGM.get().getLogger().log(Level.WARNING, "Tried to cancel an already ended poll: " + this);
      return;
    }
    Match match = this.match.get();
    if (match != null) match.callEvent(new MatchVoteFinishEvent(match, null));
  }

  /**
   * Picks a winner and ends the vote, updating map scores based on votes
   *
   * @return The picked map to play after the vote
   */
  public MapInfo finishVote() {
    this.tally = tallyVotes();
    this.winner = getVoteWinner();
    Match match = this.match.get();
    if (match != null) {
      match.getPlayers().forEach(player -> announceWinner(player, winner));
      match.callEvent(new MatchVoteFinishEvent(match, winner));
    }
    return winner;
  }

  private Tally tallyVotes() {
    assertTransition(PollState.RUNNING, PollState.ENDING);

    // How many different options each player voted for
    var voteCountByPlayer = votes.values().stream()
        .flatMap(Collection::stream)
        .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

    var countByMap = votes.entrySet().stream()
        .collect(Collectors.toMap(
            Map.Entry::getKey,
            e -> countVotes(voteCountByPlayer, e.getValue()),
            Tally.Count::plus,
            LinkedHashMap::new));

    return new Tally(countByMap);
  }

  /** @return The map that wins the vote, throw if no maps exist. */
  private MapInfo getVoteWinner() {
    assertTransition(PollState.ENDING, PollState.ENDED);

    var mostVoted = tally.maps().entrySet().stream()
        .max(Comparator.comparingInt(e -> e.getValue().votes()))
        .map(Map.Entry::getKey)
        .orElseThrow();
    return switch (system) {
      case APPROVAL_VOTING -> mostVoted;
      case RANDOM_BALLOT -> {
        var total = tally.total();
        // Single-vote, special, or above max: win directly
        if (total.votes() <= 1
            || PollIntegration.isSpecialMap(mostVoted)
            || tally.maps().get(mostVoted).percent(total) > maxThreshold) {
          candidates = Set.of(mostVoted);
          yield mostVoted;
        }

        NavigableMap<Integer, MapInfo> cumulativeVotes = new TreeMap<>();
        int maxVotes = 0;
        for (var map : tally.maps().entrySet()) {
          var count = map.getValue();
          if (count.scaledVotes() == 0 || count.percent(total) < minThreshold) continue;
          cumulativeVotes.put(maxVotes += count.scaledVotes(), map.getKey());
        }
        this.candidates = Set.copyOf(cumulativeVotes.values());
        yield cumulativeVotes.higherEntry(RANDOM.nextInt(maxVotes)).getValue();
      }
    };
  }

  public void announceWinner(MatchPlayer viewer, MapInfo winner) {
    assertTransition(PollState.ENDED, PollState.ENDED);

    for (MapInfo pgmMap : votes.keySet())
      viewer.sendMessage(getMapChatComponent(viewer, pgmMap, pgmMap.equals(winner)));

    // Check if the winning map name's length suitable for the top title, otherwise subtitle
    boolean top = winner.getName().length() < TITLE_LENGTH_CUTOFF;
    Component mapName =
        winner.getStyledName(MapNameStyle.COLOR).decoration(TextDecoration.BOLD, true);

    viewer.showTitle(title(
        top ? mapName : empty(),
        top ? empty() : mapName,
        Title.Times.times(fromTicks(5), fromTicks(60), fromTicks(5))));
  }

  private Component getMapChatComponent(MatchPlayer viewer, MapInfo map, boolean winner) {
    assertNotNull(tally);

    boolean voted = votes.get(map).contains(viewer.getId());
    return text()
        .append(text("["))
        .append(text(
            voted ? SYMBOL_VOTED : SYMBOL_IGNORE,
            voted ? NamedTextColor.GREEN : NamedTextColor.DARK_RED))
        .append(text(" ").decoration(TextDecoration.BOLD, !voted)) // Fix 1px symbol diff
        .append(tallyDisplay(map))
        .append(text("] "))
        .append(map.getStyledName(
            winner ? MapNameStyle.HIGHLIGHT_WITH_AUTHORS : MapNameStyle.COLOR_WITH_AUTHORS))
        .build();
  }

  private Component tallyDisplay(MapInfo m) {
    var map = tally.maps().get(m);
    var total = tally.total();
    return switch (system) {
      case APPROVAL_VOTING -> text(map.votes() + "", NamedTextColor.YELLOW);
      case RANDOM_BALLOT -> {
        var text = text(Math.round(map.percent(total) * 100) + "%", NamedTextColor.YELLOW);
        if (candidates != null && !candidates.contains(m)) {
          var reason = map.percent(total) < minThreshold
              ? "min"
              : tally.maps.get(winner).percent(total) > maxThreshold ? "max" : "special";
          text = text.decoration(TextDecoration.STRIKETHROUGH, true)
              .hoverEvent(translatable(
                  "vote.excluded.base",
                  NamedTextColor.YELLOW,
                  m.getStyledName(MapNameStyle.COLOR),
                  translatable("vote.excluded." + reason)));
        }
        yield text;
      }
    };
  }

  private void assertTransition(PollState expected, PollState transition) {
    if (transition == null) transition = expected;
    if (!state.compareAndSet(expected, transition)) {
      throw new IllegalStateException(
          "Failed poll transition (" + expected + "->" + transition + "), poll: " + this);
    }
  }

  /**
   * Counts the number of votes for a set of player UUIDs. Players may have multiple votes by
   * integration; defaults to using permissions "pgm.vote.extra" and "pgm.vote.extra.#". ScaledVotes
   * are computed such that they split if a user votes for multiple maps.
   *
   * @param uuids The UUIDs of the players who voted.
   * @return The number of votes counted.
   * @see tc.oc.pgm.rotation.vote.book.PollExtensionsImpl#getVotingPower(UUID) for default voting
   *     power impl
   */
  private Tally.Count countVotes(Map<UUID, Long> castedVotes, Collection<UUID> uuids) {
    int totalVotes = 0;
    int scaledVotes = 0;
    for (UUID uuid : uuids) {
      int vote = PollIntegration.getVotingPower(uuid);
      totalVotes += vote;
      scaledVotes += (int) (vote * VOTE_DIVISOR / castedVotes.get(uuid));
    }
    return new Tally.Count(uuids, totalVotes, scaledVotes);
  }

  /** @deprecated Prefer {@link #getTally} to query this information instead. */
  @Deprecated
  public Map<MapInfo, Set<UUID>> getVotes() {
    return Collections.unmodifiableMap(votes);
  }

  public Tally getTally() {
    return tally;
  }

  public record Tally(SequencedMap<MapInfo, Count> maps, Count total) {
    public Tally(SequencedMap<MapInfo, Count> maps) {
      this(
          Collections.unmodifiableSequencedMap(maps),
          maps.values().stream().reduce(new Tally.Count(Set.of(), 0, 0), Tally.Count::plus));
    }

    public record Count(Set<UUID> electors, int votes, int scaledVotes) {
      public Count(Collection<UUID> electors, int votes, int scaledVotes) {
        this(Set.copyOf(electors), votes, scaledVotes);
      }

      public int voters() {
        return electors.size();
      }

      public double percent(Count total) {
        if (total.scaledVotes == 0) return 0d; // Avoid divisions by 0
        return scaledVotes / (double) total.scaledVotes;
      }

      public Count plus(Count other) {
        var union = new ArrayList<UUID>(this.voters() + other.voters());
        union.addAll(this.electors());
        union.addAll(other.electors());
        return new Count(union, this.votes + other.votes, this.scaledVotes + other.scaledVotes);
      }
    }
  }

  @Override
  public String toString() {
    var m = match.get();
    return "MapPoll{" + "match="
        + (m == null ? "null" : m.getId()) + ", system="
        + system + ", minThreshold="
        + minThreshold + ", maxThreshold="
        + maxThreshold + ", votes="
        + votes + ", state="
        + state.get() + ", tally="
        + tally + ", candidates="
        + candidates + ", winner="
        + winner + '}';
  }
}
