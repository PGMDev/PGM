package tc.oc.pgm.score;

import static java.util.Objects.requireNonNull;

import com.google.common.collect.ImmutableMap;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.region.Region;
import tc.oc.pgm.util.material.matcher.SingleMaterialMatcher;

public class ScoreBoxFactory {
  private final Region region;
  private final int score;
  private final Filter filter;
  private final ImmutableMap<SingleMaterialMatcher, Double> redeemables;
  private final boolean silent;

  public ScoreBoxFactory(
      Region region,
      int score,
      Filter filter,
      ImmutableMap<SingleMaterialMatcher, Double> redeemables,
      boolean silent) {
    requireNonNull(region, "region");
    requireNonNull(filter, "filter");

    this.region = region;
    this.score = score;
    this.filter = filter;
    this.redeemables = redeemables;
    this.silent = silent;
  }

  public ScoreBox createScoreBox(Match match) {
    return new ScoreBox(this.region, this.score, this.filter, this.redeemables, this.silent);
  }
}
