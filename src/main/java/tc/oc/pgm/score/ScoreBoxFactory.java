package tc.oc.pgm.score;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import tc.oc.material.matcher.SingleMaterialMatcher;
import tc.oc.pgm.filters.Filter;
import tc.oc.pgm.match.Match;
import tc.oc.pgm.regions.Region;

public class ScoreBoxFactory {
  private final Region region;
  private final int score;
  private final Filter filter;
  private final ImmutableMap<SingleMaterialMatcher, Double> redeemables;

  public ScoreBoxFactory(
      Region region,
      int score,
      Filter filter,
      ImmutableMap<SingleMaterialMatcher, Double> redeemables) {
    Preconditions.checkNotNull(region, "region");
    Preconditions.checkNotNull(filter, "filter");

    this.region = region;
    this.score = score;
    this.filter = filter;
    this.redeemables = redeemables;
  }

  public ScoreBox createScoreBox(Match match) {
    return new ScoreBox(this.region, this.score, this.filter, this.redeemables);
  }
}
