package tc.oc.pgm.regions;

import java.util.function.Function;
import java.util.function.Predicate;
import tc.oc.pgm.api.feature.FeatureValidation;
import tc.oc.pgm.api.region.RegionDefinition;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;

/** Constraints a {@link RegionDefinition} must satisfy to be usable at a given call site. */
public enum RegionValidation implements FeatureValidation<RegionDefinition> {
  BLOCK_BOUNDED(RegionDefinition::isBlockBounded, def -> "Cannot enumerate blocks in region"),
  STATIC(RegionDefinition::isStatic, def -> "Cannot use non-static region here"),
  RANDOM_POINTS(
      RegionDefinition::canGetRandom,
      def -> "Cannot generate random points in region type " + def.getClass().getSimpleName());

  private final Predicate<RegionDefinition> valid;
  private final Function<RegionDefinition, String> message;

  RegionValidation(Predicate<RegionDefinition> valid, Function<RegionDefinition, String> message) {
    this.valid = valid;
    this.message = message;
  }

  @Override
  public void validate(RegionDefinition definition, Node node) throws InvalidXMLException {
    if (!this.valid.test(definition)) {
      throw new InvalidXMLException(this.message.apply(definition), node);
    }
  }

  /** For call sites that must reject a region before it is known to be a definition. */
  public static InvalidXMLException notStatic(Node node) {
    return new InvalidXMLException("Cannot use non-static region here", node);
  }
}
