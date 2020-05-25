package tc.oc.pgm.command.graph;

import app.ashcon.intake.argument.ArgumentException;
import app.ashcon.intake.argument.CommandArgs;
import app.ashcon.intake.argument.MissingArgumentException;
import app.ashcon.intake.bukkit.parametric.provider.BukkitProvider;
import app.ashcon.intake.parametric.annotation.Default;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.List;
import org.bukkit.command.CommandSender;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.util.text.TextException;

final class MapInfoParser implements BukkitProvider<MapInfo> {

  @Override
  public String getName() {
    return "map";
  }

  @Override
  public MapInfo get(CommandSender sender, CommandArgs args, List<? extends Annotation> annotations)
      throws ArgumentException {
    final PGM pgm = PGM.get();

    MapInfo map = null;
    if (args.hasNext()) {
      map = pgm.getMapLibrary().getMap(getRemainingText(args));
    } else if (isNextMap(annotations)) {
      map = pgm.getMapOrder().getNextMap();
    } else {
      final Match match = pgm.getMatchManager().getMatch(sender);
      if (match != null) {
        map = match.getMap();
      }
    }

    if (map == null && !isNextMap(annotations)) {
      throw TextException.of("map.notFound");
    }

    return map;
  }

  private String getRemainingText(CommandArgs args) throws MissingArgumentException {
    StringBuilder mapName = new StringBuilder();
    boolean first = true;

    while (args.hasNext()) {
      if (!first) {
        mapName.append(" ");
      }

      mapName.append(args.next());
      first = false;
    }

    return mapName.toString();
  }

  private boolean isNextMap(List<? extends Annotation> annotations) {
    return annotations.stream()
        .filter(o -> o instanceof Default)
        .findFirst()
        .filter(value -> Arrays.asList(((Default) value).value()).contains("next"))
        .isPresent();
  }
}
