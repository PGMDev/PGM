package tc.oc.pgm.command.graph;

import static tc.oc.pgm.util.text.TextException.exception;

import app.ashcon.intake.argument.ArgumentException;
import app.ashcon.intake.argument.CommandArgs;
import app.ashcon.intake.argument.MissingArgumentException;
import app.ashcon.intake.bukkit.parametric.provider.BukkitProvider;
import app.ashcon.intake.parametric.annotation.Default;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.StringJoiner;
import org.bukkit.command.CommandSender;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.match.Match;

final class MapInfoParser implements BukkitProvider<MapInfo> {

  @Override
  public String getName() {
    return "map";
  }

  @Override
  public MapInfo get(CommandSender sender, CommandArgs args, List<? extends Annotation> annotations)
      throws ArgumentException {
    final PGM pgm = PGM.get();

    MapInfo map;
    if (args.hasNext()) {
      map = pgm.getMapLibrary().getMap(getRemainingText(args));
    } else {
      map = getDefault(pgm, sender, annotations);
    }

    if (map == null) throw exception("map.notFound");

    return map;
  }

  private String getRemainingText(CommandArgs args) throws MissingArgumentException {
    StringJoiner mapName = new StringJoiner(" ");

    while (args.hasNext()) {
      mapName.add(args.next());
    }

    return mapName.toString();
  }

  private MapInfo getDefault(
      PGM pgm, CommandSender sender, List<? extends Annotation> annotations) {
    for (Annotation ann : annotations) {
      if (!(ann instanceof Default)) continue;

      for (String def : ((Default) ann).value()) {
        MapInfo map = getDefault(pgm, sender, def);
        if (map != null) return map;
      }
    }
    return null;
  }

  private MapInfo getDefault(PGM pgm, CommandSender sender, String def) {
    switch (def) {
      case "next":
        return pgm.getMapOrder().getNextMap();
      case "current":
        final Match match = pgm.getMatchManager().getMatch(sender);
        return match != null ? match.getMap() : null;
      default:
        throw new IllegalArgumentException(
            "Unsupported @Default value: " + def + ", expected one of: next, current");
    }
  }
}
