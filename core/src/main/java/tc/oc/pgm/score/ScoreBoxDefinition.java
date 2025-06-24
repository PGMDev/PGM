package tc.oc.pgm.score;

import static tc.oc.pgm.util.Assert.assertNotNull;

import com.google.common.collect.ImmutableMap;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.region.Region;
import tc.oc.pgm.util.material.MaterialMatcher;

public record ScoreBoxDefinition(
    Region region,
    int score,
    Filter filter,
    ImmutableMap<MaterialMatcher, Double> redeemables,
    boolean silent) {
  public ScoreBoxDefinition {
    assertNotNull(region, "region");
    assertNotNull(filter, "filter");
  }

  public ScoreBox createScoreBox(Match match) {
    return new ScoreBox(this);
  }
}
