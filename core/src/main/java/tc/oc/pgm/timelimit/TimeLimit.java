package tc.oc.pgm.timelimit;

import static com.google.common.base.Preconditions.checkNotNull;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;

import java.time.Duration;
import java.util.Collection;
import javax.annotation.Nullable;
import net.kyori.adventure.text.Component;
import tc.oc.pgm.api.feature.FeatureInfo;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.party.VictoryCondition;
import tc.oc.pgm.features.SelfIdentifyingFeatureDefinition;
import tc.oc.pgm.util.TimeUtils;
import tc.oc.pgm.util.collection.RankedSet;

@FeatureInfo(name = "time-limit")
public class TimeLimit extends SelfIdentifyingFeatureDefinition implements VictoryCondition {
  private final Duration duration, overtime, maxOvertime;
  private final @Nullable VictoryCondition result;
  private final boolean show;

  public TimeLimit(
      @Nullable String id,
      Duration duration,
      @Nullable Duration overtime,
      @Nullable Duration maxOvertime,
      @Nullable VictoryCondition result,
      boolean show) {
    super(id);
    this.duration = checkNotNull(duration);
    this.overtime = overtime;
    this.maxOvertime = maxOvertime;
    this.result = result;
    this.show = show;
  }

  public Duration getDuration() {
    return duration;
  }

  public @Nullable Duration getOvertime() {
    return overtime;
  }

  public @Nullable Duration getMaxOvertime() {
    return maxOvertime;
  }

  public @Nullable VictoryCondition getResult() {
    return result;
  }

  public boolean getShow() {
    return show;
  }

  @Override
  public Priority getPriority() {
    return Priority.TIME_LIMIT;
  }

  @Override
  public boolean isFinal(Match match) {
    if (result != null && isCompleted(match)) {
      return result.isFinal(match);
    } else {
      return false;
    }
  }

  @Override
  public boolean isCompleted(Match match) {
    return match.needModule(TimeLimitMatchModule.class).isFinished();
  }

  public @Nullable Competitor currentWinner(Match match) {
    Collection<Competitor> winners;
    if (result == null) winners = match.getWinners();
    else {
      RankedSet<Competitor> comp = new RankedSet<>(result);
      comp.addAll(match.getCompetitors());
      winners = comp.getRank(0);
    }
    if (winners.size() == 1) return winners.iterator().next();
    return null;
  }

  @Override
  public int compare(Competitor a, Competitor b) {
    if (result != null && isCompleted(a.getMatch())) {
      return result.compare(a, b);
    } else {
      return 0;
    }
  }

  @Override
  public Component getDescription(Match match) {
    Component time = text(TimeUtils.formatDuration(duration));
    if (result == null) {
      return translatable("match.timeLimit.generic", time);
    } else {
      return translatable("match.timeLimit.result", result.getDescription(match), time);
    }
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" + duration + " result=" + result;
  }
}
