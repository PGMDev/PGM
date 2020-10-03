package tc.oc.pgm.filters;

import tc.oc.pgm.api.feature.FeatureReference;
import tc.oc.pgm.filters.query.MatchQuery;
import tc.oc.pgm.payload.Payload;
import tc.oc.pgm.payload.PayloadCheckpoint;
import tc.oc.pgm.payload.PayloadDefinition;

/** Checks if a given checkpoint has been passed by the given payload */
public class PayloadCheckpointFilter extends TypedFilter<MatchQuery> {

  private final FeatureReference<? extends PayloadDefinition> payload;

  private final String id;

  public PayloadCheckpointFilter(FeatureReference<? extends PayloadDefinition> payload, String id) {
    this.payload = payload;
    this.id = id;
  }

  @Override
  public Class<? extends MatchQuery> getQueryType() {
    return MatchQuery.class;
  }

  @Override
  protected QueryResponse queryTyped(MatchQuery query) {
    Payload payload = (Payload) this.payload.get().getGoal(query.getMatch());
    if (payload == null) return QueryResponse.ABSTAIN;

    PayloadCheckpoint filterCheckpoint = payload.getCheckpoint(id);
    PayloadCheckpoint lastReachedCheckpoint = payload.getLastReachedCheckpoint();

    // If no checkpoints has been reached or the given checkpoint id is wrong
    if (lastReachedCheckpoint.isMiddle() || filterCheckpoint == null) return QueryResponse.DENY;

    int lastReachedCheckpointKey = lastReachedCheckpoint.getMapIndex();
    int filterCheckpointKey = filterCheckpoint.getMapIndex();

    // if both keys are on the same side of 0 (both are positive/negative)
    if ((lastReachedCheckpointKey ^ filterCheckpointKey) >> 31 == 0) {
      // check if the furthest reached checkpoint is past/is this filters checkpoint
      return QueryResponse.fromBoolean(
          Math.abs(lastReachedCheckpointKey) <= Math.abs(filterCheckpointKey));
    } else return QueryResponse.DENY;
  }
}
