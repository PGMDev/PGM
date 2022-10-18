package tc.oc.pgm.command.parsers;

import static cloud.commandframework.arguments.parser.ArgumentParseResult.failure;
import static cloud.commandframework.arguments.parser.ArgumentParseResult.success;
import static tc.oc.pgm.util.text.TextException.exception;

import cloud.commandframework.arguments.parser.ArgumentParseResult;
import cloud.commandframework.arguments.parser.ArgumentParser;
import cloud.commandframework.context.CommandContext;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.NonNull;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.map.MapOrder;
import tc.oc.pgm.rotation.MapPoolManager;
import tc.oc.pgm.rotation.pools.MapPool;
import tc.oc.pgm.util.StringUtils;

public final class MapPoolParser implements ArgumentParser<CommandSender, MapPool> {

  @Override
  public @NonNull ArgumentParseResult<MapPool> parse(
      final @NonNull CommandContext<CommandSender> context,
      final @NonNull Queue<String> inputQueue) {
    MapOrder mapOrder = PGM.get().getMapOrder();
    if (!(mapOrder instanceof MapPoolManager)) return failure(exception("pool.mapPoolsDisabled"));

    String poolName = inputQueue.peek();

    MapPool pool = ((MapPoolManager) mapOrder).getMapPoolByName(poolName);
    if (pool == null) return failure(exception("pool.noPoolMatch"));
    inputQueue.remove();
    return success(pool);
  }

  @Override
  public @NonNull List<@NonNull String> suggestions(
      final @NonNull CommandContext<CommandSender> commandContext, final @NonNull String input) {
    MapOrder mapOrder = PGM.get().getMapOrder();
    if (!(mapOrder instanceof MapPoolManager)) return Collections.emptyList();

    String normalized = StringUtils.normalize(input);

    return ((MapPoolManager) mapOrder)
        .getMapPools().stream()
            .map(MapPool::getName)
            .filter(n -> StringUtils.normalize(n).startsWith(normalized))
            .collect(Collectors.toList());
  }
}
