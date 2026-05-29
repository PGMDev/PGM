package tc.oc.pgm.variables.types;

import java.util.function.ObjDoubleConsumer;
import java.util.function.ToDoubleFunction;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.regions.SphereRegion;
import tc.oc.pgm.variables.Variable;

public class SphereVariable extends RegionVariable<SphereRegion.Mutable, SphereRegion> {

  public SphereVariable(SphereRegion initial) {
    super(Component.values(), initial);
  }

  public Variable<Match> getComponent(Component component) {
    return super.getComponent(component);
  }

  public enum Component implements tc.oc.pgm.regions.Component<SphereRegion.Mutable> {
    X(r -> r.getMutableOrigin().getX(), (r, v) -> r.getMutableOrigin().setX(v)),
    Y(r -> r.getMutableOrigin().getY(), (r, v) -> r.getMutableOrigin().setY(v)),
    Z(r -> r.getMutableOrigin().getZ(), (r, v) -> r.getMutableOrigin().setZ(v)),
    RADIUS(SphereRegion.Mutable::getRadius, SphereRegion.Mutable::setRadius);

    private final ToDoubleFunction<SphereRegion.Mutable> getter;
    private final ObjDoubleConsumer<SphereRegion.Mutable> setter;

    Component(
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
