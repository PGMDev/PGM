package tc.oc.pgm.map;

import java.util.logging.Logger;
import javax.annotation.Nullable;
import org.jdom2.Document;
import tc.oc.component.Component;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.match.MatchModule;
import tc.oc.pgm.match.MatchModuleFactory;
import tc.oc.pgm.module.ModuleInfo;
import tc.oc.pgm.module.ModuleLoadException;
import tc.oc.pgm.module.ModuleRegistry;
import tc.oc.xml.InvalidXMLException;

/**
 * Modules are immutable bits of code that manage specific game play and management tasks. They
 * should never store state that is specific to a match in the module object itself, but rather
 * store it on an appropriate game object such as a player, match, or team.
 *
 * <p>Scope: Map
 */
public abstract class MapModule<T extends MatchModule> implements MatchModuleFactory<T> {
  /**
   * Gets the static module information.
   *
   * @return ModuleDescription that corresponds to this class.
   */
  public ModuleInfo getInfo() {
    return ModuleInfo.get(this.getClass());
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }

  /**
   * Gets the name of this module.
   *
   * @return Name of this module.
   */
  public String getName() {
    return this.getInfo().getName();
  }

  /**
   * Get the name of the game implemented by this module, or null if it does not implement a game
   */
  public @Nullable Component getGame(MapModuleContext context) {
    return null;
  }

  @Override
  public T createMatchModule(Match match) throws ModuleLoadException {
    return null;
  }

  /** Called after the module is registered with PGM (on server startup) */
  public static void register(ModuleRegistry context, Logger logger) throws Throwable {}

  /** Called when loading a map */
  public static MapModule parse(MapModuleContext context, Logger logger, Document doc)
      throws InvalidXMLException {
    return null;
  }

  /**
   * Called after all modules have finished parsing and all FeatureReferences have been resolved
   * successfully. A module can use this method to replace FeatureReferences with
   * FeatureDefinitions. It can also throw InvalidXMLExceptions from here if needed e.g. for errors
   * that can't be detected until referenced features are available.
   */
  public void postParse(MapModuleContext context, Logger logger, Document doc)
      throws InvalidXMLException {}
}
