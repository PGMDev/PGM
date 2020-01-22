package tc.oc.pgm.commands.provider;

import app.ashcon.intake.argument.ArgumentException;
import app.ashcon.intake.argument.CommandArgs;
import app.ashcon.intake.argument.MissingArgumentException;
import app.ashcon.intake.bukkit.parametric.provider.BukkitProvider;
import app.ashcon.intake.parametric.annotation.Default;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.List;
import org.bukkit.command.CommandSender;
import tc.oc.pgm.AllTranslations;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.map.MapLibrary;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchManager;
import tc.oc.pgm.rotation.MapOrder;

public class MapInfoProvider implements BukkitProvider<MapInfo> {

  private final MatchManager matchManager;
  private final MapLibrary mapLibrary;
  private final MapOrder mapOrder;

  public MapInfoProvider(MatchManager matchManager, MapLibrary mapLibrary, MapOrder mapOrder) {
    this.matchManager = matchManager;
    this.mapLibrary = mapLibrary;
    this.mapOrder = mapOrder;
  }

  @Override
  public String getName() {
    return "map";
  }

  @Override
  public MapInfo get(CommandSender sender, CommandArgs args, List<? extends Annotation> annotations)
      throws ArgumentException {
    MapInfo map = null;

    if (args.hasNext()) {
      String mapName = getRemainingText(args);
      final MapInfo context = mapLibrary.getMap(mapName);
      if (context != null) map = context;
    } else if (isGoToNext(annotations)) {
      map = mapOrder.getNextMap();
    } else {
      final Match match = matchManager.getMatch(sender);
      if (match != null) map = match.getMap();
    }

    if (map == null && !isGoToNext(annotations)) {
      throw new ArgumentException(AllTranslations.get().translate("command.mapNotFound", sender));
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

  private boolean isGoToNext(List<? extends Annotation> annotations) {
    return annotations.stream()
        .filter(o -> o instanceof Default)
        .findFirst()
        .filter(value -> Arrays.asList(((Default) value).value()).contains("next"))
        .isPresent();
  }
}
