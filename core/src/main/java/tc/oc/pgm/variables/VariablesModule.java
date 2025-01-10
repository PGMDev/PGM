package tc.oc.pgm.variables;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import net.objecthunter.exp4j.ExpressionContext;
import net.objecthunter.exp4j.function.Function;
import org.jdom2.Document;
import org.jdom2.Element;
import tc.oc.pgm.api.feature.FeatureDefinition;
import tc.oc.pgm.api.filter.Filterables;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.MapProtos;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.map.factory.MapModuleFactory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.module.exception.ModuleLoadException;
import tc.oc.pgm.features.FeatureDefinitionContext;
import tc.oc.pgm.filters.Filterable;
import tc.oc.pgm.util.math.Formula;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.XMLUtils;
import tc.oc.pgm.variables.types.LivesVariable;
import tc.oc.pgm.variables.types.MaxBuildVariable;
import tc.oc.pgm.variables.types.PlayerLocationVariable;
import tc.oc.pgm.variables.types.ScoreVariable;
import tc.oc.pgm.variables.types.TimeLimitVariable;

public class VariablesModule implements MapModule<VariablesMatchModule> {

  private final FeatureDefinitionContext context;
  private final ImmutableMap<Class<? extends Filterable<?>>, Context<?>> variablesByScope;

  public VariablesModule(FeatureDefinitionContext context) {
    this.context = context;

    ImmutableMap.Builder<Class<? extends Filterable<?>>, Context<?>> varsBuilder =
        ImmutableMap.builder();
    for (Class<? extends Filterable<?>> scope : Filterables.SCOPES) {
      varsBuilder.put(scope, Context.of(scope, context));
    }

    this.variablesByScope = varsBuilder.build();
  }

  @SuppressWarnings("unchecked")
  public <T extends Filterable<?>> Formula.ContextFactory<T> getContext(Class<T> scope) {
    return (Formula.ContextFactory<T>) variablesByScope.get(scope);
  }

  private record Context<T extends Filterable<?>>(
      ImmutableSet<String> variables, ImmutableSet<String> arrays, Map<String, Variable<?>> vars)
      implements Formula.ContextFactory<T> {

    public static <T extends Filterable<?>> Context<T> of(
        Class<T> scope, FeatureDefinitionContext context) {
      ImmutableSet.Builder<String> variableNames = ImmutableSet.builder();
      ImmutableSet.Builder<String> arrayNames = ImmutableSet.builder();
      ImmutableMap.Builder<String, Variable<?>> variableMap = ImmutableMap.builder();

      for (Map.Entry<String, FeatureDefinition> definition : context) {
        if (definition.getValue() instanceof Variable<?> variable) {
          if (!Filterables.isAssignable(scope, variable.getScope())) continue;

          (variable.isIndexed() ? arrayNames : variableNames).add(definition.getKey());
          variableMap.put(definition.getKey(), variable);
        }
      }
      return new Context<>(variableNames.build(), arrayNames.build(), variableMap.build());
    }

    @Override
    public Set<String> getVariables() {
      return variables;
    }

    @Override
    public Set<String> getArrays() {
      return arrays;
    }

    @Override
    public ExpressionContext withContext(T scope) {
      Map<String, Double> variableCache = new HashMap<>();
      Map<String, Function> arrayCache = new HashMap<>();

      return new ExpressionContext() {
        @Override
        public Set<String> getVariables() {
          return variables;
        }

        @Override
        public Double getVariable(String id) {
          return variableCache.computeIfAbsent(id, key -> vars.get(key).getValue(scope));
        }

        @Override
        public Set<String> getFunctions() {
          return arrays;
        }

        @Override
        public Function getFunction(String id) {
          return arrayCache.computeIfAbsent(id, key -> {
            Variable.Indexed<?> def = (Variable.Indexed<?>) vars.get(key);
            if (def == null) return null;

            return new Function(id, 1) {
              @Override
              public double apply(double... doubles) {
                return def.getValue(scope, (int) doubles[0]);
              }
            };
          });
        }
      };
    }
  }

  @Override
  public VariablesMatchModule createMatchModule(Match match) throws ModuleLoadException {
    return new VariablesMatchModule(match, context);
  }

  public static class Factory implements MapModuleFactory<VariablesModule> {

    @Override
    public VariablesModule parse(MapFactory factory, Logger logger, Document doc)
        throws InvalidXMLException {

      boolean featureIds = factory.getProto().isNoOlderThan(MapProtos.FEATURE_SINGLETON_IDS);
      VariableParser parser = new VariableParser(factory);

      var features = factory.getFeatures();
      if (featureIds) {
        features.addFeature(null, "lives", LivesVariable.INSTANCE);
        features.addFeature(null, "score", ScoreVariable.INSTANCE);
        features.addFeature(null, "timelimit", TimeLimitVariable.INSTANCE);
        features.addFeature(null, "maxbuildheight", MaxBuildVariable.INSTANCE);
        for (var entry : PlayerLocationVariable.INSTANCES.entrySet()) {
          String key = "player." + entry.getKey().name().toLowerCase(Locale.ROOT);
          features.addFeature(null, key, entry.getValue());
        }
      }

      for (Element variable : XMLUtils.flattenElements(doc.getRootElement(), "variables", null)) {
        features.addFeature(variable, parser.parse(variable));
      }

      return new VariablesModule(factory.getFeatures());
    }
  }
}
