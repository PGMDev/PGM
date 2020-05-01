package tc.oc.pgm.commands;

import app.ashcon.intake.Command;
import app.ashcon.intake.CommandException;
import app.ashcon.intake.parametric.annotation.Default;
import app.ashcon.intake.parametric.annotation.Switch;
import app.ashcon.intake.parametric.annotation.Text;
import java.time.Duration;
import java.util.AbstractMap;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.kyori.text.TranslatableComponent;
import net.kyori.text.format.TextColor;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.CommandSender;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.map.MapOrder;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.cycle.CycleCountdown;
import tc.oc.pgm.listeners.ChatDispatcher;
import tc.oc.pgm.restart.CancelRestartEvent;
import tc.oc.pgm.restart.RequestRestartEvent;
import tc.oc.pgm.restart.RestartManager;
import tc.oc.pgm.rotation.MapPool;
import tc.oc.pgm.rotation.MapPoolManager;
import tc.oc.pgm.start.StartMatchModule;
import tc.oc.pgm.timelimit.TimeLimitCountdown;
import tc.oc.pgm.timelimit.TimeLimitMatchModule;
import tc.oc.pgm.util.StringUtils;
import tc.oc.pgm.util.UsernameFormatUtils;
import tc.oc.pgm.util.chat.Audience;
import tc.oc.pgm.util.component.Component;
import tc.oc.pgm.util.component.types.PersonalizedText;
import tc.oc.pgm.util.component.types.PersonalizedTranslatable;
import tc.oc.pgm.util.translations.AllTranslations;

public class AdminCommands {

  @Command(
      aliases = {"restart"},
      desc = "Restart the server",
      usage = "[seconds] - defaults to 30 seconds",
      flags = "f",
      perms = Permissions.STOP)
  public void restart(
      Audience audience, Match match, @Default("30") int duration, @Switch('f') boolean force) {

    if (!force && match.isRunning()) {
      audience.sendWarning(TranslatableComponent.of("admin.matchRunning.restart"));
      return;
    }

    queueRestart(audience, match, duration, true);
  }

  @Command(
      aliases = {"queuerestart", "qr"},
      desc = "Restart the server at the next safe opportunity",
      usage = "[seconds] - defaults to 30 seconds",
      flags = "f",
      perms = Permissions.STOP)
  public void queueRestart(
      Audience audience, Match match, @Default("30") int duration, @Switch('f') boolean force) {
    RestartManager.queueRestart(
        "Restart requested via /queuerestart command", Duration.ofSeconds(duration));

    if (force && match.isRunning()) {
      match.finish();
    }

    if (match.isRunning()) {
      audience.sendMessage(
          TranslatableComponent.of("admin.queueRestart.restartQueued", TextColor.RED));
    } else {
      audience.sendMessage(
          TranslatableComponent.of("admin.queueRestart.restartingNow", TextColor.GREEN));
    }

    PGM.get().getServer().getPluginManager().callEvent(new RequestRestartEvent());
  }

  @Command(
      aliases = {"cancelrestart", "cr"},
      desc = "Cancels a previously requested restart",
      perms = Permissions.STOP)
  public void cancelRestart(Audience audience) {
    if (RestartManager.isQueued()) {
      PGM.get().getServer().getPluginManager().callEvent(new CancelRestartEvent());
      audience.sendMessage(
          TranslatableComponent.of("admin.cancelRestart.restartUnqueued", TextColor.RED));
    } else {
      audience.sendMessage(
          TranslatableComponent.of("admin.cancelRestart.noActionTaken", TextColor.RED));
    }
  }

  @Command(
      aliases = {"end", "finish"},
      desc = "Ends the current running match, optionally with a winner",
      usage = "[competitor]",
      perms = Permissions.STOP)
  public static void end(CommandSender sender, Match match, @Nullable @Text String target)
      throws CommandException {
    Competitor winner = StringUtils.bestFuzzyMatch(target, getCompetitorMap(sender, match), 0.9);

    if (target != null && winner == null)
      throw new CommandException(
          AllTranslations.get().translate("command.competitorNotFound", sender));

    boolean ended = match.finish(winner);

    if (!ended)
      throw new CommandException(AllTranslations.get().translate("admin.end.unknownError", sender));
  }

  @Command(
      aliases = {"setnext", "sn"},
      desc = "Sets the next map. Note that the rotation will go to this map then resume as normal.",
      usage = "[map name]",
      flags = "f",
      perms = Permissions.SETNEXT)
  public static void setNext(
      CommandSender sender,
      @Switch('f') boolean force,
      @Text MapInfo map,
      MapOrder mapOrder,
      Match match)
      throws CommandException {
    if (RestartManager.isQueued() && !force) {
      throw new CommandException(AllTranslations.get().translate("map.setNext.confirm", sender));
    }

    mapOrder.setNextMap(map);

    if (RestartManager.isQueued()) {
      RestartManager.cancelRestart();
      sender.sendMessage(
          ChatColor.GREEN
              + AllTranslations.get().translate("admin.cancelRestart.restartUnqueued", sender));
    }

    Component mapName = new PersonalizedText(map.getName()).color(ChatColor.DARK_PURPLE);
    Component successful =
        new PersonalizedTranslatable(
                "map.setNext", UsernameFormatUtils.formatStaffName(sender, match), mapName)
            .getPersonalizedText()
            .color(ChatColor.GRAY);
    ChatDispatcher.broadcastAdminChatMessage(successful, match);
  }

  @Command(
      aliases = {"setpool", "setrot"},
      desc = "Set a custom map pool or revert to dynamic",
      usage = "[pool name] -r (revert to dynamic) -t (time limit for map pool)",
      flags = "r",
      perms = Permissions.SETNEXT)
  public static void setPool(
      CommandSender sender,
      Match match,
      MapOrder mapOrder,
      @Nullable String poolName,
      @Switch('r') boolean reset,
      @Switch('t') Duration timeLimit)
      throws CommandException {
    if (!match.getCountdown().getAll(CycleCountdown.class).isEmpty()) {
      sender.sendMessage(
          new PersonalizedTranslatable("command.admin.setPool.activeCycle")
              .getPersonalizedText()
              .color(ChatColor.RED));
      return;
    }
    MapPoolManager mapPoolManager = MapPoolCommands.getMapPoolManager(sender, mapOrder);
    MapPool newPool =
        reset
            ? mapPoolManager.getAppropriateDynamicPool(match).orElse(null)
            : mapPoolManager.getMapPoolByName(poolName);

    if (newPool == null) {
      Component error =
          new PersonalizedTranslatable(
                  reset ? "command.pools.noDynamic " : "command.pools.noPoolMatch")
              .getPersonalizedText()
              .color(ChatColor.RED);
      sender.sendMessage(error);
    } else {
      if (newPool.equals(mapPoolManager.getActiveMapPool())) {
        sender.sendMessage(
            new PersonalizedTranslatable(
                    "command.pools.set.matching",
                    new PersonalizedText(newPool.getName()).color(ChatColor.LIGHT_PURPLE))
                .getPersonalizedText()
                .color(ChatColor.GRAY));
        return;
      }
      mapPoolManager.updateActiveMapPool(newPool, match, true, sender, timeLimit);
    }
  }

  @Command(
      aliases = {"cancel"},
      desc = "Cancels all active PGM countdowns and disables auto-start for the current match",
      perms = Permissions.STOP)
  public static void cancel(CommandSender sender, Match match) {
    if (!match.getCountdown().getAll(TimeLimitCountdown.class).isEmpty()) {
      TimeLimitMatchModule tlmm = match.getModule(TimeLimitMatchModule.class);
      tlmm.cancel();
      tlmm.setTimeLimit(null);
    }

    match.getCountdown().cancelAll();
    match.needModule(StartMatchModule.class).setAutoStart(false);
    sender.sendMessage(
        ChatColor.GREEN + AllTranslations.get().translate("admin.cancelCountdowns", sender));
  }

  @Command(
      aliases = {"pgm"},
      desc = "Reload the PGM configuration",
      perms = Permissions.RELOAD)
  public void pgm(Audience audience) {
    PGM.get().reloadConfig();
    audience.sendMessage(TranslatableComponent.of("admin.reloadConfig", TextColor.GREEN));
  }

  private static Map<String, Competitor> getCompetitorMap(CommandSender sender, Match match) {
    return match.getCompetitors().stream()
        .map(competitor -> new AbstractMap.SimpleEntry<>(competitor.getName(sender), competitor))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }
}
