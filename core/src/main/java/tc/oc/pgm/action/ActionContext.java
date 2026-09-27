package tc.oc.pgm.action;

import org.jspecify.annotations.Nullable;
import tc.oc.pgm.api.filter.query.Query;
import tc.oc.pgm.util.math.LocalFrame;

public record ActionContext(@Nullable Query query, @Nullable LocalFrame frame) {
  public static final ActionContext EMPTY = new ActionContext(null, null);

  public static ActionContext query(Query query) {
    return new ActionContext(query, null);
  }

  public ActionContext withFrame(@Nullable LocalFrame frame) {
    return new ActionContext(query, frame);
  }

  public ActionContext withoutQuery() {
    return new ActionContext(null, frame);
  }

  public Query queryOr(Query fallback) {
    return query != null ? query : fallback;
  }
}
