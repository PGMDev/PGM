package tc.oc.pgm.api.map;

import tc.oc.pgm.api.module.ModuleContext;

/** A {@link MapInfo} that is "loaded" with its {@link MapModule}s and a {@link MapSource}. */
public interface MapContext extends MapInfo, ModuleContext<MapModule> {

  /**
   * Get a {@link MapSource} to access the maps's files.
   *
   * @return A {@link MapSource}.
   */
  MapSource getSource();
}
