package tc.oc.pgm.command;

import cloud.commandframework.annotations.CommandDescription;
import cloud.commandframework.annotations.CommandMethod;
import cloud.commandframework.annotations.CommandPermission;
import cloud.commandframework.annotations.Flag;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.map.MapLibrary;

public final class AdminCommand {

  @CommandMethod("pgm reload")
  @CommandDescription("Reload the config")
  @CommandPermission(Permissions.RELOAD)
  public void pgm() {
    PGM.get().reloadConfig();
  }

  @CommandMethod("loadnewmaps|findnewmaps|newmaps")
  @CommandDescription("Reload the config")
  @CommandPermission(Permissions.RELOAD)
  public void loadNewMaps(MapLibrary library, @Flag(value = "force", aliases = "f") boolean force) {
    library.loadNewMaps(force);
  }
}
