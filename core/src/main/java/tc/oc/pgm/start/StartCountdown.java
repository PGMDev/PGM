package tc.oc.pgm.start;

import static com.google.common.base.Preconditions.checkNotNull;

import java.time.Duration;
import javax.annotation.Nullable;
import net.kyori.text.Component;
import net.kyori.text.TranslatableComponent;
import net.kyori.text.format.TextColor;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.teams.Team;
import tc.oc.pgm.teams.TeamMatchModule;
import tc.oc.pgm.util.TimeUtils;

/** Countdown to team huddle, or match start if huddle is disabled */
public class StartCountdown extends PreMatchCountdown {

  // At this duration before match start, broadcast a warning if teams will be auto-balanced
  private static final Duration BALANCE_WARNING_TIME = Duration.ofSeconds(15);
  // TODO: Avoid coupling to the team module, either by subclassing this countdown,
  // or implementing some kind of countdown listener system.
  private final @Nullable TeamMatchModule tmm;
  private final Duration huddle;
  private boolean autoBalanced, balanceWarningSent;
  protected final boolean forced;

  public StartCountdown(Match match, boolean forced, Duration huddle) {
    super(match);
    this.huddle = checkNotNull(huddle);
    this.forced = forced;
    this.tmm = match.getModule(TeamMatchModule.class);
  }

  protected boolean willHuddle() {
    return !huddle.isZero() && !huddle.isNegative();
  }

  @Override
  protected Component formatText() {
    return TranslatableComponent.of(
        "countdown.matchStart", TextColor.GREEN, secondsRemaining(TextColor.DARK_RED));
  }

  @Override
  protected boolean showTitle() {
    return !willHuddle() && super.showTitle();
  }

  @Override
  public void onStart(Duration remaining, Duration total) {
    super.onStart(remaining, total);
    this.autoBalanced = false;
  }

  @Override
  @SuppressWarnings("deprecation")
  public void onTick(Duration remaining, Duration total) {
    super.onTick(remaining, total);

    if (remaining.getSeconds() >= 1 && remaining.getSeconds() <= 3) {
      // Auto-balance runs at match start as well, but try to run it a few seconds in advance
      if (this.tmm != null && !this.autoBalanced) {
        this.autoBalanced = true;
        this.tmm.balanceTeams();
      }
    }

    if (this.tmm != null
        && !this.autoBalanced
        && !this.balanceWarningSent
        && !TimeUtils.isLongerThan(remaining, BALANCE_WARNING_TIME)) {
      for (Team team : this.tmm.getParticipatingTeams()) {
        if (team.isStacked()) {
          this.balanceWarningSent = true;
          this.getMatch()
              .sendWarning(TranslatableComponent.of("match.balanceTeams", team.getName()));
        }
      }

      if (this.balanceWarningSent) {
        this.getMatch().playSound(COUNT_SOUND);
      }
    }
  }

  @Override
  public void onEnd(Duration total) {
    super.onEnd(total);

    if (this.tmm != null) this.tmm.balanceTeams();

    if (willHuddle()) {
      getMatch().getCountdown().start(new HuddleCountdown(getMatch()), huddle);
    } else {
      getMatch().start();
    }
  }

  public boolean isForced() {
    return forced;
  }
}
