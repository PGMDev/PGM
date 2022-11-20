package tc.oc.pgm.command.parsers;

import cloud.commandframework.arguments.parser.ArgumentParseResult;
import cloud.commandframework.arguments.parser.ArgumentParser;
import cloud.commandframework.context.CommandContext;
import java.util.List;
import java.util.Queue;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.NonNull;
import tc.oc.pgm.command.util.CommandKeys;
import tc.oc.pgm.rotation.pools.MapPoolType;
import tc.oc.pgm.rotation.pools.Rotation;

public final class RotationParser implements ArgumentParser<CommandSender, Rotation> {

  private final MapPoolParser POOL_PARSER = new MapPoolParser();

  @Override
  public @NonNull ArgumentParseResult<Rotation> parse(
      final @NonNull CommandContext<CommandSender> context,
      final @NonNull Queue<String> inputQueue) {
    context.set(CommandKeys.POOL_TYPE, MapPoolType.ORDERED);
    return POOL_PARSER.parse(context, inputQueue).mapParsedValue(p -> (Rotation) p);
  }

  @Override
  public @NonNull List<@NonNull String> suggestions(
      final @NonNull CommandContext<CommandSender> context, final @NonNull String input) {
    context.set(CommandKeys.POOL_TYPE, MapPoolType.ORDERED);
    return POOL_PARSER.suggestions(context, input);
  }
}
