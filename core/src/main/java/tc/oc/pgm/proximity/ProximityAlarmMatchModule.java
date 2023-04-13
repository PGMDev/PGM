package tc.oc.pgm.proximity;

import com.google.common.collect.Sets;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.bukkit.scheduler.BukkitTask;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;

public class ProximityAlarmMatchModule implements MatchModule {
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
      this.match.addListener(proximityAlarm.playerTracker, MatchScope.RUNNING);
    }

    match
        .getExecutor(MatchScope.RUNNING)
        .scheduleWithFixedDelay(
            () -> {
              for (ProximityAlarm proximityAlarm : ProximityAlarmMatchModule.this.proximityAlarms) {
                proximityAlarm.showAlarm();
              }
            },
            0,
            1,
            TimeUnit.SECONDS);
  }
}
