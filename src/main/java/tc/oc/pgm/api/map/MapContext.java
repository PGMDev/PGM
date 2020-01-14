package tc.oc.pgm.api.map;

import tc.oc.pgm.api.module.ModuleContext;

public interface MapContext extends MapInfo, MapInfoExtra, ModuleContext<MapModule> {

  /**
   * Get the {@link MapSource} where the source files can be downloaded.
   *
   * @return A {@link MapSource}.
   */
  MapSource getSource();

}
