package tc.oc.pgm.join;

import javax.annotation.Nullable;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.player.MatchPlayer;

/**
 * Something that is able to join the player to the match. {@link JoinGuard}s that are NOT {@link
 * JoinHandler}s are always queried first.
 */
public interface JoinHandler extends JoinGuard {

  /**
   * Join the given player to the match if at all possible. This is only called by staff commands.
   *
   * @param joining The joining player
   * @param forcedParty The party the player must be added to, or null if it doesn't matter what
   *     party they join. If the handler receives a party that it doesn't know what to do with, it
   *     should do nothing and return false.
   * @return True if the player was joined, or was already joined
   */
  boolean forceJoin(MatchPlayer joining, @Nullable Competitor forcedParty);

  /**
   * Try to join all of the given players simultaneously. This is called with all queued players
   * when the match starts. This method will be called on all handlers, breaking if the queue
   * becomes empty. Any players left in the queue will be joined through {@link #join}, and finally
   * sent to obs if that fails.
   */
  void queuedJoin(QueuedParticipants queue);
}
