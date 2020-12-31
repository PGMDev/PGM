package tc.oc.pgm.timelimit;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;

import java.time.Duration;
import java.time.Instant;
import javax.annotation.Nullable;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.util.text.TemporalComponent;

public class OvertimeCountdown extends TimeLimitCountdown {

  private @Nullable Instant maxRefresh;
  private @Nullable Competitor winner;

  public OvertimeCountdown(Match match, TimeLimit timeLimit) {
    super(match, timeLimit);
  }

  public @Nullable Duration getRemaining() {
    return remaining;
  }

  @Override
  protected TextColor urgencyColor() {
    long seconds = remaining.getSeconds();
    if (seconds > 20) {
      return NamedTextColor.GREEN;
    } else if (seconds > 10) {
      return NamedTextColor.YELLOW;
    } else if (seconds > 5) {
      return NamedTextColor.GOLD;
    } else {
      return NamedTextColor.DARK_RED;
    }
  }

  @Override
  protected Component formatText() {
    return translatable(
            "misc.overtime",
            NamedTextColor.YELLOW,
            text(colonTime(), urgencyColor()).decoration(TextDecoration.BOLD, false))
        .decoration(TextDecoration.BOLD, true);
  }

  @Nullable
  protected BossBar.Color barColor() {
    return BossBar.Color.YELLOW;
  }

  @Override
  protected boolean showChat() {
    return false;
  }

  @Override
  protected boolean playSounds() {
    // Play sounds on the last 5 seconds, but only if overtime isn't super short
    return remaining.getSeconds() <= 5 && total.getSeconds() >= 10;
  }

  @Override
  public void onStart(Duration remaining, Duration total) {
    super.onStart(remaining, total);

    // Should never happen, but rather play safe
    if (timeLimit.getOvertime() == null) return;

    match.sendMessage(translatable("broadcast.overtime", NamedTextColor.YELLOW));
    if (timeLimit.getMaxOvertime() != null) {
      match.sendMessage(
          translatable(
              "broadcast.overtime.limit",
              NamedTextColor.YELLOW,
              TemporalComponent.briefNaturalApproximate(timeLimit.getMaxOvertime())
                  .color(NamedTextColor.AQUA)));

      maxRefresh = Instant.now().plus(timeLimit.getMaxOvertime()).minus(timeLimit.getOvertime());
    }
  }

  @Override
  public void onTick(Duration remaining, Duration total) {
    Competitor newWinner = timeLimit.currentWinner(match);

    if ((newWinner == null || this.winner != newWinner)
        && (maxRefresh == null || !Instant.now().isAfter(maxRefresh))) {
      this.winner = newWinner;
      start(); // Force the countdown to be re-scheduled
      remaining = total;
    }
    super.onTick(remaining, total);
  }

  protected boolean mayEnd() {
    return true;
  }

  public void start() {
    this.getMatch().getCountdown().start(this, this.timeLimit.getOvertime());
  }
}
