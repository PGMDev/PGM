package tc.oc.pgm.portals;

import org.bukkit.util.Vector;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.region.Region;
import tc.oc.pgm.regions.TransformedRegion;

public class PortalExitRegion extends TransformedRegion {

  private final PortalTransform portalTransform;
  private final PortalTransform inverseTransform;

  public PortalExitRegion(Region entranceRegion, PortalTransform portalTransform) {
    super(entranceRegion);
    this.portalTransform = portalTransform;
    this.inverseTransform = portalTransform.inverse();
  }

  @Override
  protected Vector transform(Vector point) {
    return portalTransform.apply(point);
  }

  @Override
  protected Vector untransform(Vector point) {
    return inverseTransform.apply(point);
  }

  @Override
  public Region.Static getStaticImpl(Match match) {
    return new PortalExitRegion(region.getStatic(match), portalTransform);
  }
}
