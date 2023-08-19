package tc.oc.pgm.variables;

import java.util.function.Function;
import org.jdom2.Element;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.filters.Filterable;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.variables.types.BlitzVariable;
import tc.oc.pgm.variables.types.DummyVariable;
import tc.oc.pgm.variables.types.ScoreVariable;
import tc.oc.pgm.variables.types.TeamVariable;

public enum VariableType {
  DUMMY(
      DummyVariable::new,
      (Class<? extends Filterable<?>>) null,
      MatchPlayer.class,
      Party.class,
      Match.class),
  LIVES(BlitzVariable::new, MatchPlayer.class),
  SCORE(ScoreVariable::new, Party.class),
  TEAM(TeamVariable::new, TeamVariable.Context::new, Match.class);

  private final Function<VariableDefinition<?>, Variable<?>> builder;
  private final ContextBuilder<?> contextBuilder;

  private final Class<? extends Filterable<?>> defaultScope;
  private final Class<?>[] supportedScopes;

  @SafeVarargs
  VariableType(
      Function<VariableDefinition<?>, Variable<?>> builder,
      Class<? extends Filterable<?>> defaultScope,
      Class<? extends Filterable<?>>... supportedScopes) {
    this.builder = builder;
    this.contextBuilder = null;
    this.defaultScope = defaultScope;
    this.supportedScopes = supportedScopes;
  }

  @SafeVarargs
  VariableType(
      Function<VariableDefinition<?>, Variable<?>> builder,
      ContextBuilder<?> contextBuilder,
      Class<? extends Filterable<?>> defaultScope,
      Class<? extends Filterable<?>>... supportedScopes) {
    this.builder = builder;
    this.contextBuilder = contextBuilder;
    this.defaultScope = defaultScope;
    this.supportedScopes = supportedScopes;
  }

  public Class<? extends Filterable<?>> getDefaultScope() {
    return defaultScope;
  }

  public boolean supports(Class<? extends Filterable<?>> cls) {
    if (defaultScope == cls) return true;
    for (Class<?> supportedScope : supportedScopes) {
      if (supportedScope.isAssignableFrom(cls)) {
        return true;
      }
    }
    return false;
  }

  public Object build(MapFactory factory, Element el) throws InvalidXMLException {
    return contextBuilder == null ? null : contextBuilder.build(factory, el);
  }

  public Variable<?> buildInstance(VariableDefinition<?> definition) {
    return builder.apply(definition);
  }

  private interface ContextBuilder<T> {
    T build(MapFactory factory, Element el) throws InvalidXMLException;
  }
}
