package tc.oc.pgm.cycle;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.map.MapOrder;
import tc.oc.pgm.api.match.Match;

public class FakeCycleCountdown extends CycleCountdown {

  private final SmoothRestartModule smoothRestartModule;

  public FakeCycleCountdown(Match match, SmoothRestartModule smoothRestartModule) {
    super(match);
    this.smoothRestartModule = smoothRestartModule;
  }

  @Override
  protected Component formatText() {
    Component mapName = nextMap == null ? null : text(nextMap.getName(), NamedTextColor.AQUA);

    TranslatableComponent cycleComponent;
    if (remaining.isZero()) {
      cycleComponent =
          mapName != null ? translatable("map.cycledMap", mapName) : translatable("map.cycled");
    } else {
      Component secs = secondsRemaining(NamedTextColor.DARK_RED);
      cycleComponent = mapName != null
          ? translatable("map.cycleMap", mapName, secs)
          : translatable("map.cycle", secs);
    }

    return cycleComponent.color(NamedTextColor.GOLD);
  }

  @Override
  protected void checkSetNext() {
    final MapOrder mapOrder = PGM.get().getMapOrder();
    if (remaining.getSeconds() <= preloadSecs) {
      if (nextMap != null) return;
      nextMap = mapOrder.popNextMap();
      smoothRestartModule.updateSelectedMap(nextMap);
      Bukkit.broadcastMessage(nextMap.toString());
    } else {
      nextMap = mapOrder.getNextMap();
      if (nextMap != null) {
        smoothRestartModule.updateSelectedMap(nextMap);
      }
    }
  }
}
