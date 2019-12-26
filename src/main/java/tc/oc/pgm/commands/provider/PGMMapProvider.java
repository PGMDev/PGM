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
import tc.oc.pgm.api.match.MatchManager;
import tc.oc.pgm.commands.annotations.Text;
import tc.oc.pgm.map.MapLibrary;
import tc.oc.pgm.map.PGMMap;
import tc.oc.util.StringUtils;

public class PGMMapProvider implements BukkitProvider<PGMMap> {

  private final MatchManager matchManager;
  private final MapLibrary mapLibrary;

  public PGMMapProvider(MatchManager matchManager, MapLibrary mapLibrary) {
    this.matchManager = matchManager;
    this.mapLibrary = mapLibrary;
  }

  @Override
  public String getName() {
    return "map";
  }

  @Override
  public PGMMap get(CommandSender sender, CommandArgs args, List<? extends Annotation> annotations)
      throws ArgumentException {
    PGMMap map = matchManager.getMatch(sender).getMap();

    if (args.hasNext()) {
      String mapName = getRemainingText(args);
      map = mapLibrary.getMapByNameOrId(mapName).orElse(null);
      if (map == null) {
        String fuzzyName = StringUtils.bestFuzzyMatch(mapName, mapLibrary.getMapNames(), 0.9);
        map = mapLibrary.getMapByNameOrId(fuzzyName).orElse(null);
      }
    } else if (isGoToNext(annotations)) {
      map = matchManager.getMapOrder().getNextMap();
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

  private boolean useRemainingText(List<? extends Annotation> annotations) {
    return annotations.stream().anyMatch(o -> o instanceof Text);
  }

  private boolean isGoToNext(List<? extends Annotation> annotations) {
    return annotations.stream()
        .filter(o -> o instanceof Default)
        .findFirst()
        .filter(value -> Arrays.asList(((Default) value).value()).contains("next"))
        .isPresent();
  }
}
