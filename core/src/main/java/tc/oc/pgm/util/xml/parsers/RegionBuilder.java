package tc.oc.pgm.util.xml.parsers;

import org.jdom2.Element;
import tc.oc.pgm.api.region.Region;
import tc.oc.pgm.regions.BlockBoundedValidation;
import tc.oc.pgm.regions.RandomPointsValidation;
import tc.oc.pgm.regions.RegionParser;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;

public class RegionBuilder extends Builder<Region, RegionBuilder> {
  private final RegionParser regions;

  public RegionBuilder(RegionParser regions, Element el, String... prop) {
    super(el, prop);
    this.regions = regions;
  }

  public RegionBuilder blockBounded() {
    validate((r, n) -> regions.validate(r, BlockBoundedValidation.INSTANCE, n));
    return this;
  }

  public RegionBuilder randomPoints() {
    validate((r, n) -> regions.validate(r, RandomPointsValidation.INSTANCE, n));
    return this;
  }

  @Override
  protected Region parse(Node node) throws InvalidXMLException {
    if (prop.length == 0) return regions.parse(el);
    return regions.parseProperty(node);
  }

  @Override
  protected RegionBuilder getThis() {
    return this;
  }
}
