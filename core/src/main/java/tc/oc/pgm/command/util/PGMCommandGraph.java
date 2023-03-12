package tc.oc.pgm.command.util;

import static net.kyori.adventure.text.Component.text;

import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.extra.confirmation.CommandConfirmationManager;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.minecraft.extras.MinecraftHelp;
import io.leangen.geantyref.TypeFactory;
import java.time.Duration;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import tc.oc.pgm.action.actions.ExposedAction;
import tc.oc.pgm.api.Config;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.map.MapLibrary;
import tc.oc.pgm.api.map.MapOrder;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchManager;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.party.VictoryCondition;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.setting.SettingKey;
import tc.oc.pgm.api.setting.SettingValue;
import tc.oc.pgm.classes.PlayerClass;
import tc.oc.pgm.command.ActionCommand;
import tc.oc.pgm.command.AdminCommand;
import tc.oc.pgm.command.CancelCommand;
import tc.oc.pgm.command.ClassCommand;
import tc.oc.pgm.command.CycleCommand;
import tc.oc.pgm.command.FinishCommand;
import tc.oc.pgm.command.FreeForAllCommand;
import tc.oc.pgm.command.InventoryCommand;
import tc.oc.pgm.command.JoinCommand;
import tc.oc.pgm.command.ListCommand;
import tc.oc.pgm.command.MapCommand;
import tc.oc.pgm.command.MapDevCommand;
import tc.oc.pgm.command.MapOrderCommand;
import tc.oc.pgm.command.MapPoolCommand;
import tc.oc.pgm.command.MatchCommand;
import tc.oc.pgm.command.ModeCommand;
import tc.oc.pgm.command.ProximityCommand;
import tc.oc.pgm.command.RestartCommand;
import tc.oc.pgm.command.SettingCommand;
import tc.oc.pgm.command.StartCommand;
import tc.oc.pgm.command.StatsCommand;
import tc.oc.pgm.command.TeamCommand;
import tc.oc.pgm.command.TimeLimitCommand;
import tc.oc.pgm.command.VanishCommand;
import tc.oc.pgm.command.VotingCommand;
import tc.oc.pgm.command.injectors.MapPollProvider;
import tc.oc.pgm.command.injectors.MapPoolManagerProvider;
import tc.oc.pgm.command.injectors.MatchModuleInjectionService;
import tc.oc.pgm.command.injectors.MatchPlayerProvider;
import tc.oc.pgm.command.injectors.MatchProvider;
import tc.oc.pgm.command.injectors.PlayerProvider;
import tc.oc.pgm.command.injectors.TeamMatchModuleProvider;
import tc.oc.pgm.command.parsers.DurationParser;
import tc.oc.pgm.command.parsers.EnumParser;
import tc.oc.pgm.command.parsers.ExposedActionParser;
import tc.oc.pgm.command.parsers.FilterArgumentParser;
import tc.oc.pgm.command.parsers.MapInfoParser;
import tc.oc.pgm.command.parsers.MapPoolParser;
import tc.oc.pgm.command.parsers.MatchPlayerParser;
import tc.oc.pgm.command.parsers.ModeParser;
import tc.oc.pgm.command.parsers.OfflinePlayerParser;
import tc.oc.pgm.command.parsers.PartyParser;
import tc.oc.pgm.command.parsers.PlayerClassParser;
import tc.oc.pgm.command.parsers.PlayerParser;
import tc.oc.pgm.command.parsers.RotationParser;
import tc.oc.pgm.command.parsers.SettingValueParser;
import tc.oc.pgm.command.parsers.TeamParser;
import tc.oc.pgm.command.parsers.TeamsParser;
import tc.oc.pgm.command.parsers.VictoryConditionParser;
import tc.oc.pgm.listeners.ChatDispatcher;
import tc.oc.pgm.modes.Mode;
import tc.oc.pgm.rotation.MapPoolManager;
import tc.oc.pgm.rotation.pools.MapPool;
import tc.oc.pgm.rotation.pools.MapPoolType;
import tc.oc.pgm.rotation.pools.Rotation;
import tc.oc.pgm.rotation.vote.MapPoll;
import tc.oc.pgm.teams.Team;
import tc.oc.pgm.teams.TeamMatchModule;
import tc.oc.pgm.util.Audience;

public class PGMCommandGraph extends CommandGraph<PGM> {

  public PGMCommandGraph(PGM pgm) throws Exception {
    super(pgm);
  }

  protected MinecraftHelp<CommandSender> createHelp() {
    // Create the Minecraft help menu system
    return new MinecraftHelp<>("/pgm help", Audience::get, manager);
  }

  protected CommandConfirmationManager<CommandSender> createConfirmationManager() {
    CommandConfirmationManager<CommandSender> ccm =
        new CommandConfirmationManager<>(
            30L,
            TimeUnit.SECONDS,
            // TODO: clickable & translatable
            context ->
                Audience.get(context.getCommandContext().getSender())
                    .sendWarning(text("Confirmation required. Confirm using /pgm confirm.")),
            sender ->
                Audience.get(sender).sendWarning(text("You don't have any pending commands.")));
    ccm.registerConfirmationProcessor(this.manager);
    return ccm;
  }

  // Commands
  protected void registerCommands() {
    register(new ActionCommand());
    register(new AdminCommand());
    register(new CancelCommand());
    register(new ClassCommand());
    register(new CycleCommand());
    register(new FinishCommand());
    register(new FreeForAllCommand());
    register(new InventoryCommand());
    register(new JoinCommand());
    register(new ListCommand());
    register(new MapCommand());
    register(new MapDevCommand());
    register(new MapOrderCommand());
    register(new MapPoolCommand());
    register(new MatchCommand());
    register(new ModeCommand());
    register(new ProximityCommand());
    register(new RestartCommand());
    register(SettingCommand.getInstance());
    register(new StartCommand());
    register(new StatsCommand());
    register(new TeamCommand());
    register(new TimeLimitCommand());
    register(new VotingCommand());

    if (PGM.get().getConfiguration().isVanishEnabled()) register(new VanishCommand());

    register(ChatDispatcher.get());

    manager.command(
        manager
            .commandBuilder("pgm")
            .literal("confirm")
            .meta(CommandMeta.DESCRIPTION, "Confirm a pending command")
            .handler(this.confirmationManager.createConfirmationExecutionHandler()));

    manager.command(
        manager
            .commandBuilder("pgm")
            .literal("help")
            .argument(StringArgument.optional("query", StringArgument.StringMode.GREEDY))
            .handler(
                context ->
                    minecraftHelp.queryCommands(
                        context.<String>getOptional("query").orElse(""), context.getSender())));
  }

  // Injectors
  protected void setupInjectors() {
    registerInjector(PGM.class, Function.identity());
    registerInjector(Config.class, PGM::getConfiguration);
    registerInjector(MatchManager.class, PGM::getMatchManager);
    registerInjector(MapLibrary.class, PGM::getMapLibrary);
    registerInjector(MapOrder.class, PGM::getMapOrder);

    // NOTE: order is important. Audience must be first, otherwise a Match or MatchPlayer (which
    // implement Audience) would be used, causing everyone to get all command feedback (Match), or
    // console being unable to use commands (MatchPlayer).
    registerInjector(Audience.class, (c, s) -> Audience.get(c.getSender()));
    registerInjector(Match.class, new MatchProvider());
    registerInjector(MatchPlayer.class, new MatchPlayerProvider());
    registerInjector(TeamMatchModule.class, new TeamMatchModuleProvider());
    registerInjector(MapPoolManager.class, new MapPoolManagerProvider());
    registerInjector(MapPoll.class, new MapPollProvider());

    registerInjector(Player.class, new PlayerProvider());

    // Able to inject any match module
    injectors.registerInjectionService(new MatchModuleInjectionService());
  }

  //
  // Parsers
  //
  protected void setupParsers() {
    // Cloud has a default duration parser, but time type is not optional
    registerParser(Duration.class, new DurationParser());
    registerParser(Player.class, new PlayerParser());
    registerParser(OfflinePlayer.class, new OfflinePlayerParser());
    registerParser(MatchPlayer.class, new MatchPlayerParser());
    registerParser(MapPool.class, new MapPoolParser());
    registerParser(Rotation.class, new RotationParser());
    registerParser(MapPoolType.class, new EnumParser<>(MapPoolType.class, CommandKeys.POOL_TYPE));

    registerParser(MapInfo.class, MapInfoParser::new);
    registerParser(Party.class, PartyParser::new);
    registerParser(Team.class, TeamParser::new);
    registerParser(TypeFactory.parameterizedClass(Collection.class, Team.class), TeamsParser::new);
    registerParser(PlayerClass.class, PlayerClassParser::new);
    registerParser(Mode.class, ModeParser::new);
    registerParser(ExposedAction.class, ExposedActionParser::new);
    registerParser(
        TypeFactory.parameterizedClass(Optional.class, VictoryCondition.class),
        new VictoryConditionParser());
    registerParser(Filter.class, FilterArgumentParser::new);
    registerParser(SettingKey.class, new EnumParser<>(SettingKey.class, CommandKeys.SETTING_KEY));
    registerParser(SettingValue.class, new SettingValueParser());
  }
}
