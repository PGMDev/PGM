package tc.oc.pgm.portals;

import static tc.oc.pgm.util.Assert.assertNotNull;

import java.util.Optional;
import java.util.Random;
import org.bukkit.Location;
import org.bukkit.util.Vector;
import tc.oc.pgm.api.region.Region;

public interface PortalTransform extends InvertibleOperator<PortalTransform> {

  Vector apply(Vector v);

  Location apply(Location v);

  static PortalTransform piecewise(
      DoubleProvider x,
      DoubleProvider y,
      DoubleProvider z,
      DoubleProvider yaw,
      DoubleProvider pitch) {
    if (x == RelativeDoubleProvider.ZERO
        && y == RelativeDoubleProvider.ZERO
        && z == RelativeDoubleProvider.ZERO
        && yaw == RelativeDoubleProvider.ZERO
        && pitch == RelativeDoubleProvider.ZERO) {
      return IDENTITY;
    } else {
      return new Piecewise(x, y, z, yaw, pitch);
    }
  }

  class Piecewise implements PortalTransform {
    private final DoubleProvider x, y, z, yaw, pitch;

    private Piecewise(
        DoubleProvider x,
        DoubleProvider y,
        DoubleProvider z,
        DoubleProvider yaw,
        DoubleProvider pitch) {
      this.x = x;
      this.y = y;
      this.z = z;
      this.yaw = yaw;
      this.pitch = pitch;
    }

    private Vector mutate(Vector v) {
      v.setX(x.apply(v.getX()));
      v.setY(y.apply(v.getY()));
      v.setZ(z.apply(v.getZ()));
      return v;
    }

    private Location mutate(Location v) {
      Vector mutated = mutate(v.toVector());
      v.setX(mutated.getX());
      v.setY(mutated.getY());
      v.setZ(mutated.getZ());
      v.setYaw((float) yaw.apply(v.getYaw()));
      v.setPitch((float) pitch.apply(v.getPitch()));
      return v;
    }

    @Override
    public Vector apply(Vector v) {
      return mutate(v);
    }

    @Override
    public Location apply(Location v) {
      return mutate(v.clone());
    }

    @Override
    public boolean invertible() {
      return x.invertible()
          && y.invertible()
          && z.invertible()
          && yaw.invertible()
          && pitch.invertible();
    }

    @Override
    public PortalTransform inverse() {
      return new Piecewise(x.inverse(), y.inverse(), z.inverse(), yaw.inverse(), pitch.inverse());
    }
  }

  Identity IDENTITY = new Identity();

  class Identity implements PortalTransform {
    private Identity() {}

    @Override
    public Location apply(Location v) {
      return v;
    }

    @Override
    public Vector apply(Vector v) {
      return v;
    }

    @Override
    public boolean invertible() {
      return true;
    }

    @Override
    public PortalTransform inverse() {
      return this;
    }
  }

  static PortalTransform regional(Optional<Region.Static> from, Region.Static to) {
    return new Regional(from, to);
  }

  class Regional implements PortalTransform {
    private final Random random;
    private final Optional<Region.Static> from;
    private final Region.Static to;

    private Regional(Optional<Region.Static> from, Region.Static to) {
      this.from = assertNotNull(from);
      this.to = assertNotNull(to);
      this.random = new Random();
    }

    @Override
    public Vector apply(Vector v) {
      v = new Vector(v.getX(), v.getY(), v.getZ());
      return v.copy(to.getRandom(random));
    }

    @Override
    public Location apply(Location v) {
      v = v.clone();
      Vector region = to.getRandom(random);
      v.setX(region.getX());
      v.setY(region.getY());
      v.setZ(region.getZ());
      return v;
    }

    @Override
    public boolean invertible() {
      return from.isPresent();
    }

    @Override
    public PortalTransform inverse() {
      from.orElseThrow(() -> new IllegalStateException("not invertible"));
      return new Regional(Optional.of(to), from.get());
    }
  }

  static PortalTransform concatenate(PortalTransform first, PortalTransform last) {
    if (first instanceof Identity) {
      return last;
    } else if (last instanceof Identity) {
      return first;
    } else {
      return new Concatenate(first, last);
    }
  }

  class Concatenate implements PortalTransform {
    private final PortalTransform first, last;

    private Concatenate(PortalTransform first, PortalTransform last) {
      this.first = assertNotNull(first);
      this.last = assertNotNull(last);
    }

    @Override
    public Vector apply(Vector v) {
      return last.apply(first.apply(v));
    }

    @Override
    public Location apply(Location v) {
      return last.apply(first.apply(v));
    }

    @Override
    public boolean invertible() {
      return first.invertible() && last.invertible();
    }

    @Override
    public PortalTransform inverse() {
      return new Concatenate(last.inverse(), first.inverse());
    }
  }
}
