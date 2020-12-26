package tc.oc.pgm.filters;

import tc.oc.pgm.api.feature.FeatureReference;
import tc.oc.pgm.filters.query.FlagQuery;
import tc.oc.pgm.flag.Flag;
import tc.oc.pgm.flag.Post;

public class PreviousPostFilter extends TypedFilter<FlagQuery> {
  private final FeatureReference<? extends Post> postReference;

  public PreviousPostFilter(FeatureReference<? extends Post> post) {
    this.postReference = post;
  }

  @Override
  public Class<? extends FlagQuery> getQueryType() {
    return FlagQuery.class;
  }

  @Override
  protected QueryResponse queryTyped(FlagQuery query) {
    final Post post = this.postReference.get();
    final Flag flag = query.getFlag();
    if (post == null) return QueryResponse.ABSTAIN;
    return QueryResponse.fromBoolean(flag.getPreviousPost().equals(post));
  }
}
