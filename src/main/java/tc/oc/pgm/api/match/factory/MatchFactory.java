package tc.oc.pgm.api.match.factory;

import tc.oc.pgm.api.map.MapContext;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.player.MatchPlayer;

import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;

/** A thread-safe factory for creating {@link Match}es from {@link MapContext}s. */
public interface MatchFactory {

  /**
   * Begins the creation of a {@link Match}, without actually loading any {@link MatchModule}s.
   *
   * @param map The {@link MapContext} to load.
   * @return A future {@link Match} or {@code null} if it failed.
   */
  CompletableFuture<Match> createPreMatch(MapContext map);

  /**
   * Finishes the creation of a {@link Match}, loading the {@link MatchModule}s and teleporting the
   * given {@link MatchPlayer}s to its world.
   *
   * @param match A {@link Match} that was pre-loaded with {@link #createPreMatch(MapContext)}.
   * @param players A collection of {@link MatchPlayer}s to teleport or {@code null} for none.
   */
  void createMatch(Match match, @Nullable Iterable<MatchPlayer> players);
}
