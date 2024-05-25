package tc.oc.pgm.command.util;

import static net.kyori.adventure.text.Component.text;
import static tc.oc.pgm.util.text.TextException.noPermission;
import static tc.oc.pgm.util.text.TextException.playerOnly;
import static tc.oc.pgm.util.text.TextException.unknown;
import static tc.oc.pgm.util.text.TextException.usage;

import io.leangen.geantyref.TypeToken;
import java.lang.reflect.Type;
import java.util.Locale;
import java.util.function.Function;
import java.util.function.Supplier;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.util.ComponentMessageThrowable;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.annotations.AnnotationParser;
import org.incendo.cloud.bukkit.CloudBukkitCapabilities;
import org.incendo.cloud.exception.ArgumentParseException;
import org.incendo.cloud.exception.CommandExecutionException;
import org.incendo.cloud.exception.InvalidCommandSenderException;
import org.incendo.cloud.exception.InvalidSyntaxException;
import org.incendo.cloud.exception.NoPermissionException;
import org.incendo.cloud.exception.handling.ExceptionContext;
import org.incendo.cloud.exception.parsing.ParserException;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.injection.ParameterInjector;
import org.incendo.cloud.injection.ParameterInjectorRegistry;
import org.incendo.cloud.minecraft.extras.MinecraftHelp;
import org.incendo.cloud.paper.LegacyPaperCommandManager;
import org.incendo.cloud.parser.ArgumentParser;
import org.incendo.cloud.parser.ParserRegistry;
import org.incendo.cloud.setting.ManagerSetting;
import org.incendo.cloud.suggestion.FilteringSuggestionProcessor;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.util.Audience;

public abstract class CommandGraph<P extends Plugin> {

  protected P plugin;
  protected CommandManager<CommandSender> manager;
  protected MinecraftHelp<CommandSender> minecraftHelp;
  protected AnnotationParser<CommandSender> annotationParser;

  protected ParameterInjectorRegistry<CommandSender> injectors;
  protected ParserRegistry<CommandSender> parsers;

  public CommandGraph(P plugin) throws Exception {
    this.plugin = plugin;
    this.manager = createCommandManager();
    this.minecraftHelp = createHelp();
    // Allows us to require certain commands to be confirmed before they can be executed
    this.annotationParser = createAnnotationParser();

    // Utility
    this.injectors = manager.parameterInjectorRegistry();
    this.parsers = manager.parserRegistry();

    setupExceptionHandlers();
    setupInjectors();
    setupParsers();
    registerCommands();
  }

  protected CommandManager<CommandSender> createCommandManager() {
    LegacyPaperCommandManager<CommandSender> manager =
        LegacyPaperCommandManager.createNative(plugin, ExecutionCoordinator.simpleCoordinator());

    manager.settings().set(ManagerSetting.LIBERAL_FLAG_PARSING, true);

    // Register Brigadier mappings
    if (manager.hasCapability(CloudBukkitCapabilities.BRIGADIER)) manager.registerBrigadier();

    // Register asynchronous completions
    if (manager.hasCapability(CloudBukkitCapabilities.ASYNCHRONOUS_COMPLETION))
      manager.registerAsynchronousCompletions();

    // Basic suggestion filtering processor which avoids suggesting flags when not applicable
    manager.suggestionProcessor(
        new FilteringSuggestionProcessor<>(
            FilteringSuggestionProcessor.Filter.Simple.contextFree(
                (s, i) ->
                    i.isEmpty()
                        || !s.startsWith("-")
                        || s.toLowerCase(Locale.ROOT).startsWith(i.toLowerCase(Locale.ROOT)))));

    return manager;
  }

  protected AnnotationParser<CommandSender> createAnnotationParser() {
    return new AnnotationParser<>(manager, CommandSender.class);
  }

  protected abstract MinecraftHelp<CommandSender> createHelp();

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
    registerExceptionHandler(InvalidSyntaxException.class, e -> usage(e.correctSyntax()));
    registerExceptionHandler(InvalidCommandSenderException.class, e -> playerOnly());
    registerExceptionHandler(NoPermissionException.class, e -> noPermission());

    manager
        .exceptionController()
        .registerHandler(ArgumentParseException.class, this::handleException);
    manager
        .exceptionController()
        .registerHandler(CommandExecutionException.class, this::handleException);
  }

  protected <E extends Exception> void registerExceptionHandler(
      Class<E> ex, Function<E, ComponentLike> toComponent) {
    manager
        .exceptionController()
        .registerHandler(
            ex,
            (c) ->
                Audience.get(c.context().sender()).sendWarning(toComponent.apply(c.exception())));
  }

  protected <E extends Exception> void handleException(ExceptionContext<CommandSender, E> context) {
    Audience audience = Audience.get(context.context().sender());
    Component message = getMessage(context.exception());
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
