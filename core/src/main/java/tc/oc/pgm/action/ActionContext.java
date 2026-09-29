package tc.oc.pgm.action;

import org.jspecify.annotations.Nullable;
import tc.oc.pgm.api.filter.query.Query;
import tc.oc.pgm.util.math.LocalFrame;

public record ActionContext(
    @Nullable Query query,
    @Nullable LocalFrame frame,
    @Nullable ControlFlow flow) {
  public static final ActionContext EMPTY = new ActionContext(null, null, null);

  public static ActionContext query(Query query) {
    return new ActionContext(query, null, null);
  }

  public ActionContext withFrame(@Nullable LocalFrame frame) {
    return new ActionContext(query, frame, flow);
  }

  public ActionContext withFlow(@Nullable ControlFlow flow) {
    return new ActionContext(query, frame, flow);
  }

  public ActionContext withoutQuery() {
    return new ActionContext(null, frame, flow);
  }

  public boolean halted() {
    return flow != null && flow.halted();
  }

  public boolean continueLoop() {
    return flow == null || flow.continueLoop();
  }

  public Query queryOr(Query fallback) {
    return query != null ? query : fallback;
  }
}
