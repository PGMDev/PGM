package tc.oc.pgm.variables.types;

import java.util.function.ObjDoubleConsumer;
import java.util.function.ToDoubleFunction;
import tc.oc.pgm.regions.BlockRegion;

public class BlockVariable extends RegionVariable<BlockRegion.Mutable, BlockRegion> {

  public BlockVariable(BlockRegion initial) {
    super(Component.values(), initial);
  }

  public enum Component implements tc.oc.pgm.regions.Component<BlockRegion.Mutable> {
    X(r -> r.getMutableLocation().getBlockX(), (r, v) -> r.getMutableLocation().setX(v)),
    Y(r -> r.getMutableLocation().getBlockY(), (r, v) -> r.getMutableLocation().setY(v)),
    Z(r -> r.getMutableLocation().getBlockZ(), (r, v) -> r.getMutableLocation().setZ(v));

    private final ToDoubleFunction<BlockRegion.Mutable> getter;
    private final ObjDoubleConsumer<BlockRegion.Mutable> setter;

    Component(
        ToDoubleFunction<BlockRegion.Mutable> getter,
        ObjDoubleConsumer<BlockRegion.Mutable> setter) {
      this.getter = getter;
      this.setter = setter;
    }

    @Override
    public ToDoubleFunction<BlockRegion.Mutable> getter() {
      return getter;
    }

    @Override
    public ObjDoubleConsumer<BlockRegion.Mutable> setter() {
      return setter;
    }
  }
}
