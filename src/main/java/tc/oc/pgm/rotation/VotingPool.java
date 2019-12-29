package tc.oc.pgm.rotation;

import app.ashcon.intake.CommandException;
import java.util.*;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import org.bukkit.configuration.ConfigurationSection;
import tc.oc.component.Component;
import tc.oc.component.types.PersonalizedText;
import tc.oc.component.types.PersonalizedTranslatable;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.map.PGMMap;

public class VotingPool extends MapPool {

  // Number of maps in the vote, unless not enough maps in pool
  private static final int MAX_VOTE_OPTIONS = 5;

  private final double DEFAULT_WEIGHT;
  private final int VOTE_SIZE;
  private final Map<PGMMap, Double> mapScores = new HashMap<>();

  private Poll currentPoll;

  public VotingPool(MapPoolManager manager, ConfigurationSection section, String name) {
    super(manager, section, name);
    DEFAULT_WEIGHT = 1d / maps.size();
    VOTE_SIZE = Math.min(MAX_VOTE_OPTIONS, maps.size());

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
    currentPoll = new Poll();
    match.getPlayers().forEach(currentPoll::sendMessage);
  }

  /** Represents a polling process, with a set of options. */
  public class Poll {
    private static final String SYMBOL_IGNORE = "\u274c"; // ❌
    private static final String SYMBOL_VOTED = "\u2714"; // ✔

    private final Map<PGMMap, Set<UUID>> votes = new HashMap<>();

    Poll() {
      // FIXME: currently just picks best 5 maps, instead of randomly pick 5 based on score.
      mapScores.entrySet().stream()
          .sorted(Comparator.comparingDouble(Map.Entry::getValue))
          .map(Map.Entry::getKey)
          .limit(VOTE_SIZE)
          .forEach(map -> votes.put(map, new HashSet<>()));
    }

    public void sendMessage(MatchPlayer viewer) {
      for (PGMMap pgmMap : votes.keySet()) {
        viewer.sendMessage(getMapComponent(viewer, pgmMap));
      }
    }

    private Component getMapComponent(MatchPlayer viewer, PGMMap map) {
      boolean voted = votes.get(map).contains(viewer.getId());
      return new PersonalizedText(
              new PersonalizedText("["),
              new PersonalizedText(
                  voted ? SYMBOL_VOTED : SYMBOL_IGNORE,
                  voted ? ChatColor.GREEN : ChatColor.DARK_RED),
              new PersonalizedText(" " + votes.get(map).size(), ChatColor.YELLOW),
              new PersonalizedText("] "),
              new PersonalizedText(map.getInfo().getShortDescription(viewer.getBukkit()) + " ")
              // PGM doesn't have this info currently
              // new PersonalizedText(map.getInfo().getLocalizedGenre())
              )
          .hoverEvent(
              HoverEvent.Action.SHOW_TEXT,
              // FIXME: translate
              new PersonalizedTranslatable("Click to toggle vote").render(viewer.getBukkit()))
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
          .max(Comparator.comparingInt(e -> e.getValue().size()))
          .map(Map.Entry::getKey)
          .orElse(null);
    }

    /**
     * Picks a winner and ends the vote
     *
     * @return The picked map to play after the vote
     */
    PGMMap finishVote() {
      PGMMap picked = getMostVotedMap();
      if (picked == null) picked = getRandom();

      // Amount of players that voted, smaller or equal to amount of votes
      double voters = votes.values().stream().flatMap(Collection::stream).distinct().count();
      // Could turn the votes.size into premium-dependent vote count.
      votes.forEach((map, votes) -> mapScores.put(map, votes.size() / voters));
      mapScores.put(picked, 0d);
      return picked;
    }
  }
}
