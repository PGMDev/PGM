package tc.oc.pgm.api.match;

import tc.oc.pgm.api.player.MatchPlayer;

/** Represents the state that {@link MatchModule}s can listen and respond to events. */
public enum MatchScope {

  /**
   * The {@link Match} and its {@link org.bukkit.World} are loaded and all {@link MatchPlayer}s are
   * observing, but can request to join.
   */
  LOADED,

  /**
   * The {@link Match} is actively running, but not finished. {@link MatchPlayer}s might be
   * participating or still observing.
   */
  RUNNING
}
