package tc.oc.pgm.join;

import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.match.QueuedParty;
import tc.oc.pgm.teams.Team;

/** Something that is able to join the player to the match. */
public interface JoinHandler {

  default boolean join(MatchPlayer joining, @Nullable Competitor chosenParty) {
    return join(joining, JoinRequest.fromPlayer(joining, (Team) chosenParty));
  }

  default boolean join(MatchPlayer joining, JoinRequest request) {
    JoinResult result = queryJoin(joining, request);
    return join(joining, request, result);
  }

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
  @Deprecated // Kept for backwards-compatibility
  default JoinResult queryJoin(MatchPlayer joining, @Nullable Competitor chosenParty) {
    return queryJoin(joining, JoinRequest.fromPlayer(joining, (Team) chosenParty));
  }

  JoinResult queryJoin(MatchPlayer joining, JoinRequest request);

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
  @Deprecated // Kept for backwards-compatibility
  default boolean join(MatchPlayer joining, @Nullable Competitor chosenParty, JoinResult result) {
    return join(joining, JoinRequest.fromPlayer(joining, (Team) chosenParty), result);
  }

  boolean join(MatchPlayer joining, JoinRequest request, JoinResult result);

  /**
   * Join the given player to the match if at all possible. This is only called by staff commands.
   *
   * @param joining The joining player
   * @param forcedParty The party the player must be added to, or null if it doesn't matter what
   *     party they join. If the handler receives a party that it doesn't know what to do with, it
   *     should do nothing and return false.
   * @return True if the player was joined, or was already joined
   */
  default boolean forceJoin(MatchPlayer joining, @Nullable Competitor forcedParty) {
    return join(joining, JoinRequest.of((Team) forcedParty, JoinRequest.Flag.FORCE));
  }

  /**
   * Try to join all of the given players simultaneously. This is called with all queued players
   * when the match starts. This method will be called on all handlers, breaking if the queue
   * becomes empty. Any players left in the queue will be joined through {@link #join}, and finally
   * sent to obs if that fails.
   */
  void queuedJoin(QueuedParty queue);
}
