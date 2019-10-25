package tc.oc.pgm.development;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import tc.oc.pgm.map.PGMMap;

/** Log handler that collects map-related errors and indexes them by map */
public class MapErrorTracker extends Handler {
  private Multimap<PGMMap, PGMMap.MapLogRecord> errors = ArrayListMultimap.create();

  public MapErrorTracker() {
    this.setLevel(Level.WARNING);
  }

  public Multimap<PGMMap, PGMMap.MapLogRecord> getErrors() {
    return errors;
  }

  public void clearErrors(PGMMap map) {
    errors.removeAll(map);
  }

  /** Clear errors for any maps NOT in the given set */
  public void clearErrorsExcept(Collection<PGMMap> excepted) {
    errors.keySet().retainAll(excepted);
  }

  @Override
  public void publish(LogRecord record) {
    if (record instanceof PGMMap.MapLogRecord && isLoggable(record)) {
      PGMMap.MapLogRecord mapRecord = (PGMMap.MapLogRecord) record;
      errors.put(mapRecord.getMap(), mapRecord);
    }
  }

  @Override
  public void close() throws SecurityException {}

  @Override
  public void flush() {}
}
