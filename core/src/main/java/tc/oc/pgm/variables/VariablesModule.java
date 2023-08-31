package tc.oc.pgm.variables;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterators;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
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
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.XMLUtils;

public class VariablesModule implements MapModule<VariablesMatchModule> {

  private final ImmutableList<VariableDefinition<?>> variables;
  private final ImmutableMap<Class<? extends Filterable<?>>, VariableCache<?>> variablesByScope;

  public VariablesModule(ImmutableList<VariableDefinition<?>> variables) {
    this.variables = variables;

    ImmutableMap.Builder<Class<? extends Filterable<?>>, VariableCache<?>> varsBuilder =
        ImmutableMap.builder();
    for (Class<? extends Filterable<?>> scope : Filterables.SCOPES) {
      varsBuilder.put(scope, VariableCache.of(scope, variables));
    }

    this.variablesByScope = varsBuilder.build();
  }

  @Override
  public @Nullable Collection<Class<? extends MatchModule>> getWeakDependencies() {
    return ImmutableList.of(TeamMatchModule.class, BlitzMatchModule.class, ScoreMatchModule.class);
  }

  public ImmutableSet<String> getVariableNames(Class<? extends Filterable<?>> scope) {
    return variablesByScope.get(scope).names;
  }

  @SuppressWarnings("unchecked")
  public <T extends Filterable<?>> VariableContextBuilder<T> getContextBuilder(Class<T> scope) {
    return (VariableContextBuilder<T>) variablesByScope.get(scope).context;
  }

  private static class VariableCache<T extends Filterable<?>> {
    private final ImmutableSet<String> names;
    private final VariableContextBuilder<T> context;

    public VariableCache(ImmutableSet<String> names, VariableContextBuilder<T> context) {
      this.names = names;
      this.context = context;
    }

    public static <T extends Filterable<?>> VariableCache<T> of(
        Class<T> scope, ImmutableList<VariableDefinition<?>> variables) {
      List<VariableDefinition<?>> vars =
          variables.stream()
              .filter(v -> Filterables.isAssignable(scope, v.getScope()))
              .collect(Collectors.toList());

      return new VariableCache<T>(
          ImmutableSet.copyOf(Iterators.transform(vars.iterator(), VariableDefinition::getId)),
          new VariableContextBuilder<>(ImmutableList.copyOf(vars)));
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
