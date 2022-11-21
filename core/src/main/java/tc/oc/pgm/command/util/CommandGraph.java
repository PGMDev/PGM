package tc.oc.pgm.command.util;

import static net.kyori.adventure.text.Component.text;
import static tc.oc.pgm.util.text.TextException.noPermission;
import static tc.oc.pgm.util.text.TextException.playerOnly;
import static tc.oc.pgm.util.text.TextException.unknown;
import static tc.oc.pgm.util.text.TextException.usage;

import cloud.commandframework.CommandManager;
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
import cloud.commandframework.exceptions.parsing.ParserException;
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
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.util.ComponentMessageThrowable;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.Nullable;
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
import tc.oc.pgm.api.setting.SettingKey;
import tc.oc.pgm.api.setting.SettingValue;
import tc.oc.pgm.classes.PlayerClass;
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
import tc.oc.pgm.command.injectors.MapPollProvider;
import tc.oc.pgm.command.injectors.MapPoolManagerProvider;
import tc.oc.pgm.command.injectors.MatchModuleInjectionService;
import tc.oc.pgm.command.injectors.MatchPlayerProvider;
import tc.oc.pgm.command.injectors.MatchProvider;
import tc.oc.pgm.command.injectors.TeamMatchModuleProvider;
import tc.oc.pgm.command.parsers.DurationParser;
import tc.oc.pgm.command.parsers.EnumParser;
import tc.oc.pgm.command.parsers.MapInfoParser;
import tc.oc.pgm.command.parsers.MapPoolParser;
import tc.oc.pgm.command.parsers.MatchPlayerParser;
import tc.oc.pgm.command.parsers.ModeParser;
import tc.oc.pgm.command.parsers.PartyParser;
import tc.oc.pgm.command.parsers.PlayerClassParser;
import tc.oc.pgm.command.parsers.RotationParser;
import tc.oc.pgm.command.parsers.SettingValueParser;
import tc.oc.pgm.command.parsers.TeamParser;
import tc.oc.pgm.command.parsers.TeamsParser;
import tc.oc.pgm.command.parsers.VictoryConditionParser;
import tc.oc.pgm.community.command.ModerationCommand;
import tc.oc.pgm.community.command.ReportCommand;
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

    this.manager.setSetting(CommandManager.ManagerSettings.LIBERAL_FLAG_PARSING, true);

    // Create the Minecraft help menu system
    this.minecraftHelp = new MinecraftHelp<>("/pgm help", Audience::get, manager);

    // Register Brigadier mappings
    if (this.manager.hasCapability(CloudBukkitCapabilities.BRIGADIER))
      this.manager.registerBrigadier();

    // Register asynchronous completions
    if (this.manager.hasCapability(CloudBukkitCapabilities.ASYNCHRONOUS_COMPLETION))
      this.manager.registerAsynchronousCompletions();

    // Add the input queue to the context, this allows greedy suggestions to work on it
    this.manager.registerCommandPreProcessor(
        context ->
            context.getCommandContext().store(CommandKeys.INPUT_QUEUE, context.getInputQueue()));

    // By default, suggestions run by a filtered processor.
    // By default, it prevents suggestions like "s" -> "Something" or "someh" -> "Something"
    this.manager.commandSuggestionProcessor((cpc, strings) -> strings);

    // Create the confirmation this.manager. This allows us to require certain commands to be
    // confirmed before they can be executed
    this.confirmationManager =
        new CommandConfirmationManager<>(
            30L,
            TimeUnit.SECONDS,
            // TODO: clickable & translatable
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
            .commandBuilder("pgm")
            .literal("help")
            .argument(StringArgument.optional("query", StringArgument.StringMode.GREEDY))
            .handler(
                context ->
                    minecraftHelp.queryCommands(
                        context.<String>getOptional("query").orElse(""), context.getSender())));
  }

  // Commands
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
    register(new MapDevCommand());

    if (pgm.getConfiguration().isCommunityMode()) {
      register(new ReportCommand());
      register(new ModerationCommand());
    }

    register(pgm.getVanishManager());
    register(ChatDispatcher.get());
  }

  protected void register(Object command) {
    annotationParser.parse(command);
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

    // Able to inject any match module
    injectors.registerInjectionService(new MatchModuleInjectionService());
  }

  protected <T> void registerInjector(Class<T> type, ParameterInjector<CommandSender, T> provider) {
    injectors.registerInjector(type, provider);
  }

  protected <T> void registerInjector(Class<T> type, Function<PGM, T> function) {
    registerInjector(type, (a, b) -> function.apply(PGM.get()));
  }

  //
  // Parsers
  //
  protected void setupParsers() {
    // Cloud has a default duration parser, but time type is not optional
    registerParser(Duration.class, new DurationParser());
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
    registerParser(
        TypeFactory.parameterizedClass(Optional.class, VictoryCondition.class),
        new VictoryConditionParser());
    registerParser(SettingKey.class, new EnumParser<>(SettingKey.class, CommandKeys.SETTING_KEY));
    registerParser(SettingValue.class, new SettingValueParser());
  }

  protected <T> void registerParser(Class<T> type, ArgumentParser<CommandSender, T> parser) {
    parsers.registerParserSupplier(TypeToken.get(type), op -> parser);
  }

  protected <T> void registerParser(Class<T> type, ParserBuilder<T> parser) {
    parsers.registerParserSupplier(TypeToken.get(type), op -> parser.create(manager, op));
  }

  protected <T> void registerParser(Type type, ArgumentParser<CommandSender, T> parser) {
    parsers.registerParserSupplier(TypeToken.get(type), op -> parser);
  }

  protected <T> void registerParser(Type type, ParserBuilder<T> parser) {
    parsers.registerParserSupplier(TypeToken.get(type), op -> parser.create(manager, op));
  }

  // Exception handling
  protected void setupExceptionHandlers() {
    registerExceptionHandler(InvalidSyntaxException.class, e -> usage(e.getCorrectSyntax()));
    registerExceptionHandler(InvalidCommandSenderException.class, e -> playerOnly());
    registerExceptionHandler(NoPermissionException.class, e -> noPermission());

    manager.registerExceptionHandler(ArgumentParseException.class, this::handleException);
    manager.registerExceptionHandler(CommandExecutionException.class, this::handleException);
  }

  protected <E extends Exception> void registerExceptionHandler(
      Class<E> ex, Function<E, ComponentLike> toComponent) {
    manager.registerExceptionHandler(
        ex, (cs, e) -> Audience.get(cs).sendWarning(toComponent.apply(e)));
  }

  private <E extends Exception> void handleException(CommandSender cs, E e) {
    Audience audience = Audience.get(cs);
    Component message = getMessage(e);
    if (message != null) audience.sendWarning(message);
  }

  private @Nullable Component getMessage(Throwable t) {
    ComponentMessageThrowable messageThrowable = getParentCause(t, ComponentMessageThrowable.class);
    if (messageThrowable != null) return messageThrowable.componentMessage();

    ParserException parseException = getParentCause(t, ParserException.class);
    if (parseException != null) return text(parseException.getMessage());

    t.printStackTrace();
    return unknown(t).componentMessage();
  }

  private <T> T getParentCause(Throwable t, Class<T> type) {
    if (t == null || type.isInstance(t)) return type.cast(t);
    return getParentCause(t.getCause(), type);
  }
}
