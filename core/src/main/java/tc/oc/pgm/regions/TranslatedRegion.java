package tc.oc.pgm.regions;

import org.bukkit.util.Vector;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.region.Region;

/** Region adaptor that applies a translation. */
public class TranslatedRegion extends TransformedRegion {
  private final Vector offset;

  public TranslatedRegion(Region region, Vector offset) {
    super(region);
    this.offset = offset;
  }

  public static TranslatedRegion translate(Region region, Vector offset) {
    return new TranslatedRegion(region, offset);
  }

  @Override
  protected Vector transform(Vector point) {
    return point.clone().add(this.offset);
  }

  @Override
  protected Vector untransform(Vector point) {
    return point.clone().subtract(this.offset);
  }

  @Override
  protected Bounds getTransformedBounds() {
    return this.region.getBounds().translate(this.offset);
  }

  @Override
  public Region.Static getStaticImpl(Match match) {
    return new TranslatedRegion(region.getStatic(match), offset);
  }
}
