package tc.oc.pgm.api.map;

import tc.oc.pgm.api.module.ModuleContext;

/** A {@link MapInfo} that is "loaded" with its {@link MapModule}s and a {@link MapSource}. */
public interface MapContext extends ModuleContext<MapModule> {

  /**
   * Get an immutable {@link MapInfo} which doesn't hold strong references to the context.
   *
   * @return A {@link MapInfo} for this context.
   */
  MapInfo getInfo();
}
