package tc.oc.pgm.filters.query;

import javax.annotation.Nullable;
import org.bukkit.event.Event;

public class Query implements tc.oc.pgm.api.filter.query.Query {

  private final @Nullable Event event;

  public Query(@Nullable Event event) {
    this.event = event;
  }

  @Override
  public @Nullable Event getEvent() {
    return event;
  }
}
