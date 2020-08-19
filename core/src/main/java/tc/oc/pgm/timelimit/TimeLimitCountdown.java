package tc.oc.pgm.timelimit;

import java.time.Duration;
import javax.annotation.Nullable;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.TranslatableComponent;
import net.kyori.text.format.TextColor;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.countdowns.MatchCountdown;
import tc.oc.pgm.util.chat.Sound;

public class TimeLimitCountdown extends MatchCountdown {

  private static final Sound NOTICE_SOUND =
      new Sound("note.pling", 1f, 1.19f); // Significant moments
  private static final Sound IMMINENT_SOUND =
      new Sound("random.click", 0.25f, 2f); // Last 30 seconds
  private static final Sound CRESCENDO_SOUND =
      new Sound("portal.trigger", 1f, 0.78f); // Last few seconds

  protected final TimeLimit timeLimit;

  public TimeLimitCountdown(Match match, TimeLimit timeLimit) {
    super(match);
    this.timeLimit = timeLimit;
  }

  public @Nullable Duration getRemaining() {
    return remaining;
  }

  @Override
  protected Component formatText() {
    return TranslatableComponent.of(
        "misc.timeRemaining", TextColor.AQUA, TextComponent.of(colonTime(), urgencyColor()));
  }

  @Override
  protected boolean showChat() {
    return this.timeLimit.getShow() && super.showChat();
  }

  @Override
  protected boolean showBossBar() {
    return this.timeLimit.getShow() && super.showBossBar();
  }

  protected boolean playSounds() {
    return this.timeLimit.getShow();
  }

  @Override
  public void onTick(Duration remaining, Duration total) {
    super.onTick(remaining, total);

    if (this.playSounds()) {
      long secondsLeft = remaining.getSeconds();
      if (secondsLeft > 30) {
        // Beep for chat messages before the last 30 seconds
        if (this.showChat()) {
          this.getMatch().playSound(NOTICE_SOUND);
        }
      } else if (secondsLeft > 0) {
        // Tick for the last 30 seconds
        this.getMatch().playSound(IMMINENT_SOUND);
      }
      if (secondsLeft == 5) {
        // Play the portal crescendo sound up to the last moment
        this.getMatch().playSound(CRESCENDO_SOUND);
      }
    }
  }

  protected void freeze(Duration remaining) {
    this.remaining = remaining;
    invalidateBossBar();
  }

  protected boolean mayEnd() {
    return timeLimit.getOvertime() == null || timeLimit.currentWinner(match) != null;
  }

  @Override
  public void onEnd(Duration total) {
    super.onEnd(total);
    if (mayEnd()) {
      this.getMatch().calculateVictory();
    } else {
      TimeLimitMatchModule tl = this.getMatch().getModule(TimeLimitMatchModule.class);
      if (tl != null) tl.startOvertime();
    }
    this.freeze(Duration.ZERO);
  }

  @Override
  public void onCancel(Duration remaining, Duration total) {
    super.onCancel(remaining, total);
    if (this.getMatch().isFinished()) {
      this.freeze(remaining);
    }
  }

  public void start() {
    this.getMatch().getCountdown().start(this, this.timeLimit.getDuration());
  }

  public void cancel() {
    this.getMatch().getCountdown().cancel(this);
  }
}
