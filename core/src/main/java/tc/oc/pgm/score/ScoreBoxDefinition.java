package tc.oc.pgm.score;

import static tc.oc.pgm.util.Assert.assertNotNull;

import com.google.common.collect.ImmutableMap;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.action.Action;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.region.Region;
import tc.oc.pgm.util.material.MaterialMatcher;

public record ScoreBoxDefinition(
    Region region,
    int score,
    Filter filter,
    ImmutableMap<MaterialMatcher, Double> redeemables,
    boolean silent,
    @Nullable Action<? super MatchPlayer> action) {
  public ScoreBoxDefinition {
    assertNotNull(region, "region");
    assertNotNull(filter, "filter");
  }

  public ScoreBox createScoreBox(Match match) {
    return new ScoreBox(this);
  }
}
