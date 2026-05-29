package tc.oc.pgm.variables.types;

import tc.oc.pgm.api.region.RegionDefinition;
import tc.oc.pgm.regions.BlockRegion;
import tc.oc.pgm.regions.Component;

import java.util.function.ObjDoubleConsumer;
import java.util.function.ToDoubleFunction;

public class BlockRegion extends RegionVariable<RegionDefinition.Mutable, tc.oc.pgm.regions.BlockRegion> {

    public BlockVariable(BlockRegion initial) {
        super(Component.values(), initial);
    }

    public Variable<Match> getComponent(Component component) {
        return super.getComponent(component);
    }

    public enum Component implements tc.oc.pgm.regions.Component<RegionDefinition.Mutable> {
        X(r -> r.getMutableX().getX(), (r, v) -> r.getMutableX().setX(v)),
        Y(r -> r.getMutableY().getY(), (r, v) -> r.getMutableY().setY(v)),
        Z(r -> r.getMutableZ().getZ(), (r, v) -> r.getMutableZ().setZ(v));
    }

    private final ToDoubleFunction<RegionDefinition.Mutable> getter;
    private final ObjDoubleConsumer<RegionDefinition.Mutable> setter;
}
