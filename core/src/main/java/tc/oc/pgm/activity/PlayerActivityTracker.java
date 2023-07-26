package tc.oc.pgm.activity;

import com.google.common.collect.Maps;
import java.time.Duration;
import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;
import tc.oc.pgm.api.party.Competitor;

public class PlayerActivityTracker {

  private final Map<Competitor, TeamTimeRecord> records;

  public PlayerActivityTracker() {
    this.records = Maps.newHashMap();
  }

  public void start(Competitor competitor) {
    records.computeIfAbsent(competitor, k -> new TeamTimeRecord()).start();
  }

  public void endAll() {
    records.keySet().forEach(this::end);
  }

  public void end(Competitor competitor) {
    if (records.containsKey(competitor)) {
      records.get(competitor).end();
    }
  }

  public Duration getTotalTime() {
    return records.values().stream()
        .map(TeamTimeRecord::getTimePlayed)
        .reduce(Duration.ZERO, Duration::plus);
  }

  public Duration getTimePlayed(Competitor competitor) {
    TeamTimeRecord record = records.get(competitor);
    if (record == null) return Duration.ZERO;

    return record.getTimePlayed();
  }

  public Competitor getPrimaryTeam() {
    if (records.isEmpty()) return null;

    return records.entrySet().stream()
        .max(Comparator.comparing(entry -> entry.getValue().getTimePlayed()))
        .map(Entry::getKey)
        .orElse(null);
  }
}
