package tc.oc.pgm.action;

import com.google.common.collect.ImmutableMap;
import tc.oc.pgm.api.feature.FeatureInfo;
import tc.oc.pgm.features.SelfIdentifyingFeatureDefinition;
import tc.oc.pgm.util.math.LocalFrame;
import tc.oc.pgm.util.math.LocalRef;
import tc.oc.pgm.variables.LocalScope;

/** A reusable action body with parameters, private locals and an optional return value. */
@FeatureInfo(name = "function")
public class FunctionDefinition<S> extends SelfIdentifyingFeatureDefinition {
  private final Class<S> scope;
  private final ImmutableMap<String, LocalRef> params;
  private final LocalScope locals;
  private final Action<S> body;
  private final boolean usesControl;
  private final boolean returnsValue;
  private final double defaultValue;

  public FunctionDefinition(
      String id,
      Class<S> scope,
      ImmutableMap<String, LocalRef> params,
      LocalScope locals,
      Action<S> body,
      boolean usesControl,
      boolean returnsValue,
      double defaultValue) {
    super(id);
    this.scope = scope;
    this.params = params;
    this.locals = locals;
    this.body = body;
    this.usesControl = usesControl;
    this.returnsValue = returnsValue;
    this.defaultValue = defaultValue;
  }

  public Class<S> getScope() {
    return scope;
  }

  public ImmutableMap<String, LocalRef> getParams() {
    return params;
  }

  public Action<S> getBody() {
    return body;
  }

  public boolean usesControl() {
    return usesControl;
  }

  public boolean returnsValue() {
    return returnsValue;
  }

  public double getDefaultValue() {
    return defaultValue;
  }

  public LocalFrame createFrame() {
    return locals.createFrame(null);
  }
}
