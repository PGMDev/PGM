package tc.oc.pgm.command;

import app.ashcon.intake.Command;
import app.ashcon.intake.parametric.annotation.Switch;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.map.MapLibrary;

public final class AdminCommand {

  @Command(
      aliases = {"pgm"},
      desc = "Reload the config",
      perms = Permissions.RELOAD)
  public void pgm() {
    PGM.get().reloadConfig();
  }

  @Command(
      aliases = {"loadnewmaps", "findnewmaps", "newmaps"},
      desc = "Load new maps",
      flags = "f",
      perms = Permissions.RELOAD)
  public void loadNewMaps(MapLibrary library, @Switch('f') boolean force) {
    library.loadNewMaps(force);
  }
}
