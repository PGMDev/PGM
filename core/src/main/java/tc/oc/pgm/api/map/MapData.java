package tc.oc.pgm.api.map;

import java.time.Duration;
import java.time.Instant;
import tc.oc.pgm.api.match.Match;

public interface MapData {

  String getId();

  Instant lastPlayed();

  Duration lastDuration();

  double score();

  void setScore(double score, boolean update);

  void saveMatch(Match match, double score);
}
