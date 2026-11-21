package tc.oc.pgm.regions;

import java.util.function.ObjDoubleConsumer;
import java.util.function.ToDoubleFunction;
import tc.oc.pgm.api.region.Region;

public interface Component<R extends Region> {
  String name();

  ToDoubleFunction<R> getter();

  ObjDoubleConsumer<R> setter();
}
