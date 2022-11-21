package tc.oc.pgm.rotation.vote;

import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.newline;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;
import static net.kyori.adventure.text.event.ClickEvent.runCommand;
import static net.kyori.adventure.text.event.HoverEvent.showText;
import static net.kyori.adventure.title.Title.title;
import static tc.oc.pgm.util.TimeUtils.fromTicks;
import static tc.oc.pgm.util.text.PlayerComponent.player;
import static tc.oc.pgm.util.text.TextException.exception;
import static tc.oc.pgm.util.text.TextTranslations.translate;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.event.PlayerVoteEvent;
import tc.oc.pgm.api.integration.Integration;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.map.MapTag;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.setting.SettingKey;
import tc.oc.pgm.api.setting.SettingValue;
import tc.oc.pgm.events.MapVoteWinnerEvent;
import tc.oc.pgm.util.inventory.tag.ItemTag;
import tc.oc.pgm.util.named.MapNameStyle;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.text.TextException;
import tc.oc.pgm.util.text.TextTranslations;

/** Represents a polling process, with a set of options. */
public class MapPoll {
  private static final String SYMBOL_IGNORE = "\u2715"; // ✕
  private static final String SYMBOL_VOTED = "\u2714"; // ✔

  private static final int TITLE_LENGTH_CUTOFF = 15;

  private static final Component VOTE_BOOK_AUTHOR = text("PGM");
  private static final Component VOTE_HEADER =
      translatable("vote.header.map", NamedTextColor.DARK_PURPLE);
  private static final Component VOTE_BOOK_TITLE =
      translatable("vote.title.map", NamedTextColor.GOLD, TextDecoration.BOLD);

  // Workaround: ItemStacks can't have invisible metadata, so these are used as a replacement.
  static final String VOTE_BOOK_METADATA = "vote_book";
  static final ItemTag<String> VOTE_BOOK_TAG = ItemTag.newString(VOTE_BOOK_METADATA);

  private final WeakReference<Match> match;

  private final Map<MapInfo, Set<UUID>> votes;
  private final VotePoolOptions options;
  private boolean running = true;

  public MapPoll(Match match, List<MapInfo> maps, VotePoolOptions voteOptions) {
    this.match = new WeakReference<>(match);
    this.votes = new HashMap<>();
    this.options = voteOptions;
    maps.forEach(m -> votes.put(m, new HashSet<>()));

    match.addListener(new VotingBookListener(this, match), MatchScope.LOADED);
    match.getPlayers().forEach(viewer -> sendBook(viewer, false));
  }

  public void announceWinner(MatchPlayer viewer, MapInfo winner) {
    for (MapInfo pgmMap : votes.keySet())
      viewer.sendMessage(getMapChatComponent(viewer, pgmMap, pgmMap.equals(winner)));

    // Check if the winning map name's length suitable for the top title, otherwise subtitle
    boolean top = winner.getName().length() < TITLE_LENGTH_CUTOFF;
    Component mapName =
        winner.getStyledName(MapNameStyle.COLOR).decoration(TextDecoration.BOLD, true);

    viewer.showTitle(
        title(
            top ? mapName : empty(),
            top ? empty() : mapName,
            Title.Times.times(fromTicks(5), fromTicks(60), fromTicks(5))));
  }

  private Component getMapChatComponent(MatchPlayer viewer, MapInfo map, boolean winner) {
    boolean voted = votes.get(map).contains(viewer.getId());

    return text()
        .append(text("["))
        .append(
            text(
                voted ? SYMBOL_VOTED : SYMBOL_IGNORE,
                voted ? NamedTextColor.GREEN : NamedTextColor.DARK_RED))
        .append(text(" ").decoration(TextDecoration.BOLD, !voted)) // Fix 1px symbol diff
        .append(text("" + countVotes(votes.get(map)), NamedTextColor.YELLOW))
        .append(text("] "))
        .append(
            map.getStyledName(
                winner ? MapNameStyle.HIGHLIGHT_WITH_AUTHORS : MapNameStyle.COLOR_WITH_AUTHORS))
        .build();
  }

  private Component getOverridePlayer(MapInfo map) {
    UUID playerId = options.getOverrideMaps().get(map);
    Component playerName = null;
    Player bukkit = Bukkit.getPlayer(playerId);

    if (playerId != null && bukkit != null && !Integration.isVanished(bukkit)) {
      playerName = player(playerId, NameStyle.FANCY);
    }

    return playerName;
  }

  public void sendBook(MatchPlayer viewer, boolean forceOpen) {
    if (viewer.isLegacy()) {
      // Must use separate sendMessages, since 1.7 clients do not like the newline character
      viewer.sendMessage(VOTE_HEADER);
      for (MapInfo pgmMap : votes.keySet())
        viewer.sendMessage(getMapBookComponent(viewer, pgmMap, getOverridePlayer(pgmMap)));
      return;
    }

    TextComponent.Builder content = text();
    content.append(VOTE_HEADER);
    content.append(newline());

    for (MapInfo pgmMap : votes.keySet())
      content
          .append(newline())
          .append(getMapBookComponent(viewer, pgmMap, getOverridePlayer(pgmMap)));

    Book book = Book.builder().author(VOTE_BOOK_AUTHOR).pages(content.build()).build();

    ItemStack held = viewer.getInventory().getItemInHand();
    if (held.getType() != Material.ENCHANTED_BOOK) {
      viewer.getInventory().setHeldItemSlot(2);
    }

    viewer.getInventory().setItemInHand(createVoteBookItem(viewer));

    if (forceOpen || viewer.getSettings().getValue(SettingKey.VOTE) == SettingValue.VOTE_ON)
      viewer.openBook(book);
  }

  private ItemStack createVoteBookItem(MatchPlayer viewer) {
    Locale locale = TextTranslations.getLocale(viewer.getBukkit());

    ItemStack personalDummyVoteBook = new ItemStack(Material.ENCHANTED_BOOK);
    VOTE_BOOK_TAG.set(personalDummyVoteBook, VOTE_BOOK_METADATA);
    ItemMeta meta = personalDummyVoteBook.getItemMeta();

    Component dummyTitle = translate(VOTE_BOOK_TITLE, locale);
    meta.setDisplayName(TextTranslations.translateLegacy(dummyTitle, viewer.getBukkit()));

    personalDummyVoteBook.setItemMeta(meta);

    return personalDummyVoteBook;
  }

  private Component getMapBookComponent(
      MatchPlayer viewer, MapInfo map, @Nullable Component identified) {
    boolean voted = votes.get(map).contains(viewer.getId());

    TextComponent.Builder text = text();
    TextComponent.Builder hover = text();

    hover.append(
        text(
            map.getTags().stream().map(MapTag::toString).collect(Collectors.joining(" ")),
            NamedTextColor.YELLOW));

    if (identified != null) {
      hover
          .append(
              text()
                  .append(newline())
                  .append(text("+ ", NamedTextColor.GREEN, TextDecoration.BOLD))
                  .append(text("Sponsored by ", NamedTextColor.GRAY)))
          .append(identified);
    } // TODO: ^ hard coded text value, in the future make translatable or passed in so custom
    // messages can be set

    text.append(
        text(
            voted ? SYMBOL_VOTED : SYMBOL_IGNORE,
            voted ? NamedTextColor.DARK_GREEN : NamedTextColor.DARK_RED));
    text.append(text(" ").decoration(TextDecoration.BOLD, !voted));
    text.append(text(map.getName(), NamedTextColor.GOLD, TextDecoration.BOLD));
    text.hoverEvent(showText(hover.build()));
    text.clickEvent(runCommand("/votenext -o " + map.getName())); // Fix 1px symbol diff
    return text.build();
  }

  /**
   * Toggle the vote of a user for a certain map. Player is allowed to vote for several maps.
   *
   * @param vote The map to vote for/against
   * @param player The player voting
   * @return true if the player is now voting for the map, false otherwise
   * @throws tc.oc.pgm.util.text.TextException If the map is not an option in the poll
   */
  public boolean toggleVote(MapInfo vote, UUID player) throws TextException {
    Set<UUID> votes = this.votes.get(vote);
    if (votes == null) throw exception("map.notFound");

    if (match.get() != null) {
      match.get().callEvent(new PlayerVoteEvent(player, vote, !votes.contains(player)));
    }

    if (votes.add(player)) return true;
    votes.remove(player);
    return false;
  }

  /** @return The map currently winning the vote, null if no vote is running. */
  private MapInfo getMostVotedMap() {
    return votes.entrySet().stream()
        .max(Comparator.comparingInt(e -> countVotes(e.getValue())))
        .map(Map.Entry::getKey)
        .orElse(null);
  }

  /**
   * Count the amount of votes for a set of uuids. Players with the pgm.premium permission get
   * double votes.
   *
   * @param uuids The players who voted
   * @return The number of votes counted
   */
  private int countVotes(Collection<UUID> uuids) {
    return uuids.stream()
        .map(Bukkit::getPlayer)
        // Count disconnected players as 1, can't test for their perms
        .mapToInt(this::calcVoteMultiplier)
        .sum();
  }

  private int calcVoteMultiplier(Player player) {
    // Count disconnected players as 1, can't test for their perms
    if (player != null) {
      for (int i = 5; i > 1; i--) {
        if (player.hasPermission(Permissions.VOTE_MULTIPLIER + "." + i)) {
          return i;
        }
      }
      // Legacy extra vote permission node support
      return player.hasPermission(Permissions.EXTRA_VOTE) ? 2 : 1;
    }
    return 1;
  }

  public boolean isRunning() {
    return running;
  }

  /**
   * Picks a winner and ends the vote, updating map scores based on votes
   *
   * @return The picked map to play after the vote
   */
  public MapInfo finishVote() {
    running = false;
    MapInfo picked = getMostVotedMap();
    Match match = this.match.get();

    if (match != null) match.getPlayers().forEach(player -> announceWinner(player, picked));

    if (picked != null) match.callEvent(new MapVoteWinnerEvent(picked));

    return picked;
  }

  public void cancel() {
    running = false;
  }

  public Map<MapInfo, Set<UUID>> getVotes() {
    return votes;
  }

  public boolean isCustom() {
    return !options.getCustomVoteMaps().isEmpty();
  }
}
