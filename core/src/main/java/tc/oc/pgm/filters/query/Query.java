package tc.oc.pgm.filters.query;

import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

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
