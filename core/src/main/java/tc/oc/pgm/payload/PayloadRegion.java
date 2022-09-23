package tc.oc.pgm.payload;

import org.bukkit.util.Vector;
import tc.oc.pgm.regions.AbstractRegion;
import tc.oc.pgm.regions.Bounds;

public class PayloadRegion extends AbstractRegion {

  private final Payload payload;
  private final double distanceSquared;

  public PayloadRegion(Payload payload) {
    this.payload = payload;
    this.distanceSquared = Math.pow(payload.getDefinition().getRadius(), 2);
  }

  @Override
  public boolean contains(Vector point) {
    return payload.getCurrentPosition().distanceSquared(point) <= distanceSquared;
  }

  @Override
  public Bounds getBounds() {
    return Bounds.unbounded();
  }
}
