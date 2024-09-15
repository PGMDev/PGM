package tc.oc.pgm.timelimit;

import static net.kyori.adventure.text.Component.translatable;

import java.time.Duration;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.countdowns.MatchCountdown;
import tc.oc.pgm.util.bukkit.Sounds;

public class TimeLimitCountdown extends MatchCountdown {

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
          this.getMatch().playSound(Sounds.MATCH_COUNTDOWN);
        }
      } else if (secondsLeft > 0) {
        // Tick for the last 30 seconds
        this.getMatch().playSound(Sounds.TIMELIMIT_COUNTDOWN);
      }
      if (secondsLeft == 5) {
        // Play the portal crescendo sound up to the last moment
        this.getMatch().playSound(Sounds.TIMELIMIT_CRESCENDO);
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
