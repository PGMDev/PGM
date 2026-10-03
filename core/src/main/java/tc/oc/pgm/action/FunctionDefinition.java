package tc.oc.pgm.action;

import com.google.common.collect.ImmutableMap;
import tc.oc.pgm.api.feature.FeatureInfo;
import tc.oc.pgm.features.SelfIdentifyingFeatureDefinition;
import tc.oc.pgm.filters.Filterable;
import tc.oc.pgm.util.math.LocalFrame;
import tc.oc.pgm.util.math.LocalRef;
import tc.oc.pgm.variables.LocalScope;

/** A reusable action body with parameters, private locals and an optional return value. */
@FeatureInfo(name = "function")
public class FunctionDefinition extends SelfIdentifyingFeatureDefinition {
  private final Class<?> scope;
  private final ImmutableMap<String, LocalRef> params;
  private final LocalScope locals;
  private final Action<?> body;
  private final boolean returnsValue;

  public FunctionDefinition(
      String id,
      Class<?> scope,
      ImmutableMap<String, LocalRef> params,
      LocalScope locals,
      Action<?> body,
      boolean returnsValue) {
    super(id);
    this.scope = scope;
    this.params = params;
    this.locals = locals;
    this.body = body;
    this.returnsValue = returnsValue;
  }

  public Class<?> getScope() {
    return scope;
  }

  public ImmutableMap<String, LocalRef> getParams() {
    return params;
  }

  public boolean returnsValue() {
    return returnsValue;
  }

  public LocalFrame createFrame() {
    return locals.createFrame(null);
  }

  @SuppressWarnings("unchecked")
  public ControlFlow call(Filterable<?> t, ActionContext context, LocalFrame frame) {
    var flow = new ControlFlow();
    ((Action<Filterable<?>>) body).trigger(t, new ActionContext(context.query(), frame, flow));
    return flow;
  }
}
