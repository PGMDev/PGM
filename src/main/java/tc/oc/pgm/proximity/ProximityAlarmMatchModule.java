package tc.oc.pgm.proximity;

import com.google.common.collect.Sets;
import java.util.Random;
import java.util.Set;
import org.bukkit.event.HandlerList;
import org.bukkit.scheduler.BukkitTask;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.match.MatchModule;

public class ProximityAlarmMatchModule extends MatchModule {
  private static final long ALARM_INTERVAL = 3;

  protected final Set<ProximityAlarm> proximityAlarms = Sets.newHashSet();
  protected BukkitTask task;

  public ProximityAlarmMatchModule(Match match, Set<ProximityAlarmDefinition> definitions) {
    super(match);

    Random random = new Random();
    for (ProximityAlarmDefinition definition : definitions) {
      proximityAlarms.add(new ProximityAlarm(this.match, definition, random));
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
