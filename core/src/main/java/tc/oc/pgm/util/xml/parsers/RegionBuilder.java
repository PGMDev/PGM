package tc.oc.pgm.util.xml.parsers;

import org.jdom2.Element;
import org.jetbrains.annotations.NotNull;
import tc.oc.pgm.api.map.MapProtos;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.region.Region;
import tc.oc.pgm.regions.BlockBoundedValidation;
import tc.oc.pgm.regions.EmptyRegion;
import tc.oc.pgm.regions.EverywhereRegion;
import tc.oc.pgm.regions.RandomPointsValidation;
import tc.oc.pgm.regions.RegionParser;
import tc.oc.pgm.regions.StaticValidation;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;

public abstract sealed class RegionBuilder<T extends Region> extends Builder<T, RegionBuilder<T>>
    permits RegionBuilder.OfRegion, RegionBuilder.OfStatic {
  protected final RegionParser regions;
  private boolean children = false;

  private RegionBuilder(RegionParser regions, @NotNull Element el, String... prop) {
    super(el, prop);
    this.regions = regions;
  }

  /** Use children parsing for legacy xml versions only * */
  public RegionBuilder<T> legacy(MapFactory context) {
    if (!context.getProto().isOlderThan(MapProtos.MODULE_SUBELEMENT_VERSION)) return this;
    this.prop = new String[] {};
    this.attr = false;
    this.child = false;
    this.self = true;
    this.children = true;
    return this;
  }

  public RegionBuilder<T> blockBounded() {
    validate((r, n) -> regions.validate(r, BlockBoundedValidation.INSTANCE, n));
    return this;
  }

  public RegionBuilder<T> randomPoints() {
    validate((r, n) -> regions.validate(r, RandomPointsValidation.INSTANCE, n));
    return this;
  }

  @SuppressWarnings("unchecked")
  public T orEverywhere() throws InvalidXMLException {
    return optional((T) EverywhereRegion.INSTANCE);
  }

  @SuppressWarnings("unchecked")
  public T orNowhere() throws InvalidXMLException {
    return optional((T) EmptyRegion.INSTANCE);
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

  public static final class OfRegion extends RegionBuilder<Region> {
    public OfRegion(RegionParser regions, Element el, String... prop) {
      super(regions, el, prop);
    }

    @Override
    protected Region parse(Node node) throws InvalidXMLException {
      return parseRegion(node);
    }
  }

  public static final class OfStatic extends RegionBuilder<Region.Static> {

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
