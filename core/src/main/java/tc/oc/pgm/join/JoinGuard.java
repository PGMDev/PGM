package tc.oc.pgm.join;

import javax.annotation.Nullable;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.player.MatchPlayer;

/**
 * Something that monitors or filters the joining process, but cannot necessarily join the player.
 */
public interface JoinGuard {

  /**
   * Without side-effects, test what would happen if the given player tried to join the match right
   * now.
   *
   * @param joining The joining player
   * @param chosenParty A specific party the player wants to join, or null if they have no
   *     preference
   * @return Result of the join request. If the implementor does not know how to handle the query,
   *     it can return null to delegate to whatever other handlers are available. Any other result
   *     will be the final result of the query, and no other handlers will be called.
   */
  @Nullable
  JoinResult queryJoin(MatchPlayer joining, @Nullable Competitor chosenParty);

  /**
   * Try to join the given player to the match, or tell them why they can't. This handler does not
   * have to handle the request if it doesn't know how, or doesn't care. Note that a handler is
   * allowed to return a result from {@link #queryJoin} that it does not handle in {@link #join},
   * and vice-versa.
   *
   * @param joining The joining player
   * @param chosenParty A specific party the player wants to join, or null if they have no
   *     preference
   * @param result A fresh result from {@link #queryJoin} that should be used
   * @return True if this implementor "handled" the join, meaning either the player joined the match
   *     successfully, or received some feedback explaining why they didn't. Returning true prevents
   *     any other handlers from being called after this one.
   */
  boolean join(MatchPlayer joining, @Nullable Competitor chosenParty, JoinResult result);
}
