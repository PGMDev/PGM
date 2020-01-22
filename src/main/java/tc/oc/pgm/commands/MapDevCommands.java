package tc.oc.pgm.commands;

import app.ashcon.intake.Command;
import app.ashcon.intake.parametric.annotation.Switch;
import tc.oc.pgm.api.map.MapLibrary;

public class MapDevCommands {

  @Command(
      aliases = {"loadnewmaps"},
      desc = "Loads new maps")
  public static void loadNewMaps(MapLibrary library, @Switch('f') boolean force) {
    library.loadNewMaps(force);
  }
}
