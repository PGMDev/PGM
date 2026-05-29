package tc.oc.pgm.variables.types;

import java.util.function.ObjDoubleConsumer;
import java.util.function.ToDoubleFunction;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.regions.PointRegion;
import tc.oc.pgm.variables.Variable;

public class PointVariable extends RegionVariable<PointRegion.Mutable, PointRegion> {

  public PointVariable(PointRegion initial) {
    super(Component.values(), initial);
  }

  public Variable<Match> getComponent(Component component) {
    return super.getComponent(component);
  }

  public enum Component implements tc.oc.pgm.regions.Component<PointRegion.Mutable> {
    X(r -> r.getMutablePosition().getX(), (r, v) -> r.getMutablePosition().setX(v)),
    Y(r -> r.getMutablePosition().getY(), (r, v) -> r.getMutablePosition().setY(v)),
    Z(r -> r.getMutablePosition().getZ(), (r, v) -> r.getMutablePosition().setZ(v));

    private final ToDoubleFunction<PointRegion.Mutable> getter;
    private final ObjDoubleConsumer<PointRegion.Mutable> setter;

    Component(
        ToDoubleFunction<PointRegion.Mutable> getter,
        ObjDoubleConsumer<PointRegion.Mutable> setter) {
      this.getter = getter;
      this.setter = setter;
    }

    @Override
    public ToDoubleFunction<PointRegion.Mutable> getter() {
      return getter;
    }

    @Override
    public ObjDoubleConsumer<PointRegion.Mutable> setter() {
      return setter;
    }
  }
}
