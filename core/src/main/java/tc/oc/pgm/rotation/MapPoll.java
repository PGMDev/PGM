package tc.oc.pgm.rotation;

import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.newline;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;
import static net.kyori.adventure.text.event.ClickEvent.runCommand;
import static net.kyori.adventure.text.event.HoverEvent.showText;
import static net.kyori.adventure.title.Title.title;
import static tc.oc.pgm.util.TimeUtils.fromTicks;
import static tc.oc.pgm.util.text.TextTranslations.translate;

import app.ashcon.intake.CommandException;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
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
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.map.MapTag;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.named.MapNameStyle;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.setting.SettingKey;
import tc.oc.pgm.api.setting.SettingValue;
import tc.oc.pgm.util.inventory.tag.ItemTag;
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

  // Workaround: ItemStacks cant have invisible metadata, so these are used as a replacement.
  static final String VOTE_BOOK_METADATA = "vote_book";
  static final ItemTag<String> VOTE_BOOK_TAG = ItemTag.newString(VOTE_BOOK_METADATA);

  private final WeakReference<Match> match;
  private final Map<MapInfo, Double> mapScores;
  private final Set<MapInfo> overrides;
  private final int voteSize;

  private final Map<MapInfo, Set<UUID>> votes = new HashMap<>();

  MapPoll(Match match, Map<MapInfo, Double> mapScores, Set<MapInfo> overrides, int voteSize) {
    this.match = new WeakReference<>(match);
    this.mapScores = mapScores;
    this.overrides = overrides;
    this.voteSize = voteSize;

    selectMaps();
  }

  private void selectMaps() {
    // Sorting beforehand, saves future key remaps, as bigger values are placed at the end
    List<MapInfo> sortedDist =
        mapScores.entrySet().stream()
            .sorted(Comparator.comparingDouble(Map.Entry::getValue))
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());

    NavigableMap<Double, MapInfo> cumulativeScores = new TreeMap<>();
    double maxWeight = cummulativeMap(0, sortedDist, cumulativeScores);

    // Add all override maps before selecting random
    overrides.forEach(map -> votes.put(map, new HashSet<>()));

    for (int i = overrides.size(); i < voteSize; i++) {
      NavigableMap<Double, MapInfo> subMap =
          cumulativeScores.tailMap(Math.random() * maxWeight, true);
      Map.Entry<Double, MapInfo> selected = subMap.pollFirstEntry();

      if (selected == null) break; // No more maps to poll
      votes.put(selected.getValue(), new HashSet<>()); // Add map to votes
      if (votes.size() >= voteSize) break; // Skip replace logic after all maps have been selected

      // Remove map from pool, updating cumulative scores
      double selectedWeight = getWeight(selected.getValue());
      maxWeight -= selectedWeight;

      NavigableMap<Double, MapInfo> temp = new TreeMap<>();
      cummulativeMap(selected.getKey() - selectedWeight, subMap.values(), temp);

      subMap.clear();
      cumulativeScores.putAll(temp);
    }
  }

  private double getWeight(MapInfo map) {
    return getWeight(mapScores.get(map));
  }

  public static double getWeight(Double score) {
    if (score == null || score <= 0) return 0;
    return Math.max(Math.pow(score, 2), Double.MIN_VALUE);
  }

  private double cummulativeMap(
      double currWeight, Collection<MapInfo> maps, Map<Double, MapInfo> result) {
    for (MapInfo map : maps) {
      double score = getWeight(map);
      if (score > 0) result.put(currWeight += score, map);
    }
    return currWeight;
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
            Title.Times.of(fromTicks(5), fromTicks(60), fromTicks(5))));
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

  public void sendBook(MatchPlayer viewer, boolean forceOpen) {
    if (viewer.isLegacy()) {
      // Must use separate sendMessages, since 1.7 clients do not like the newline character
      viewer.sendMessage(VOTE_HEADER);
      for (MapInfo pgmMap : votes.keySet()) viewer.sendMessage(getMapBookComponent(viewer, pgmMap));
      return;
    }

    TextComponent.Builder content = text();
    content.append(VOTE_HEADER);
    content.append(newline());

    for (MapInfo pgmMap : votes.keySet())
      content.append(newline()).append(getMapBookComponent(viewer, pgmMap));

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

  private Component getMapBookComponent(MatchPlayer viewer, MapInfo map) {
    boolean voted = votes.get(map).contains(viewer.getId());

    TextComponent.Builder text = text();
    text.append(
        text(
            voted ? SYMBOL_VOTED : SYMBOL_IGNORE,
            voted ? NamedTextColor.DARK_GREEN : NamedTextColor.DARK_RED));
    text.append(text(" ").decoration(TextDecoration.BOLD, !voted));
    text.append(text(map.getName(), NamedTextColor.GOLD, TextDecoration.BOLD));
    text.hoverEvent(
        showText(
            text(
                map.getTags().stream().map(MapTag::toString).collect(Collectors.joining(" ")),
                NamedTextColor.YELLOW)));
    text.clickEvent(runCommand("/votenext -o " + map.getName())); // Fix 1px symbol diff
    return text.build();
  }

  /**
   * Toggle the vote of a user for a certain map. Player is allowed to vote for several maps.
   *
   * @param vote The map to vote for/against
   * @param player The player voting
   * @return true if the player is now voting for the map, false otherwise
   * @throws CommandException If the map is not an option in the poll
   */
  public boolean toggleVote(MapInfo vote, UUID player) throws CommandException {
    Set<UUID> votes = this.votes.get(vote);
    if (votes == null) throw new CommandException(vote.getName() + " is not an option in the poll");

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
  private int countVotes(Set<UUID> uuids) {
    return uuids.stream()
        .map(Bukkit::getPlayer)
        // Count disconnected players as 1, can't test for their perms
        .mapToInt(p -> p == null || !p.hasPermission(Permissions.EXTRA_VOTE) ? 1 : 2)
        .sum();
  }

  /**
   * Picks a winner and ends the vote, updating map scores based on votes
   *
   * @return The picked map to play after the vote
   */
  MapInfo finishVote() {
    MapInfo picked = getMostVotedMap();
    Match match = this.match.get();
    if (match != null) {
      match.getPlayers().forEach(player -> announceWinner(player, picked));
    }

    updateScores();
    return picked;
  }

  private void updateScores() {
    double voters = votes.values().stream().flatMap(Collection::stream).distinct().count();
    if (voters == 0) return;
    votes.forEach((m, v) -> mapScores.put(m, Math.max(v.size() / voters, Double.MIN_VALUE)));
  }
}
