package tc.oc.pgm.api.match.factory;

import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;
import tc.oc.pgm.api.map.MapContext;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.player.MatchPlayer;

/** A thread-safe factory for creating {@link Match}es from {@link MapContext}s. */
public interface MatchFactory {

  /**
   * Begins the creation of a {@link Match}, without actually loading any {@link MatchModule}s.
   *
   * @param map The {@link MapContext} to load.
   */
  CompletableFuture<Match> initMatch(MapContext map);

  /**
   * Finishes the creation of a {@link Match}, loading the {@link MatchModule}s and teleporting the
   * given {@link MatchPlayer}s to its world.
   *
   * @param oldMatch A {@link Match} to teleport players from or {@code null} for none.
   * @param match A {@link Match} that was pre-loaded with {@link #initMatch(MapContext)}.
   */
  void moveMatch(@Nullable Match oldMatch, Match match);
}
