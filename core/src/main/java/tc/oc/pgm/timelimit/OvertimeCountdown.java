package tc.oc.pgm.timelimit;

import java.time.Duration;
import java.time.Instant;
import javax.annotation.Nullable;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.TranslatableComponent;
import net.kyori.text.format.TextColor;
import net.kyori.text.format.TextDecoration;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.util.text.PeriodFormats;

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
      return TextColor.GREEN;
    } else if (seconds > 10) {
      return TextColor.YELLOW;
    } else if (seconds > 5) {
      return TextColor.GOLD;
    } else {
      return TextColor.DARK_RED;
    }
  }

  @Override
  protected Component formatText() {
    return TranslatableComponent.of(
            "misc.overtime",
            TextColor.YELLOW,
            TextComponent.of(colonTime(), urgencyColor()).decoration(TextDecoration.BOLD, false))
        .decoration(TextDecoration.BOLD, true);
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

    match.sendMessage(TranslatableComponent.of("broadcast.overtime", TextColor.YELLOW));
    if (timeLimit.getMaxOvertime() != null) {
      match.sendMessage(
          TranslatableComponent.of(
              "broadcast.overtime.limit",
              TextColor.YELLOW,
              PeriodFormats.briefNaturalApproximate(timeLimit.getMaxOvertime())
                  .color(TextColor.AQUA)));

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
