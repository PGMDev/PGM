package tc.oc.pgm.rotation;

import app.ashcon.intake.CommandException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import tc.oc.component.Component;
import tc.oc.component.types.PersonalizedText;
import tc.oc.component.types.PersonalizedTranslatable;
import tc.oc.pgm.AllTranslations;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.world.NMSHacks;

/** Represents a polling process, with a set of options. */
public class MapPoll {
  private static final String SYMBOL_IGNORE = "\u2715"; // ✕
  private static final String SYMBOL_VOTED = "\u2714"; // ✔

  private final WeakReference<Match> match;
  private final Map<MapInfo, Double> mapScores;
  private final int voteSize;

  private final Map<MapInfo, Set<UUID>> votes = new HashMap<>();

  MapPoll(Match match, Map<MapInfo, Double> mapScores, int voteSize) {
    this.match = new WeakReference<>(match);
    this.mapScores = mapScores;
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

    for (int i = 0; i < voteSize; i++) {
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

  public void sendMessage(MatchPlayer viewer) {
    for (MapInfo pgmMap : votes.keySet()) viewer.sendMessage(getMapChatComponent(viewer, pgmMap));
  }

  private Component getMapChatComponent(MatchPlayer viewer, MapInfo map) {
    boolean voted = votes.get(map).contains(viewer.getId());
    return new PersonalizedText(
        new PersonalizedText("["),
        new PersonalizedText(
            voted ? SYMBOL_VOTED : SYMBOL_IGNORE, voted ? ChatColor.GREEN : ChatColor.DARK_RED),
        new PersonalizedText(" ").bold(!voted), // Fix 1px symbol diff
        new PersonalizedText("" + countVotes(votes.get(map)), ChatColor.YELLOW),
        new PersonalizedText("] "), // FIXME: map fancy name, consider implementing Named
        new PersonalizedText(map.getName() + " ", ChatColor.GOLD));
  }

  public void sendBook(MatchPlayer viewer) {
    String title = ChatColor.GOLD + "" + ChatColor.BOLD;
    title += AllTranslations.get().translate("command.pool.vote.book.title", viewer.getBukkit());

    ItemStack is = new ItemStack(Material.WRITTEN_BOOK);
    BookMeta meta = (BookMeta) is.getItemMeta();
    meta.setAuthor("PGM");
    meta.setTitle(title);

    List<Component> content = new ArrayList<>(votes.size() + 2);
    content.add(
        new PersonalizedText(
            new PersonalizedTranslatable("command.pool.vote.book.header"), ChatColor.DARK_PURPLE));
    content.add(new PersonalizedText("\n\n"));

    for (MapInfo pgmMap : votes.keySet()) content.add(getMapBookComponent(viewer, pgmMap));

    NMSHacks.setBookPages(meta, new PersonalizedText(content).render(viewer.getBukkit()));
    is.setItemMeta(meta);

    ItemStack held = viewer.getInventory().getItemInHand();
    if (held.getType() != Material.WRITTEN_BOOK
        || !title.equals(((BookMeta) is.getItemMeta()).getTitle())) {
      viewer.getInventory().setHeldItemSlot(2);
    }
    viewer.getInventory().setItemInHand(is);
    NMSHacks.openBook(is, viewer.getBukkit());
  }

  private Component getMapBookComponent(MatchPlayer viewer, MapInfo map) {
    boolean voted = votes.get(map).contains(viewer.getId());
    return new PersonalizedText(
            new PersonalizedText(
                voted ? SYMBOL_VOTED : SYMBOL_IGNORE,
                voted ? ChatColor.DARK_GREEN : ChatColor.DARK_RED),
            new PersonalizedText(" ").bold(!voted), // Fix 1px symbol diff
            new PersonalizedText(map.getName() + "\n", ChatColor.BOLD, ChatColor.GOLD))
        .hoverEvent(
            HoverEvent.Action.SHOW_TEXT,
            new PersonalizedText( // FIXME: map tags
                    new LinkedList<String>().stream().collect(Collectors.joining(" ")),
                    ChatColor.YELLOW)
                .render())
        .clickEvent(ClickEvent.Action.RUN_COMMAND, "/votenext " + map.getName());
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
        .mapToInt(p -> p == null || !p.hasPermission(Permissions.PREMIUM) ? 1 : 2)
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
      match.getPlayers().forEach(this::sendMessage);
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
