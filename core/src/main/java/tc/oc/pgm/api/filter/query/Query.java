package tc.oc.pgm.api.filter.query;

import javax.annotation.Nullable;
import org.bukkit.event.Event;

public interface Query {
  @Nullable
  Event getEvent();
}
