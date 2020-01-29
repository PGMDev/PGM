package tc.oc.pgm.modes;

import java.util.Set;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.joda.time.Duration;
import tc.oc.component.Component;
import tc.oc.component.types.PersonalizedText;
import tc.oc.component.types.PersonalizedTranslatable;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.countdowns.CountdownContext;
import tc.oc.pgm.countdowns.MatchCountdown;
import tc.oc.pgm.timelimit.TimeLimitCountdown;

public class ModeChangeCountdown extends MatchCountdown implements Comparable<ModeChangeCountdown> {

  private final CountdownContext context;
  private final Mode mode;

  public ModeChangeCountdown(Match match, ObjectiveModesMatchModule parent, Mode mode) {
    super(match);

    this.context = parent.getCountdown();
    this.mode = mode;
  }

  public Mode getMode() {
    return this.mode;
  }

  @Override
  public void onEnd(Duration total) {
    super.onEnd(total);
    Bukkit.getPluginManager().callEvent(new ObjectiveModeChangeEvent(this.getMatch(), this.mode));
  }

  /**
   * Sorts two {@link tc.oc.pgm.modes.ModeChangeCountdown} based on whether or not the match is
   * running. If the match is running, then the modes are sorted by the time left. If the match is
   * not running, then the modes are sorted by the time their modes will have left.
   *
   * @param that The mode to compare to.
   */
  @Override
  public int compareTo(ModeChangeCountdown that) {
    boolean running = this.getMatch().isRunning();

    Duration d1 = running ? this.context.getTimeLeft(this) : this.getMode().getAfter();
    Duration d2 = running ? this.context.getTimeLeft(that) : that.getMode().getAfter();

    return d1.compareTo(d2);
  }

  @Override
  protected Component formatText() {
    return new PersonalizedText(
        new PersonalizedTranslatable(
            "match.objectiveMode.countdown",
            getMode().getComponentName(),
            secondsRemaining(ChatColor.AQUA)),
        ChatColor.DARK_AQUA);
  }

  @Override
  protected float bossBarProgress(Duration remaining, Duration total) {
    return super.bossBarProgress(remaining, this.mode.getShowBefore());
  }

  @Override
  public boolean showBossBar() {
    CountdownContext countdowns = this.getMatch().getCountdown();
    Set<TimeLimitCountdown> timeLimitCountdowns = countdowns.getAll(TimeLimitCountdown.class);

    for (TimeLimitCountdown limit : timeLimitCountdowns) {
      // Don't show the countdown if it wont happen before the match ends
      if (countdowns.getTimeLeft(limit).isShorterThan(remaining)) {
        return false;
      }
    }

    return remaining.getStandardSeconds() < this.mode.getShowBefore().getStandardSeconds();
  }

  @Override
  protected boolean showChat() {
    return false;
  }
}
