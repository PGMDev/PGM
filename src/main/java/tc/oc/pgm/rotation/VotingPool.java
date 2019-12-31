package tc.oc.pgm.rotation;

import app.ashcon.intake.CommandException;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.stream.Collectors;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import tc.oc.component.Component;
import tc.oc.component.types.PersonalizedText;
import tc.oc.component.types.PersonalizedTranslatable;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.map.PGMMap;

public class VotingPool extends MapPool {

  // Number of maps in the vote, unless not enough maps in pool
  private static final int MAX_VOTE_OPTIONS = 5;
  // If maps were single voted, it would avg to this default
  private static final double DEFAULT_WEIGHT = 1d / MAX_VOTE_OPTIONS;

  // Amount of maps to display on vote
  private final int VOTE_SIZE;
  private final Map<PGMMap, Double> mapScores = new HashMap<>();

  private Poll currentPoll;

  public VotingPool(MapPoolManager manager, ConfigurationSection section, String name) {
    super(manager, section, name);
    VOTE_SIZE = Math.min(MAX_VOTE_OPTIONS, maps.size() - 1);

    for (PGMMap map : maps) {
      mapScores.put(map, DEFAULT_WEIGHT);
    }
  }

  public Poll getCurrentPoll() {
    return currentPoll;
  }

  /** Ticks scores for all maps, making them go slowly towards DEFAULT_WEIGHT. */
  private void tickScores() {
    mapScores.replaceAll((mapScores, value) -> (value * 3 + DEFAULT_WEIGHT) / 4);
  }

  @Override
  public PGMMap popNextMap() {
    if (currentPoll == null) return getRandom();

    PGMMap map = currentPoll.finishVote();
    currentPoll = null;
    tickScores();
    mapScores.put(map, 0d);
    return map;
  }

  @Override
  public PGMMap getNextMap() {
    return currentPoll == null ? null : currentPoll.getMostVotedMap();
  }

  @Override
  public void setNextMap(PGMMap map) {
    currentPoll = null;
  }

  @Override
  public void matchEnded(Match match) {
    if (manager.getOverriderMap() != null) return;
    currentPoll = new Poll(match);
    match.getPlayers().forEach(p -> currentPoll.sendMessage(p, false));
  }

  /** Represents a polling process, with a set of options. */
  public class Poll {
    private static final String SYMBOL_IGNORE = "\u274c"; // ❌
    private static final String SYMBOL_VOTED = "\u2714"; // ✔

    private final WeakReference<Match> match;
    private final Map<PGMMap, Set<UUID>> votes = new HashMap<>();

    Poll(Match match) {
      this.match = new WeakReference<>(match);
      // Sorting beforehand, saves future key remaps, as bigger values are placed at the end
      List<PGMMap> sortedDist =
          mapScores.entrySet().stream()
              .sorted(Comparator.comparingDouble(Map.Entry::getValue))
              .map(Map.Entry::getKey)
              .collect(Collectors.toList());

      NavigableMap<Double, PGMMap> cumulativeScores = new TreeMap<>();
      double maxWeight = cummulativeMap(0, sortedDist, cumulativeScores);

      for (int i = 0; i < VOTE_SIZE; i++) {
        NavigableMap<Double, PGMMap> subMap =
            cumulativeScores.tailMap(Math.random() * maxWeight, true);
        Map.Entry<Double, PGMMap> selected = subMap.pollFirstEntry();
        // Add map to votes
        votes.put(selected.getValue(), new HashSet<>());
        // No need to do replace logic after maps have been selected
        if (votes.size() >= VOTE_SIZE) break;

        // Remove map from pool, updating cumulative scores
        double selectedWeight = mapScores.get(selected.getValue());
        maxWeight -= selectedWeight;

        NavigableMap<Double, PGMMap> temp = new TreeMap<>();
        cummulativeMap(selected.getKey() - selectedWeight, subMap.values(), temp);

        subMap.clear();
        cumulativeScores.putAll(temp);
      }
    }

    private double cummulativeMap(
        double currWeight, Collection<PGMMap> maps, Map<Double, PGMMap> result) {
      for (PGMMap map : maps) {
        double score = mapScores.get(map);
        if (score > 0) result.put(currWeight += mapScores.get(map), map);
      }
      return currWeight;
    }

    public void sendMessage(MatchPlayer viewer, boolean showVotes) {
      for (PGMMap pgmMap : votes.keySet()) {
        viewer.sendMessage(getMapComponent(viewer, pgmMap, showVotes));
      }
    }

    private Component getMapComponent(MatchPlayer viewer, PGMMap map, boolean showVotes) {
      boolean voted = votes.get(map).contains(viewer.getId());
      return new PersonalizedText(
              new PersonalizedText("["),
              new PersonalizedText(
                  voted ? SYMBOL_VOTED : SYMBOL_IGNORE,
                  voted ? ChatColor.GREEN : ChatColor.DARK_RED),
              new PersonalizedText(
                  showVotes ? " " + countVotes(votes.get(map)) : "", ChatColor.YELLOW),
              new PersonalizedText("] "),
              new PersonalizedText(map.getInfo().getShortDescription(viewer.getBukkit()) + " ")
              // PGM isn't reading this from xml currently
              // new PersonalizedText(map.getInfo().getLocalizedGenre())
              )
          .hoverEvent(
              HoverEvent.Action.SHOW_TEXT,
              new PersonalizedTranslatable("command.pool.vote.hover").render(viewer.getBukkit()))
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
    public boolean toggleVote(PGMMap vote, UUID player) throws CommandException {
      Set<UUID> votes = this.votes.get(vote);
      if (votes == null)
        throw new CommandException(vote.getName() + " is not an option in the poll");

      if (votes.add(player)) return true;
      votes.remove(player);
      return false;
    }

    /** @return The map currently winning the vote, null if no vote is running. */
    private PGMMap getMostVotedMap() {
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
     * Picks a winner and ends the vote
     *
     * @return The picked map to play after the vote
     */
    PGMMap finishVote() {
      PGMMap picked = getMostVotedMap();
      if (picked == null) picked = getRandom();
      Match match = this.match.get();
      if (match != null) {
        match.getPlayers().forEach(p -> currentPoll.sendMessage(p, true));
      }

      // Amount of players that voted, smaller or equal to amount of votes
      double voters = votes.values().stream().flatMap(Collection::stream).distinct().count();
      if (voters > 0) votes.forEach((map, votes) -> mapScores.put(map, votes.size() / voters));
      mapScores.put(picked, 0d);
      return picked;
    }
  }
}
