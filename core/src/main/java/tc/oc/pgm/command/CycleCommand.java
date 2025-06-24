package tc.oc.pgm.command;

import static tc.oc.pgm.api.Permissions.DEV;
import static tc.oc.pgm.api.map.Phase.DEVELOPMENT;
import static tc.oc.pgm.util.text.TextException.exception;

import java.time.Duration;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.annotation.specifier.FlagYielding;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.CommandDescription;
import org.incendo.cloud.annotations.Flag;
import org.incendo.cloud.annotations.Permission;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.map.MapOrder;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.cycle.CycleMatchModule;

public final class CycleCommand {

  @Command("cycle [duration] [map]")
  @CommandDescription("Cycle to the next match")
  @Permission(Permissions.START)
  public void cycle(
      CommandSender sender,
      Match match,
      MapOrder mapOrder,
      @Argument("duration") Duration duration,
      @Argument("map") @FlagYielding MapInfo map,
      @Flag(value = "force", aliases = "f") boolean force,
      @Flag(value = "override") boolean override) {
    if (match.isRunning()) {
      if (!force) throw exception("admin.matchRunning.cycle");
      if (needsOverride(match) && !override) {
        throw exception("admin.matchRunning.cycle.override");
      }
    }

    if (map != null && mapOrder.getNextMap() != map) {
      if (PGM.get().getConfiguration().enforceDevPhase()
          && DEVELOPMENT.equals(map.getPhase())
          && !sender.hasPermission(DEV)) {
        throw exception("map.setNext.notDev");
      }
      mapOrder.setNextMap(map);
      MapOrderCommand.sendSetNextMessage(map, sender);
    }

    match.needModule(CycleMatchModule.class).startCountdown(duration);
  }

  @Command("recycle|rematch [duration]")
  @CommandDescription("Reload (cycle to) the current map")
  @Permission(Permissions.START)
  public void recycle(
      CommandSender sender,
      Match match,
      MapOrder mapOrder,
      @Argument("duration") Duration duration,
      @Flag(value = "force", aliases = "f") boolean force,
      @Flag(value = "override") boolean override) {
    cycle(sender, match, mapOrder, duration, match.getMap(), force, override);
  }

  private boolean needsOverride(Match match) {
    return match.getPlayers().size() >= 8 && match.getDuration().toMinutes() >= 5;
  }
}
