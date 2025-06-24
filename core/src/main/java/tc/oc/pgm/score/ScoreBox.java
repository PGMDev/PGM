package tc.oc.pgm.score;

import static tc.oc.pgm.util.Assert.assertNotNull;

import com.google.common.collect.Maps;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.region.Region;
import tc.oc.pgm.util.material.MaterialMatcher;

public class ScoreBox {
  private static final Duration COOLDOWN = Duration.ofMillis(500);
  private final ScoreBoxDefinition definition;

  private final Map<UUID, Instant> lastScoreTime = Maps.newHashMap();

  public ScoreBox(ScoreBoxDefinition definition) {
    this.definition = definition;
  }

  public Region getRegion() {
    return this.definition.region();
  }

  public int getScore() {
    return this.definition.score();
  }

  public Filter getFilter() {
    return this.definition.filter();
  }

  public Map<MaterialMatcher, Double> getRedeemables() {
    return this.definition.redeemables();
  }

  public boolean isSilent() {
    return this.definition.silent();
  }

  public @Nullable Instant getLastScoreTime(MatchPlayer player) {
    assertNotNull(player, "player");

    return this.lastScoreTime.get(player.getId());
  }

  public boolean canScore(MatchPlayer player) {
    return this.definition.filter().query(player).isAllowed();
  }

  public boolean isCoolingDown(MatchPlayer player) {
    Instant lastScore = this.getLastScoreTime(player);
    return lastScore != null && Duration.between(lastScore, Instant.now()).compareTo(COOLDOWN) < 0;
  }

  public void setLastScoreTime(MatchPlayer player, Instant time) {
    assertNotNull(player, "player");
    assertNotNull(time, "time");

    this.lastScoreTime.put(player.getId(), time);
  }
}
