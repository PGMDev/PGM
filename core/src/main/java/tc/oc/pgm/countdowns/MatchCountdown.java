package tc.oc.pgm.countdowns;

import static net.kyori.adventure.bossbar.BossBar.bossBar;
import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.title.Title.title;
import static tc.oc.pgm.util.TimeUtils.fromTicks;
import static tc.oc.pgm.util.text.TemporalComponent.clock;
import static tc.oc.pgm.util.text.TemporalComponent.seconds;

import java.time.Duration;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.title.Title;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.events.CountdownCancelEvent;
import tc.oc.pgm.events.CountdownEndEvent;
import tc.oc.pgm.events.CountdownStartEvent;

public abstract class MatchCountdown extends Countdown {
  protected final Match match;
  protected final BossBar bossBar;
  protected Duration remaining, total;

  public MatchCountdown(Match match, BossBar bossBar) {
    this.match = match;
    this.bossBar = bossBar;
  }

  public MatchCountdown(Match match, BossBar.Color color) {
    this(match, bossBar(space(), 1, color, BossBar.Overlay.PROGRESS));
  }

  public MatchCountdown(Match match) {
    this(match, BossBar.Color.BLUE);
  }

  public Match getMatch() {
    return this.match;
  }

  public BossBar getBossBar() {
    return this.bossBar;
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

    invalidateBossBar();

    match.callEvent(new CountdownStartEvent(match, this));
    super.onStart(remaining, total);
  }

  @Override
  public void onTick(Duration remaining, Duration total) {
    this.remaining = remaining;
    this.total = total;

    invalidateBossBar();

    if (showChat()) {
      getMatch().sendMessage(formatText());
    }

    if (showTitle()) {
      getMatch()
          .showTitle(
              title(
                  text(remaining.getSeconds(), NamedTextColor.YELLOW),
                  empty(),
                  Title.Times.times(Duration.ZERO, fromTicks(5), fromTicks(15))));
    }

    super.onTick(remaining, total);
  }

  @Override
  public void onEnd(Duration total) {
    match.callEvent(new CountdownEndEvent(match, this));
    hideBossBar();
  }

  @Override
  public void onCancel(Duration remaining, Duration total) {
    super.onCancel(remaining, total);
    match.callEvent(new CountdownCancelEvent(match, this));
    hideBossBar();
  }

  protected void hideBossBar() {
    match.hideBossBar(bossBar);
  }

  protected void invalidateBossBar() {
    if (showBossBar()) {
      bossBar.progress(bossBarProgress(remaining, total));
      bossBar.name(formatText());
      BossBar.Color color = barColor();
      if (color != null) bossBar.color(color);

      match.showBossBar(bossBar);
    } else {
      hideBossBar();
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

  @Nullable
  protected BossBar.Color barColor() {
    return null;
  }

  protected Component secondsRemaining(TextColor color) {
    return seconds(remaining.getSeconds(), color);
  }

  protected TextComponent colonTime() {
    return clock(remaining).color(urgencyColor());
  }

  protected float bossBarProgress(Duration remaining, Duration total) {
    return total.isZero() ? 0f : Math.min(1f, (float) remaining.toMillis() / total.toMillis());
  }

  public Duration getRemaining() {
    return remaining;
  }
}
