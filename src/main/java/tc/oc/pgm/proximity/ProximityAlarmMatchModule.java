package tc.oc.pgm.proximity;

import com.google.common.collect.Sets;
import java.util.Set;
import org.bukkit.event.HandlerList;
import org.bukkit.scheduler.BukkitTask;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;

public class ProximityAlarmMatchModule implements MatchModule {
  private static final long ALARM_INTERVAL = 3;

  private final Match match;
  protected final Set<ProximityAlarm> proximityAlarms = Sets.newHashSet();
  protected BukkitTask task;

  public ProximityAlarmMatchModule(Match match, Set<ProximityAlarmDefinition> definitions) {
    this.match = match;
    for (ProximityAlarmDefinition definition : definitions) {
      proximityAlarms.add(new ProximityAlarm(match, definition, match.getRandom()));
    }
  }

  @Override
  public void enable() {
    for (ProximityAlarm proximityAlarm : this.proximityAlarms) {
      this.match.addListener(proximityAlarm, MatchScope.RUNNING);
    }

    this.task =
        this.match
            .getScheduler(MatchScope.RUNNING)
            .runTaskTimer(
                ALARM_INTERVAL,
                new Runnable() {
                  @Override
                  public void run() {
                    for (ProximityAlarm proximityAlarm :
                        ProximityAlarmMatchModule.this.proximityAlarms) {
                      proximityAlarm.showAlarm();
                    }
                  }
                });
  }

  @Override
  public void disable() {
    this.task.cancel();

    for (ProximityAlarm proximityAlarm : this.proximityAlarms) {
      HandlerList.unregisterAll(proximityAlarm);
    }
  }
}
