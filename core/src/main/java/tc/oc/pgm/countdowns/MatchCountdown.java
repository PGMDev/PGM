package tc.oc.pgm.countdowns;

import static net.kyori.adventure.title.Title.title;

import java.time.Duration;
import javax.annotation.Nullable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.bossbar.BossBarMatchModule;
import tc.oc.pgm.events.CountdownCancelEvent;
import tc.oc.pgm.events.CountdownEndEvent;
import tc.oc.pgm.events.CountdownStartEvent;
import tc.oc.pgm.util.TimeUtils;
import tc.oc.pgm.util.bossbar.BossBarSource;
import tc.oc.pgm.util.bossbar.DynamicBossBar;

public abstract class MatchCountdown extends Countdown {
  protected final Match match;
  protected final BossBarMatchModule bbmm;
  protected Duration remaining, total;
  protected final BossBarSource bossBar;

  class CountdownBar extends DynamicBossBar {
    @Override
    public boolean isVisible(Player viewer) {
      return true;
    }

    @Override
    public Component getText(Player viewer) {
      return formatText();
    }

    @Override
    public float getMeter(Player viewer) {
      return bossBarProgress(remaining, total);
    }
  };

  public MatchCountdown(Match match, @Nullable BossBarSource bossBar) {
    this.match = match;
    this.bbmm = match.needModule(BossBarMatchModule.class);
    if (bossBar != null) {
      this.bossBar = bossBar;
    } else {
      this.bossBar = new CountdownBar();
    }
  }

  public MatchCountdown(Match match) {
    this(match, null);
  }

  public Match getMatch() {
    return this.match;
  }

  protected abstract Component formatText();

  protected boolean showChat() {
    long secondsLeft = remaining.getSeconds();
    return secondsLeft > 0
        && (secondsLeft % 300 == 0
            || // every 5 minutes
            (secondsLeft % 60 == 0 && secondsLeft <= 300)
            || // every minute for the last 5 minutes
            (secondsLeft % 10 == 0 && secondsLeft <= 30)
            || // every 10 seconds for the last 30 seconds
            secondsLeft <= 5); // every second for the last 5 seconds
  }

  protected boolean showBossBar() {
    return true;
  }

  protected boolean showTitle() {
    return false;
  }

  @Override
  public void onStart(Duration remaining, Duration total) {
    this.remaining = remaining;
    this.total = total;

    showOrHideBossBar();

    match.callEvent(new CountdownStartEvent(match, this));
    super.onStart(remaining, total);
  }

  @Override
  public void onTick(Duration remaining, Duration total) {
    this.remaining = remaining;
    this.total = total;

    showOrHideBossBar();
    invalidateBossBar();

    if (showChat()) {
      getMatch().sendMessage(formatText());
    }

    if (showTitle()) {
      getMatch()
          .showTitle(
              title(
                  Component.text(remaining.getSeconds(), NamedTextColor.YELLOW),
                  Component.empty(),
                  Title.Times.of(Duration.ZERO, Duration.ofMillis(5), Duration.ofMillis(15))));
    }

    super.onTick(remaining, total);
  }

  @Override
  public void onEnd(Duration total) {
    match.callEvent(new CountdownEndEvent(match, this));
    bbmm.removeBossBar(bossBar);
  }

  @Override
  public void onCancel(Duration remaining, Duration total) {
    super.onCancel(remaining, total);
    match.callEvent(new CountdownCancelEvent(match, this));
    bbmm.removeBossBar(bossBar);
  }

  protected void invalidateBossBar() {
    if (bossBar instanceof DynamicBossBar) {
      ((DynamicBossBar) bossBar).invalidate();
    }
  }

  private void showOrHideBossBar() {
    if (showBossBar()) {
      bbmm.pushBossBarIfAbsent(bossBar);
    } else {
      bbmm.removeBossBar(bossBar);
    }
  }

  protected TextColor urgencyColor() {
    long seconds = remaining.getSeconds();
    if (seconds > 60) {
      return NamedTextColor.GREEN;
    } else if (seconds > 30) {
      return NamedTextColor.YELLOW;
    } else if (seconds > 5) {
      return NamedTextColor.GOLD;
    } else {
      return NamedTextColor.DARK_RED;
    }
  }

  protected Component secondsRemaining(TextColor color) {
    long seconds = remaining.getSeconds();
    if (seconds == 1) {
      return Component.translatable("misc.second", Component.text("1", color));
    } else {
      return Component.translatable("misc.seconds", Component.text(String.valueOf(seconds), color));
    }
  }

  protected String colonTime() {
    return TimeUtils.formatDuration(remaining);
  }

  protected float bossBarProgress(Duration remaining, Duration total) {
    return total.isZero() ? 0f : (float) remaining.toMillis() / total.toMillis();
  }

  public Duration getRemaining() {
    return remaining;
  }
}
