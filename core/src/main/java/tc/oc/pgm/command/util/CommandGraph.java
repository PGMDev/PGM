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
import io.leangen.geantyref.TypeToken;
import java.lang.reflect.Type;
import java.util.function.Function;
import java.util.function.Supplier;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.util.ComponentMessageThrowable;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.util.Audience;

public abstract class CommandGraph<P extends Plugin> {

  protected P plugin;
  protected PaperCommandManager<CommandSender> manager;
  protected MinecraftHelp<CommandSender> minecraftHelp;
  protected CommandConfirmationManager<CommandSender> confirmationManager;
  protected AnnotationParser<CommandSender> annotationParser;

  protected ParameterInjectorRegistry<CommandSender> injectors;
  protected ParserRegistry<CommandSender> parsers;

  public CommandGraph(P plugin) throws Exception {
    this.plugin = plugin;
    this.manager = createCommandManager();
    this.minecraftHelp = createHelp();
    // Allows us to require certain commands to be confirmed before they can be executed
    this.confirmationManager = createConfirmationManager();
    this.annotationParser = createAnnotationParser();

    // Utility
    this.injectors = manager.parameterInjectorRegistry();
    this.parsers = manager.parserRegistry();

    setupExceptionHandlers();
    setupInjectors();
    setupParsers();
    registerCommands();
  }

  protected PaperCommandManager<CommandSender> createCommandManager() throws Exception {
    PaperCommandManager<CommandSender> manager =
        PaperCommandManager.createNative(plugin, CommandExecutionCoordinator.simpleCoordinator());

    manager.setSetting(CommandManager.ManagerSettings.LIBERAL_FLAG_PARSING, true);

    // Register Brigadier mappings
    if (manager.hasCapability(CloudBukkitCapabilities.BRIGADIER)) manager.registerBrigadier();

    // Register asynchronous completions
    if (manager.hasCapability(CloudBukkitCapabilities.ASYNCHRONOUS_COMPLETION))
      manager.registerAsynchronousCompletions();

    // Add the input queue to the context, this allows greedy suggestions to work on it
    manager.registerCommandPreProcessor(
        context ->
            context.getCommandContext().store(CommandKeys.INPUT_QUEUE, context.getInputQueue()));

    // By default, suggestions run by a filtered processor.
    // By default, it prevents suggestions like "s" -> "Something" or "someh" -> "Something"
    manager.commandSuggestionProcessor((cpc, strings) -> strings);

    return manager;
  }

  protected AnnotationParser<CommandSender> createAnnotationParser() {
    final Function<ParserParameters, CommandMeta> commandMetaFunction =
        p ->
            CommandMeta.simple()
                .with(
                    CommandMeta.DESCRIPTION,
                    p.get(StandardParameters.DESCRIPTION, "No description"))
                .build();
    return new AnnotationParser<>(manager, CommandSender.class, commandMetaFunction);
  }

  protected abstract MinecraftHelp<CommandSender> createHelp();

  protected abstract CommandConfirmationManager<CommandSender> createConfirmationManager();

  protected abstract void setupInjectors();

  protected abstract void setupParsers();

  protected abstract void registerCommands();

  // Commands
  protected void register(Object command) {
    annotationParser.parse(command);
  }

  // Injectors
  protected <T> void registerInjector(Class<T> type, ParameterInjector<CommandSender, T> provider) {
    injectors.registerInjector(type, provider);
  }

  protected <T> void registerInjector(Class<T> type, Supplier<T> supplier) {
    registerInjector(type, (a, b) -> supplier.get());
  }

  protected <T> void registerInjector(Class<T> type, Function<P, T> function) {
    registerInjector(type, (a, b) -> function.apply(plugin));
  }

  // Parsers
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

  protected <E extends Exception> void handleException(CommandSender cs, E e) {
    Audience audience = Audience.get(cs);
    Component message = getMessage(e);
    if (message != null) audience.sendWarning(message);
  }

  protected @Nullable Component getMessage(Throwable t) {
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
