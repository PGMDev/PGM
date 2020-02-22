package tc.oc.pgm.filters.query;

import javax.annotation.Nullable;
import org.bukkit.event.Event;

public interface IQuery {
  @Nullable
  Event getEvent();
}
