package tc.oc.pgm.timelimit;

import static com.google.common.base.Preconditions.checkNotNull;

import java.time.Duration;
import javax.annotation.Nullable;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.TranslatableComponent;
import tc.oc.pgm.api.feature.FeatureInfo;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.party.VictoryCondition;
import tc.oc.pgm.features.SelfIdentifyingFeatureDefinition;
import tc.oc.pgm.util.TimeUtils;

@FeatureInfo(name = "time-limit")
public class TimeLimit extends SelfIdentifyingFeatureDefinition implements VictoryCondition {
  private final Duration duration;
  private final @Nullable VictoryCondition result;
  private final boolean show;

  public TimeLimit(
      @Nullable String id, Duration duration, @Nullable VictoryCondition result, boolean show) {
    super(id);
    this.duration = checkNotNull(duration);
    this.result = result;
    this.show = show;
  }

  public Duration getDuration() {
    return duration;
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
    TimeLimitCountdown countdown = match.needModule(TimeLimitMatchModule.class).getCountdown();
    return countdown != null && match.getCountdown().isFinished(countdown);
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
    Component time = TextComponent.of(TimeUtils.formatDuration(duration));
    if (result == null) {
      return TranslatableComponent.of("match.timeLimit.generic", time);
    } else {
      return TranslatableComponent.of("match.timeLimit.result", result.getDescription(match), time);
    }
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" + duration + " result=" + result;
  }
}
