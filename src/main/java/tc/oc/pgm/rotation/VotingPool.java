package tc.oc.pgm.rotation;

import java.util.*;
import org.bukkit.configuration.ConfigurationSection;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.map.PGMMap;

@SuppressWarnings("unchecked")
public class VotingPool extends MapPool {

  // Number of maps in the vote, unless not enough maps in pool
  private static final int MAX_VOTE_OPTIONS = 5;

  private final double DEFAULT_WEIGHT;
  private final int VOTE_SIZE;
  private final Map<PGMMap, Double> mapScores = new HashMap<>();
  private final Map<PGMMap, Set<UUID>> votes = new HashMap<>();

  public VotingPool(MapPoolManager manager, ConfigurationSection section, String name) {
    super(manager, section, name);
    DEFAULT_WEIGHT = 1d / maps.size();
    VOTE_SIZE = Math.min(MAX_VOTE_OPTIONS, maps.size());

    for (PGMMap map : maps) {
      mapScores.put(map, DEFAULT_WEIGHT);
    }
  }

  /** Updates scores for all maps, with a weighted average towards DEFAULT_WEIGHT */
  private void updateScores() {
    mapScores.replaceAll((mapScores, value) -> (value * 3 + DEFAULT_WEIGHT) / 4);
  }

  /**
   * Register a vote from a player towards a map. Player is allowed to vote for several maps.
   *
   * @param votedMap The map to vote for/against
   * @param player The player voting
   * @param inFavor true if voting for the map, false if removing their vote
   * @return true if the vote was casted successfully, false otherwise
   */
  public boolean registerVote(PGMMap votedMap, UUID player, boolean inFavor) {
    Set<UUID> votes = this.votes.computeIfAbsent(votedMap, m -> new HashSet<>());

    if (inFavor) return votes.add(player);
    else return votes.remove(player);
  }

  /** @return The map currently winning the vote, null if no vote is running. */
  private PGMMap getMostVotedMap() {
    return votes.entrySet().stream()
        .max(Comparator.comparingInt(e -> e.getValue().size()))
        .map(Map.Entry::getKey)
        .orElse(null);
  }

  /**
   * Resets the voting process after a map has been picked
   *
   * @param picked The picked map after the vote
   */
  private void resetVote(PGMMap picked) {
    votes.clear();
    mapScores.put(picked, 0d);
  }

  @Override
  public PGMMap popNextMap() {
    PGMMap voted = getMostVotedMap();

    // If no vote has happened, pick a random map from the pool
    if (voted == null) {
      voted = getRandom();
    }

    updateScores();
    resetVote(voted);

    return voted;
  }

  @Override
  public PGMMap getNextMap() {
    return getMostVotedMap();
  }

  @Override
  public void matchEnded(Match match) {
    // TODO: Start a vote, pick VOTE_SIZE best maps
  }
}
