package tc.oc.pgm.util.xml.parsers;

import org.jdom2.Element;
import org.jetbrains.annotations.NotNull;
import tc.oc.pgm.api.region.Region;
import tc.oc.pgm.regions.BlockBoundedValidation;
import tc.oc.pgm.regions.RandomPointsValidation;
import tc.oc.pgm.regions.RegionParser;
import tc.oc.pgm.regions.StaticValidation;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;

public abstract class RegionBuilder<T extends Region> extends Builder<T, RegionBuilder<T>> {
  protected final RegionParser regions;
  private boolean children = false;

  private RegionBuilder(RegionParser regions, @NotNull Element el, String... prop) {
    super(el, prop);
    this.regions = regions;
  }

  public RegionBuilder<T> blockBounded() {
    validate((r, n) -> regions.validate(r, BlockBoundedValidation.INSTANCE, n));
    return this;
  }

  public RegionBuilder<T> randomPoints() {
    validate((r, n) -> regions.validate(r, RandomPointsValidation.INSTANCE, n));
    return this;
  }

  /** Sets up to parse for a union of all inner regions, legacy only! */
  public RegionBuilder<T> children() {
    this.children = true;
    return this;
  }

  protected Region parseRegion(Node node) throws InvalidXMLException {
    if (children && node.isElement()) {
      return regions.parseChildren(node.getElement());
    }
    if (prop.length == 0) return regions.parse(el);
    return regions.parseProperty(node);
  }

  @Override
  protected RegionBuilder<T> getThis() {
    return this;
  }

  public static class OfRegion extends RegionBuilder<Region> {
    public OfRegion(RegionParser regions, Element el, String... prop) {
      super(regions, el, prop);
    }

    @Override
    protected Region parse(Node node) throws InvalidXMLException {
      return parseRegion(node);
    }
  }

  public static class OfStatic extends RegionBuilder<Region.Static> {

    public OfStatic(RegionParser regions, Element el, String... prop) {
      super(regions, el, prop);
      validate((r, n) -> regions.validate(r, StaticValidation.INSTANCE, n));
    }

    @Override
    protected Region.Static parse(Node node) throws InvalidXMLException {
      Region region = parseRegion(node);
      if (!(region instanceof Region.Static s)) throw StaticValidation.makeException(node);
      return s;
    }
  }
}
