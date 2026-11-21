package tc.oc.pgm.variables.types;

import java.util.function.ObjDoubleConsumer;
import java.util.function.ToDoubleFunction;
import tc.oc.pgm.regions.Component;
import tc.oc.pgm.regions.CylindricalRegion;

public class CylindricalVariable
    extends RegionVariable<CylindricalRegion.Mutable, CylindricalRegion> {

  private static final Component<CylindricalRegion.Mutable>[] COMPONENTS =
      CylindricalVariable.CylindricalComponent.values();

  public CylindricalVariable(CylindricalRegion initial) {
    super(COMPONENTS, initial);
  }

  public enum CylindricalComponent implements Component<CylindricalRegion.Mutable> {
    X(r -> r.getMutableBase().getX(), (r, v) -> r.getMutableBase().setX(v)),
    Y(r -> r.getMutableBase().getY(), (r, v) -> r.getMutableBase().setY(v)),
    Z(r -> r.getMutableBase().getZ(), (r, v) -> r.getMutableBase().setZ(v)),
    RADIUS(CylindricalRegion.Mutable::getRadius, CylindricalRegion.Mutable::setRadius),
    HEIGHT(CylindricalRegion.Mutable::getHeight, CylindricalRegion.Mutable::setHeight);

    private final ToDoubleFunction<CylindricalRegion.Mutable> getter;
    private final ObjDoubleConsumer<CylindricalRegion.Mutable> setter;

    CylindricalComponent(
        ToDoubleFunction<CylindricalRegion.Mutable> getter,
        ObjDoubleConsumer<CylindricalRegion.Mutable> setter) {
      this.getter = getter;
      this.setter = setter;
    }

    @Override
    public ToDoubleFunction<CylindricalRegion.Mutable> getter() {
      return getter;
    }

    @Override
    public ObjDoubleConsumer<CylindricalRegion.Mutable> setter() {
      return setter;
    }
  }
}
