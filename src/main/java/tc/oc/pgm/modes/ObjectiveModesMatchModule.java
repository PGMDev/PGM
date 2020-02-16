package tc.oc.pgm.modes;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import tc.oc.component.types.PersonalizedText;
import tc.oc.pgm.api.chat.Sound;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.countdowns.CountdownContext;

public class ObjectiveModesMatchModule implements MatchModule {

  private static final Sound SOUND = new Sound("mob.zombie.remedy", 0.15f, 1.2f);

  private final Match match;
  private final List<Mode> modes;
  private final List<ModeChangeCountdown> countdowns;
  private final CountdownContext countdownContext;

  public ObjectiveModesMatchModule(Match match, List<Mode> modes) {
    this.match = match;
    this.modes = modes;
    this.countdowns = new ArrayList<>(this.modes.size());
    this.countdownContext = new CountdownContext(match, match.getLogger());
  }

  @Override
  public void load() {
    for (Mode mode : this.modes) {
      ModeChangeCountdown countdown = new ModeChangeCountdown(match, this, mode);
      this.countdowns.add(countdown);
    }
  }

  @Override
  public void enable() {
    for (ModeChangeCountdown countdown : this.countdowns) {
      this.countdownContext.start(countdown, countdown.getMode().getAfter());
    }
  }

  @Override
  public void disable() {
    for (ModeChangeCountdown countdown : this.getAllCountdowns()) {
      this.countdownContext.cancel(countdown);
    }
  }

  public CountdownContext getCountdown() {
    return this.countdownContext;
  }

  public List<ModeChangeCountdown> getAllCountdowns() {
    return new ImmutableList.Builder<ModeChangeCountdown>()
        .addAll(this.countdownContext.getAll(ModeChangeCountdown.class))
        .build();
  }

  public List<ModeChangeCountdown> getSortedCountdowns() {
    List<ModeChangeCountdown> listClone = new ArrayList<>(this.countdowns);
    Collections.sort(listClone);

    return listClone;
  }

  public List<ModeChangeCountdown> getActiveCountdowns() {
    List<ModeChangeCountdown> activeCountdowns =
        new ArrayList<>(
            Collections2.filter(
                this.getAllCountdowns(),
                new Predicate<ModeChangeCountdown>() {
                  @Override
                  public boolean apply(@Nullable ModeChangeCountdown countdown) {
                    return ObjectiveModesMatchModule.this
                            .getCountdown()
                            .getTimeLeft(countdown)
                            .getStandardSeconds()
                        > 0;
                  }
                }));
    Collections.sort(activeCountdowns);

    return activeCountdowns;
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onObjectiveModeChange(ObjectiveModeChangeEvent event) {
    event
        .getMatch()
        .sendMessage(
            new PersonalizedText(ChatColor.DARK_AQUA)
                .extra("> > > > ")
                .extra(new PersonalizedText(event.getName(), ChatColor.RED))
                .extra(" < < < <"));
    event.getMatch().playSound(SOUND);
  }
}
