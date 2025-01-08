package tc.oc.pgm.command;

import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.CommandDescription;
import org.incendo.cloud.annotations.Flag;
import org.incendo.cloud.annotations.Permission;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.map.MapLibrary;

public final class AdminCommand {

  @Command("pgm reload")
  @CommandDescription("Reload the config")
  @Permission(Permissions.RELOAD)
  public void pgm() {
    PGM.get().reloadConfig();
    PGM.get().getDatastore().refreshMapData();
  }

  @Command("loadnewmaps|findnewmaps|newmaps")
  @CommandDescription("Reload the config")
  @Permission(Permissions.RELOAD)
  public void loadNewMaps(MapLibrary library, @Flag(value = "force", aliases = "f") boolean force) {
    library.loadNewMaps(force);
  }
}
