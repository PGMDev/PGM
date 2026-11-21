package tc.oc.pgm.variables.types;

import java.util.function.ObjDoubleConsumer;
import java.util.function.ToDoubleFunction;
import tc.oc.pgm.regions.Component;
import tc.oc.pgm.regions.SphereRegion;

public class SphereVariable extends RegionVariable<SphereRegion.Mutable, SphereRegion> {

  private static final Component<SphereRegion.Mutable>[] COMPONENTS =
      SphereVariable.SphereComponent.values();

  public SphereVariable(SphereRegion initial) {
    super(COMPONENTS, initial);
  }

  public enum SphereComponent implements Component<SphereRegion.Mutable> {
    X(r -> r.getMutableOrigin().getX(), (r, v) -> r.getMutableOrigin().setX(v)),
    Y(r -> r.getMutableOrigin().getY(), (r, v) -> r.getMutableOrigin().setY(v)),
    Z(r -> r.getMutableOrigin().getZ(), (r, v) -> r.getMutableOrigin().setZ(v)),
    RADIUS(SphereRegion.Mutable::getRadius, SphereRegion.Mutable::setRadius);

    private final ToDoubleFunction<SphereRegion.Mutable> getter;
    private final ObjDoubleConsumer<SphereRegion.Mutable> setter;

    SphereComponent(
        ToDoubleFunction<SphereRegion.Mutable> getter,
        ObjDoubleConsumer<SphereRegion.Mutable> setter) {
      this.getter = getter;
      this.setter = setter;
    }

    @Override
    public ToDoubleFunction<SphereRegion.Mutable> getter() {
      return getter;
    }

    @Override
    public ObjDoubleConsumer<SphereRegion.Mutable> setter() {
      return setter;
    }
  }
}
