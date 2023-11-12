package tc.oc.pgm.variables;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import net.objecthunter.exp4j.ExpressionContext;
import net.objecthunter.exp4j.function.Function;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.filter.Filterables;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.map.factory.MapModuleFactory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.module.exception.ModuleLoadException;
import tc.oc.pgm.blitz.BlitzMatchModule;
import tc.oc.pgm.filters.Filterable;
import tc.oc.pgm.score.ScoreMatchModule;
import tc.oc.pgm.teams.TeamMatchModule;
import tc.oc.pgm.util.math.Formula;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.XMLUtils;
import tc.oc.pgm.variables.types.IndexedVariable;

public class VariablesModule implements MapModule<VariablesMatchModule> {

  private final ImmutableList<VariableDefinition<?>> variables;
  private final ImmutableMap<Class<? extends Filterable<?>>, Context<?>> variablesByScope;

  public VariablesModule(ImmutableList<VariableDefinition<?>> variables) {
    this.variables = variables;

    ImmutableMap.Builder<Class<? extends Filterable<?>>, Context<?>> varsBuilder =
        ImmutableMap.builder();
    for (Class<? extends Filterable<?>> scope : Filterables.SCOPES) {
      varsBuilder.put(scope, Context.of(scope, variables));
    }

    this.variablesByScope = varsBuilder.build();
  }

  @Override
  public @Nullable Collection<Class<? extends MatchModule>> getWeakDependencies() {
    return ImmutableList.of(TeamMatchModule.class, BlitzMatchModule.class, ScoreMatchModule.class);
  }

  @SuppressWarnings("unchecked")
  public <T extends Filterable<?>> Formula.ContextFactory<T> getContext(Class<T> scope) {
    return (Formula.ContextFactory<T>) variablesByScope.get(scope);
  }

  private static class Context<T extends Filterable<?>> implements Formula.ContextFactory<T> {
    private final ImmutableSet<String> variables;
    private final ImmutableSet<String> arrays;
    private final Map<String, VariableDefinition<?>> vars;

    public Context(
        ImmutableSet<String> variables,
        ImmutableSet<String> arrays,
        Map<String, VariableDefinition<?>> vars) {
      this.variables = variables;
      this.arrays = arrays;
      this.vars = vars;
    }

    public static <T extends Filterable<?>> Context<T> of(
        Class<T> scope, List<VariableDefinition<?>> variables) {
      ImmutableSet.Builder<String> variableNames = ImmutableSet.builder();
      ImmutableSet.Builder<String> arrayNames = ImmutableSet.builder();
      ImmutableMap.Builder<String, VariableDefinition<?>> variableMap = ImmutableMap.builder();

      for (VariableDefinition<?> variable : variables) {
        if (!Filterables.isAssignable(scope, variable.getScope())) continue;

        (variable.isIndexed() ? arrayNames : variableNames).add(variable.getId());
        variableMap.put(variable.getId(), variable);
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
      Match match = scope.getMatch();
      Map<String, Double> variableCache = new HashMap<>();
      Map<String, Function> arrayCache = new HashMap<>();

      return new ExpressionContext() {
        @Override
        public Set<String> getVariables() {
          return variables;
        }

        @Override
        public Double getVariable(String id) {
          return variableCache.computeIfAbsent(
              id, key -> vars.get(key).getVariable(match).getValue(scope));
        }

        @Override
        public Set<String> getFunctions() {
          return arrays;
        }

        @Override
        public Function getFunction(String id) {
          return arrayCache.computeIfAbsent(
              id,
              key -> {
                VariableDefinition<?> def = vars.get(key);
                if (def == null) return null;

                IndexedVariable<?> variable = (IndexedVariable<?>) def.getVariable(match);
                return new Function(id, 1) {
                  @Override
                  public double apply(double... doubles) {
                    return variable.getValue(scope, (int) doubles[0]);
                  }
                };
              });
        }
      };
    }
  }

  @Nullable
  @Override
  public VariablesMatchModule createMatchModule(Match match) throws ModuleLoadException {
    for (VariableDefinition<?> varDef : this.variables) {
      match.getFeatureContext().add(varDef.buildInstance());
    }

    return new VariablesMatchModule(match);
  }

  public static class Factory implements MapModuleFactory<VariablesModule> {

    // The limitation is due to them being used in exp4j formulas for.
    public static final Pattern VARIABLE_ID = Pattern.compile("[A-Za-z_]\\w*");

    @Nullable
    @Override
    public VariablesModule parse(MapFactory factory, Logger logger, Document doc)
        throws InvalidXMLException {

      ImmutableList.Builder<VariableDefinition<?>> variables = ImmutableList.builder();
      VariableParser parser = new VariableParser(factory);

      for (Element variable : XMLUtils.flattenElements(doc.getRootElement(), "variables", null)) {
        VariableDefinition<?> varDef = parser.parse(variable);
        factory.getFeatures().addFeature(variable, varDef);
        variables.add(varDef);
      }

      return new VariablesModule(variables.build());
    }
  }
}
