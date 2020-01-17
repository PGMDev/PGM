package tc.oc.pgm.countdowns;

import javax.annotation.Nullable;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.entity.Player;
import org.joda.time.Duration;
import tc.oc.bossbar.BossBar;
import tc.oc.bossbar.DynamicBossBar;
import tc.oc.component.Component;
import tc.oc.component.types.PersonalizedText;
import tc.oc.component.types.PersonalizedTranslatable;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.bossbar.BossBarMatchModule;
import tc.oc.util.components.PeriodFormats;

public abstract class MatchCountdown extends Countdown {
  protected final Match match;
  protected final BossBarMatchModule bbmm;
  protected Duration remaining, total;

  protected final BossBar bossBar;

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
      return Duration.ZERO.equals(total) ? 0f : (float) remaining.getMillis() / total.getMillis();
    }
  };

  public MatchCountdown(Match match, @Nullable BossBar bossBar) {
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
    long secondsLeft = remaining.getStandardSeconds();
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

    super.onStart(remaining, total);
  }

  @Override
  public void onTick(Duration remaining, Duration total) {
    this.remaining = remaining;
    this.total = total;

    showOrHideBossBar();
    invalidateBossBar();

    if (showChat()) {
      getMatch().sendMessage(formatText().toLegacyText());
    }

    if (showTitle()) {
      getMatch()
          .showTitle(
              new PersonalizedText(
                  String.valueOf(remaining.getStandardSeconds()), ChatColor.YELLOW),
              new PersonalizedText(""),
              0,
              5,
              15);
    }

    super.onTick(remaining, total);
  }

  @Override
  public void onEnd(Duration total) {
    bbmm.removeBossBar(bossBar);
  }

  @Override
  public void onCancel(Duration remaining, Duration total) {
    super.onCancel(remaining, total);
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

  protected ChatColor urgencyColor() {
    long seconds = remaining.getStandardSeconds();
    if (seconds > 60) {
      return ChatColor.GREEN;
    } else if (seconds > 30) {
      return ChatColor.YELLOW;
    } else if (seconds > 5) {
      return ChatColor.GOLD;
    } else {
      return ChatColor.DARK_RED;
    }
  }

  protected Component secondsRemaining(ChatColor color) {
    long seconds = remaining.getStandardSeconds();
    if (seconds == 1) {
      return new PersonalizedTranslatable(
          "countdown.singularCompound", new PersonalizedText("1", color));
    } else {
      return new PersonalizedTranslatable(
          "countdown.pluralCompound", new PersonalizedText(String.valueOf(seconds), color));
    }
  }

  protected String colonTime() {
    return PeriodFormats.COLONS.print(remaining.toPeriod());
  }

  protected float bossBarProgress(Duration remaining, Duration total) {
    return Duration.ZERO.equals(total) ? 0f : (float) remaining.getMillis() / total.getMillis();
  }
}
