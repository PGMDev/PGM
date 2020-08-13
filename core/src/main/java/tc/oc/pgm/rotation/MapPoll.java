package tc.oc.pgm.rotation;

import app.ashcon.intake.CommandException;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.TranslatableComponent;
import net.kyori.text.event.ClickEvent;
import net.kyori.text.event.HoverEvent;
import net.kyori.text.format.TextColor;
import net.kyori.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.map.MapTag;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.setting.SettingKey;
import tc.oc.pgm.api.setting.SettingValue;
import tc.oc.pgm.util.named.MapNameStyle;
import tc.oc.pgm.util.nms.NMSHacks;
import tc.oc.pgm.util.text.TextTranslations;

/** Represents a polling process, with a set of options. */
public class MapPoll {
  private static final String SYMBOL_IGNORE = "\u2715"; // ✕
  private static final String SYMBOL_VOTED = "\u2714"; // ✔

  private static final int TITLE_LENGTH_CUTOFF = 15;

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
        top ? mapName : TextComponent.empty(), top ? TextComponent.empty() : mapName, 5, 60, 5);
  }

  private Component getMapChatComponent(MatchPlayer viewer, MapInfo map, boolean winner) {
    boolean voted = votes.get(map).contains(viewer.getId());

    return TextComponent.builder()
        .append("[")
        .append(voted ? SYMBOL_VOTED : SYMBOL_IGNORE, voted ? TextColor.GREEN : TextColor.DARK_RED)
        .append(
            TextComponent.of(" ").decoration(TextDecoration.BOLD, !voted)) // Fix 1px symbol diff
        .append("" + countVotes(votes.get(map)), TextColor.YELLOW)
        .append("] ")
        .append(
            map.getStyledName(
                winner ? MapNameStyle.HIGHLIGHT_WITH_AUTHORS : MapNameStyle.COLOR_WITH_AUTHORS))
        .build();
  }

  public void sendBook(MatchPlayer viewer, boolean forceOpen) {
    if (viewer.isLegacy()) {
      // Must use separate sendMessages, since 1.7 clients do not like the newline character
      viewer.sendMessage(TranslatableComponent.of("vote.header.map", TextColor.DARK_PURPLE));
      for (MapInfo pgmMap : votes.keySet()) viewer.sendMessage(getMapBookComponent(viewer, pgmMap));
      return;
    }

    TextComponent.Builder content = TextComponent.builder();
    content.append(TranslatableComponent.of("vote.header.map", TextColor.DARK_PURPLE));
    content.append(TextComponent.newline());

    for (MapInfo pgmMap : votes.keySet())
      content.append(TextComponent.newline()).append(getMapBookComponent(viewer, pgmMap));

    ItemStack is = new ItemStack(Material.WRITTEN_BOOK);
    BookMeta meta = (BookMeta) is.getItemMeta();
    meta.setAuthor("PGM");

    String title = ChatColor.GOLD + "" + ChatColor.BOLD;
    title += TextTranslations.translate("vote.title.map", viewer.getBukkit());
    meta.setTitle(title);

    NMSHacks.setBookPages(
        meta, TextTranslations.toBaseComponent(content.build(), viewer.getBukkit()));
    is.setItemMeta(meta);

    ItemStack held = viewer.getInventory().getItemInHand();
    if (held.getType() != Material.WRITTEN_BOOK
        || !title.equals(((BookMeta) is.getItemMeta()).getTitle())) {
      viewer.getInventory().setHeldItemSlot(2);
    }
    viewer.getInventory().setItemInHand(is);

    if (forceOpen || viewer.getSettings().getValue(SettingKey.VOTE) == SettingValue.VOTE_ON)
      NMSHacks.openBook(is, viewer.getBukkit());
  }

  private Component getMapBookComponent(MatchPlayer viewer, MapInfo map) {
    boolean voted = votes.get(map).contains(viewer.getId());

    return TextComponent.builder()
        .append(
            voted ? SYMBOL_VOTED : SYMBOL_IGNORE, voted ? TextColor.DARK_GREEN : TextColor.DARK_RED)
        .append(
            TextComponent.of(" ").decoration(TextDecoration.BOLD, !voted)) // Fix 1px symbol diff
        .append(map.getName(), TextColor.GOLD, TextDecoration.BOLD)
        .hoverEvent(
            HoverEvent.showText(
                TextComponent.of(
                    map.getTags().stream().map(MapTag::toString).collect(Collectors.joining(" ")),
                    TextColor.YELLOW)))
        .clickEvent(ClickEvent.runCommand("/votenext -o " + map.getName()))
        .build();
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
