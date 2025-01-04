package tc.oc.pgm.api.map;

import java.time.Duration;
import java.time.Instant;

public interface MapData {

  String getId();

  Instant lastPlayed();

  Duration lastDuration();

  double score();

  void setScore(double score, boolean update);

  void saveMatch(Duration duration, double score);
}
