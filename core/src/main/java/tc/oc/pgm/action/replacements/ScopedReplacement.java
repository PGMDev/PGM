package tc.oc.pgm.action.replacements;

import java.util.function.BiFunction;
import net.kyori.adventure.text.ComponentLike;
import org.jspecify.annotations.Nullable;
import tc.oc.pgm.api.filter.Filterables;
import tc.oc.pgm.filters.Filterable;
import tc.oc.pgm.util.math.LocalFrame;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;

public abstract class ScopedReplacement<S extends Filterable<?>> implements Replacement {
  private final Class<S> scope;

  public ScopedReplacement(Class<S> scope) {
    this.scope = scope;
  }

  @Override
  public void validate(Class<? extends Filterable<?>> filterable, Node node)
      throws InvalidXMLException {
    if (!Filterables.isAssignable(filterable, scope)) {
      throw new InvalidXMLException(
          "Wrong replacement scope, got " + filterable.getSimpleName() + " but expected "
              + scope.getSimpleName() + " or lower",
          node);
    }
  }

  protected abstract ComponentLike getImpl(S ctx, @Nullable LocalFrame locals);

  @Override
  public ComponentLike get(Filterable<?> filterable, @Nullable LocalFrame locals) {
    S ctx = filterable.getFilterableAncestor(scope);
    if (ctx == null)
      throw new IllegalStateException("Wrong replacement scope for '"
          + this
          + "', expected "
          + scope.getSimpleName()
          + " which cannot be found in "
          + filterable.getClass().getSimpleName());

    return getImpl(ctx, locals);
  }

  public static <S extends Filterable<?>> ScopedReplacement<S> of(
      Class<S> scope, BiFunction<S, @Nullable LocalFrame, ComponentLike> get) {
    return new ScopedReplacement<>(scope) {
      @Override
      protected ComponentLike getImpl(S ctx, @Nullable LocalFrame locals) {
        return get.apply(ctx, locals);
      }
    };
  }
}
