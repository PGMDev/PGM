package tc.oc.pgm.score;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import javax.annotation.Nullable;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.player.MatchPlayerState;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.api.region.Region;
import tc.oc.pgm.filters.query.PlayerStateQuery;
import tc.oc.pgm.util.material.matcher.SingleMaterialMatcher;

public class ScoreBox {
  private final Region region;
  private final int score;
  private final Filter filter;
  private final ImmutableMap<SingleMaterialMatcher, Double> redeemables;

  private final Map<MatchPlayerState, Instant> lastScoreTime = Maps.newHashMap();

  public ScoreBox(
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

  public Region getRegion() {
    return this.region;
  }

  public int getScore() {
    return this.score;
  }

  public Filter getFilter() {
    return this.filter;
  }

  public Map<SingleMaterialMatcher, Double> getRedeemables() {
    return redeemables;
  }

  public @Nullable Instant getLastScoreTime(MatchPlayerState player) {
    Preconditions.checkNotNull(player, "player");

    return this.lastScoreTime.get(player);
  }

  public boolean canScore(ParticipantState player) {
    return this.filter.query(new PlayerStateQuery(null, player)).isAllowed();
  }

  public boolean isCoolingDown(MatchPlayerState player) {
    Instant lastScore = this.getLastScoreTime(player);
    return lastScore != null && lastScore.plus(Duration.ofSeconds(1)).isAfter(Instant.now());
  }

  public void setLastScoreTime(MatchPlayerState player, Instant time) {
    Preconditions.checkNotNull(player, "player");
    Preconditions.checkNotNull(time, "time");

    this.lastScoreTime.put(player, time);
  }
}
