package tc.oc.pgm.timelimit;

import static net.kyori.adventure.key.Key.key;
import static net.kyori.adventure.sound.Sound.sound;
import static net.kyori.adventure.text.Component.translatable;

import java.time.Duration;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.countdowns.MatchCountdown;

public class TimeLimitCountdown extends MatchCountdown {

  private static final Sound NOTICE_SOUND =
      sound(key("note.pling"), Sound.Source.MASTER, 1f, 1.19f); // Significant moments
  private static final Sound IMMINENT_SOUND =
      sound(key("random.click"), Sound.Source.MASTER, 0.25f, 2f); // Last 30 seconds
  private static final Sound CRESCENDO_SOUND =
      sound(key("portal.trigger"), Sound.Source.MASTER, 1f, 0.78f); // Last few seconds

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
    return translatable("misc.timeRemaining", NamedTextColor.AQUA, colonTime());
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

  @Nullable
  protected BossBar.Color barColor() {
    long seconds = remaining.getSeconds();
    if (seconds > 60) {
      return BossBar.Color.GREEN;
    } else if (seconds > 30) {
      return BossBar.Color.YELLOW;
    } else {
      return BossBar.Color.RED;
    }
  }

  protected void freeze(Duration remaining) {
    this.remaining = remaining;
    hideBossBar();
  }

  protected boolean mayEnd() {
    return timeLimit.getOvertime() == null || timeLimit.currentWinner(match) != null;
  }

  @Override
  public void onEnd(Duration total) {
    super.onEnd(total);
    TimeLimitMatchModule tl = this.getMatch().needModule(TimeLimitMatchModule.class);
    if (mayEnd()) {
      tl.setFinished(true);
      this.getMatch().calculateVictory();
    } else {
      tl.startOvertime();
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
