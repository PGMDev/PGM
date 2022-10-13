package tc.oc.pgm.command.util;

import static net.kyori.adventure.text.Component.text;
import static tc.oc.pgm.util.text.TextException.noPermission;
import static tc.oc.pgm.util.text.TextException.playerOnly;
import static tc.oc.pgm.util.text.TextException.unknown;
import static tc.oc.pgm.util.text.TextException.usage;

import cloud.commandframework.annotations.AnnotationParser;
import cloud.commandframework.annotations.injection.ParameterInjector;
import cloud.commandframework.annotations.injection.ParameterInjectorRegistry;
import cloud.commandframework.arguments.parser.ArgumentParser;
import cloud.commandframework.arguments.parser.ParserParameters;
import cloud.commandframework.arguments.parser.ParserRegistry;
import cloud.commandframework.arguments.parser.StandardParameters;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.bukkit.CloudBukkitCapabilities;
import cloud.commandframework.exceptions.ArgumentParseException;
import cloud.commandframework.exceptions.CommandExecutionException;
import cloud.commandframework.exceptions.InvalidCommandSenderException;
import cloud.commandframework.exceptions.InvalidSyntaxException;
import cloud.commandframework.exceptions.NoPermissionException;
import cloud.commandframework.execution.CommandExecutionCoordinator;
import cloud.commandframework.extra.confirmation.CommandConfirmationManager;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.minecraft.extras.MinecraftHelp;
import cloud.commandframework.paper.PaperCommandManager;
import io.leangen.geantyref.TypeFactory;
import io.leangen.geantyref.TypeToken;
import java.lang.reflect.Type;
import java.time.Duration;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.util.ComponentMessageThrowable;
import org.bukkit.command.CommandSender;
import tc.oc.pgm.api.Config;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.map.MapLibrary;
import tc.oc.pgm.api.map.MapOrder;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchManager;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.party.VictoryCondition;
import tc.oc.pgm.api.player.MatchPlayer;
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
import tc.oc.pgm.command.VotingCommand;
import tc.oc.pgm.command.injectors.AudienceProvider;
import tc.oc.pgm.command.injectors.MatchPlayerProvider;
import tc.oc.pgm.command.injectors.MatchProvider;
import tc.oc.pgm.command.injectors.TeamModuleInjector;
import tc.oc.pgm.command.parsers.DurationParser;
import tc.oc.pgm.command.parsers.MapInfoParser;
import tc.oc.pgm.command.parsers.MapPoolParser;
import tc.oc.pgm.command.parsers.MatchPlayerParser;
import tc.oc.pgm.command.parsers.PartyParser;
import tc.oc.pgm.command.parsers.TeamParser;
import tc.oc.pgm.command.parsers.TeamsParser;
import tc.oc.pgm.command.parsers.VictoryConditionParser;
import tc.oc.pgm.community.command.ModerationCommand;
import tc.oc.pgm.community.command.ReportCommand;
import tc.oc.pgm.listeners.ChatDispatcher;
import tc.oc.pgm.rotation.pools.MapPool;
import tc.oc.pgm.teams.Team;
import tc.oc.pgm.teams.TeamMatchModule;
import tc.oc.pgm.util.Audience;

public class CommandGraph {

  private final PGM pgm;
  private final PaperCommandManager<CommandSender> manager;
  private final MinecraftHelp<CommandSender> minecraftHelp;
  private final CommandConfirmationManager<CommandSender> confirmationManager;
  private final AnnotationParser<CommandSender> annotationParser;

  private final ParameterInjectorRegistry<CommandSender> injectors;
  private final ParserRegistry<CommandSender> parsers;

  public CommandGraph(PGM pgm) throws Exception {
    this.pgm = pgm;
    this.manager =
        PaperCommandManager.createNative(pgm, CommandExecutionCoordinator.simpleCoordinator());

    //
    // Create the Minecraft help menu system
    //
    this.minecraftHelp = new MinecraftHelp<>("/pgmhelp", Audience::get, manager);

    // Register Brigadier mappings
    if (this.manager.hasCapability(CloudBukkitCapabilities.BRIGADIER))
      this.manager.registerBrigadier();

    // Register asynchronous completions
    if (this.manager.hasCapability(CloudBukkitCapabilities.ASYNCHRONOUS_COMPLETION))
      this.manager.registerAsynchronousCompletions();

    // By default, suggestions run by a filtered processor.
    // That prevents valid suggestions like "s" -> "Something" or "someh" -> "Something"
    this.manager.commandSuggestionProcessor((cpc, strings) -> strings);

    // Create the confirmation this.manager. This allows us to require certain commands to be
    // confirmed before they can be executed
    this.confirmationManager =
        new CommandConfirmationManager<>(
            30L,
            TimeUnit.SECONDS,
            // TODO: clickable
            context ->
                Audience.get(context.getCommandContext().getSender())
                    .sendWarning(text("Confirmation required. Confirm using /pgm confirm.")),
            sender ->
                Audience.get(sender).sendWarning(text("You don't have any pending commands.")));
    this.confirmationManager.registerConfirmationProcessor(this.manager);

    final Function<ParserParameters, CommandMeta> commandMetaFunction =
        p ->
            CommandMeta.simple()
                .with(
                    CommandMeta.DESCRIPTION,
                    p.get(StandardParameters.DESCRIPTION, "No description"))
                .build();

    this.annotationParser =
        new AnnotationParser<>(manager, CommandSender.class, commandMetaFunction);

    // Utility
    this.injectors = manager.parameterInjectorRegistry();
    this.parsers = manager.parserRegistry();

    setupExceptionHandlers();
    setupInjectors();
    setupParsers();
    registerCommands();

    manager.command(
        manager
            .commandBuilder("pgm")
            .literal("confirm")
            .meta(CommandMeta.DESCRIPTION, "Confirm a pending command")
            .handler(this.confirmationManager.createConfirmationExecutionHandler()));

    manager.command(
        manager
            .commandBuilder("pgmhelp")
            .argument(StringArgument.optional("query", StringArgument.StringMode.GREEDY))
            .handler(
                context ->
                    minecraftHelp.queryCommands(
                        context.<String>getOptional("query").orElse(""), context.getSender())));
  }

  //
  // Commands
  //
  public void registerCommands() {
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
    register(new MapOrderCommand());
    register(new MapPoolCommand()); //
    register(new MatchCommand());
    register(new ModeCommand());
    register(new ProximityCommand());
    register(new RestartCommand());
    register(new SettingCommand());
    register(new StartCommand());
    register(new StatsCommand());
    register(new TeamCommand());
    register(new TimeLimitCommand());
    register(new VotingCommand());
    register(new MapDevCommand());

    if (pgm.getConfiguration().isCommunityMode()) {
      register(new ReportCommand());
      register(new ModerationCommand());
    }

    register(pgm.getVanishManager());
    register(ChatDispatcher.get());
  }

  public void register(Object command, String... aliases) {
    annotationParser.parse(command);
  }

  //
  // Injectors
  //
  protected void setupInjectors() {
    registerInjector(PGM.class, Function.identity());
    registerInjector(Config.class, PGM::getConfiguration);
    registerInjector(MatchManager.class, PGM::getMatchManager);
    registerInjector(MapLibrary.class, PGM::getMapLibrary);
    registerInjector(MapOrder.class, PGM::getMapOrder);

    registerInjector(Audience.class, new AudienceProvider());
    registerInjector(Match.class, new MatchProvider());
    registerInjector(MatchPlayer.class, new MatchPlayerProvider());
    registerInjector(TeamMatchModule.class, new TeamModuleInjector());
  }

  private <T> void registerInjector(Class<T> type, ParameterInjector<CommandSender, T> provider) {
    injectors.registerInjector(type, provider);
  }

  private <T> void registerInjector(Class<T> type, Function<PGM, T> function) {
    registerInjector(type, (a, b) -> function.apply(PGM.get()));
  }

  //
  // Parsers
  //
  private void setupParsers() {
    // Cloud has a default duration parser, but time type is not optional
    registerParser(Duration.class, new DurationParser());
    registerParser(MatchPlayer.class, new MatchPlayerParser());
    registerParser(MapPool.class, new MapPoolParser());

    registerParser(MapInfo.class, MapInfoParser::new);
    registerParser(Party.class, PartyParser::new);
    registerParser(Team.class, TeamParser::new);
    registerParser(TypeFactory.parameterizedClass(Collection.class, Team.class), TeamsParser::new);
    registerParser(VictoryCondition.class, new VictoryConditionParser());
    // registerParser(SettingKey.class, new SettingKeyParser());
    // registerParser(SettingValue.class, new EnumProvider<>(SettingValue.class));
  }

  private <T> void registerParser(Class<T> type, ArgumentParser<CommandSender, T> parser) {
    parsers.registerParserSupplier(TypeToken.get(type), op -> parser);
  }

  private <T> void registerParser(Class<T> type, ParserBuilder<T> parser) {
    parsers.registerParserSupplier(TypeToken.get(type), op -> parser.create(manager, op));
  }

  private <T> void registerParser(Type type, ParserBuilder<T> parser) {
    parsers.registerParserSupplier(TypeToken.get(type), op -> parser.create(manager, op));
  }

  //
  // Exception handling
  //
  private void setupExceptionHandlers() {
    registerExceptionHandler(InvalidSyntaxException.class, e -> usage(e.getCorrectSyntax()));
    registerExceptionHandler(InvalidCommandSenderException.class, e -> playerOnly());
    registerExceptionHandler(NoPermissionException.class, e -> noPermission());

    manager.registerExceptionHandler(ArgumentParseException.class, this::handleException);
    manager.registerExceptionHandler(CommandExecutionException.class, this::handleException);
  }

  private <E extends Exception> void registerExceptionHandler(
      Class<E> ex, Function<E, ComponentLike> toComponent) {
    manager.registerExceptionHandler(
        ex, (cs, e) -> Audience.get(cs).sendWarning(toComponent.apply(e)));
  }

  private <E extends Exception> void handleException(CommandSender cs, E e) {
    Audience audience = Audience.get(cs);
    ComponentMessageThrowable messageThrowable = getMessageThrowable(e);

    if (messageThrowable != null) {
      final Component message = messageThrowable.componentMessage();
      if (message != null) audience.sendWarning(message);
    } else {
      audience.sendWarning(unknown(e).componentMessage());
      e.printStackTrace();
    }
  }

  private ComponentMessageThrowable getMessageThrowable(Throwable t) {
    if (t == null || t instanceof ComponentMessageThrowable) return (ComponentMessageThrowable) t;
    return getMessageThrowable(t.getCause());
  }
}
