package tc.oc.pgm.variables.types;

import java.util.function.ObjDoubleConsumer;
import java.util.function.ToDoubleFunction;
import tc.oc.pgm.regions.Component;
import tc.oc.pgm.regions.CuboidRegion;

public class CuboidVariable extends RegionVariable<CuboidRegion.Mutable, CuboidRegion> {

  private static final Component<CuboidRegion.Mutable>[] COMPONENTS = CuboidComponent.values();

  public CuboidVariable(CuboidRegion initial) {
    super(COMPONENTS, initial);
  }

  public enum CuboidComponent implements Component<CuboidRegion.Mutable> {
    MIN_X(r -> r.getMutableMin().getX(), (r, v) -> r.getMutableMin().setX(v)),
    MIN_Y(r -> r.getMutableMin().getY(), (r, v) -> r.getMutableMin().setY(v)),
    MIN_Z(r -> r.getMutableMin().getZ(), (r, v) -> r.getMutableMin().setZ(v)),
    MAX_X(r -> r.getMutableMax().getX(), (r, v) -> r.getMutableMax().setX(v)),
    MAX_Y(r -> r.getMutableMax().getY(), (r, v) -> r.getMutableMax().setY(v)),
    MAX_Z(r -> r.getMutableMax().getZ(), (r, v) -> r.getMutableMax().setZ(v));

    private final ToDoubleFunction<CuboidRegion.Mutable> getter;
    private final ObjDoubleConsumer<CuboidRegion.Mutable> setter;

    CuboidComponent(
        ToDoubleFunction<CuboidRegion.Mutable> getter,
        ObjDoubleConsumer<CuboidRegion.Mutable> setter) {
      this.getter = getter;
      this.setter = setter;
    }

    @Override
    public ToDoubleFunction<CuboidRegion.Mutable> getter() {
      return getter;
    }

    @Override
    public ObjDoubleConsumer<CuboidRegion.Mutable> setter() {
      return setter;
    }
  }
}
