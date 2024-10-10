package tc.oc.pgm.util.xml.parsers;

import org.jdom2.Element;
import tc.oc.pgm.api.filter.Filterables;
import tc.oc.pgm.features.FeatureDefinitionContext;
import tc.oc.pgm.filters.Filterable;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;
import tc.oc.pgm.variables.Variable;

public class VariableBuilder<T extends Filterable<?>>
    extends Builder<Variable<T>, VariableBuilder<T>> {
  private final FeatureDefinitionContext features;

  public VariableBuilder(FeatureDefinitionContext features, Element el, String... prop) {
    super(el, prop);
    this.features = features;
  }

  public VariableBuilder<T> bound(Class<? extends Filterable<?>> scope) {
    validate((var, n) -> {
      if (!Filterables.isAssignable(scope, var.getScope()))
        throw new InvalidXMLException(
            "Wrong variable scope for '"
                + n.getValue()
                + "', required "
                + scope.getSimpleName()
                + " or higher, but was "
                + var.getScope().getSimpleName(),
            el);
    });
    return this;
  }

  public <S extends Filterable<?>> VariableBuilder<S> scope(Class<S> scope) {
    validate((var, n) -> {
      if (scope != var.getScope())
        throw new InvalidXMLException(
            "Wrong variable scope for '"
                + n.getValue()
                + "', required "
                + scope.getSimpleName()
                + " but variable was "
                + var.getScope().getSimpleName(),
            n);
    });
    return (VariableBuilder<S>) this;
  }

  public VariableBuilder<T> writtable() {
    validate((var, n) -> {
      if (var.isReadonly())
        throw new InvalidXMLException("Variable was readonly when write access is required", n);
    });
    return this;
  }

  public Variable.Exclusive<T> singleExclusive() throws InvalidXMLException {
    validate((var, n) -> {
      if (!var.isExclusive() || ((Variable.Exclusive<T>) var).getCardinality() != 1)
        throw new InvalidXMLException("Variable with exclusive=1 required", n);
    });
    return (Variable.Exclusive<T>) required();
  }

  public Variable.Exclusive<T> exclusive() throws InvalidXMLException {
    validate((var, n) -> {
      if (!var.isExclusive()) throw new InvalidXMLException("Variable with exclusive required", n);
    });
    return (Variable.Exclusive<T>) required();
  }

  @Override
  protected Variable<T> parse(Node node) throws InvalidXMLException {
    return features.resolve(node, Variable.class);
  }

  @Override
  protected VariableBuilder<T> getThis() {
    return this;
  }
}
