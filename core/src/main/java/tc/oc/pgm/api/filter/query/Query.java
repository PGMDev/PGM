package tc.oc.pgm.api.filter.query;

import org.bukkit.event.Event;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface Query {
  @Nullable
  Event getEvent();
}
