package tc.oc.pgm.filters.parse;

import tc.oc.pgm.api.feature.FeatureValidation;
import tc.oc.pgm.api.filter.FilterDefinition;
import tc.oc.pgm.api.filter.Filterables;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.filters.Filterable;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;

public class DynamicFilterValidation implements FeatureValidation<FilterDefinition> {
  public static final DynamicFilterValidation ANY = new DynamicFilterValidation(null);
  public static final DynamicFilterValidation PLAYER =
      new DynamicFilterValidation(MatchPlayer.class);
  public static final DynamicFilterValidation PARTY = new DynamicFilterValidation(Party.class);
  public static final DynamicFilterValidation MATCH = new DynamicFilterValidation(Match.class);

  private final Class<? extends Filterable<?>> type;

  private DynamicFilterValidation(Class<? extends Filterable<?>> type) {
    this.type = type;
  }

  public static DynamicFilterValidation of(Class<? extends Filterable<?>> type) {
    if (type == null) return ANY;
    if (type == Match.class) return MATCH;
    if (type == Party.class) return PARTY;
    if (type == MatchPlayer.class) return PLAYER;
    return new DynamicFilterValidation(type);
  }

  @Override
  public void validate(FilterDefinition definition, Node node) throws InvalidXMLException {
    if (!definition.isDynamic()) throw new InvalidXMLException("Filter must be dynamic", node);
    if (type != null && !definition.respondsTo(type))
      throw new InvalidXMLException(
          "Expected a filter that can react to changes in "
              + type.getSimpleName()
              + ". The filter "
              + definition.getDefinitionType().getSimpleName()
              + " only responds to "
              + Filterables.scope(definition).getSimpleName()
              + " or lower",
          node);
  }
}
