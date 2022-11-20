package tc.oc.pgm.command.parsers;

import static cloud.commandframework.arguments.parser.ArgumentParseResult.failure;
import static cloud.commandframework.arguments.parser.ArgumentParseResult.success;
import static tc.oc.pgm.util.text.TextException.exception;

import cloud.commandframework.arguments.parser.ArgumentParseResult;
import cloud.commandframework.arguments.parser.ArgumentParser;
import cloud.commandframework.context.CommandContext;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.NonNull;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.map.MapOrder;
import tc.oc.pgm.command.util.CommandKeys;
import tc.oc.pgm.rotation.MapPoolManager;
import tc.oc.pgm.rotation.pools.MapPool;
import tc.oc.pgm.rotation.pools.MapPoolType;
import tc.oc.pgm.util.LiquidMetal;
import tc.oc.pgm.util.StringUtils;

public final class MapPoolParser implements ArgumentParser<CommandSender, MapPool> {

  @Override
  public @NonNull ArgumentParseResult<MapPool> parse(
      final @NonNull CommandContext<CommandSender> context,
      final @NonNull Queue<String> inputQueue) {
    MapOrder mapOrder = PGM.get().getMapOrder();
    if (!(mapOrder instanceof MapPoolManager)) return failure(exception("pool.mapPoolsDisabled"));

    String input = inputQueue.peek();

    MapPool pool =
        StringUtils.bestFuzzyMatch(
            input, getPools(context, (MapPoolManager) mapOrder).iterator(), MapPool::getName);
    if (pool == null) return failure(exception("pool.noPoolMatch"));
    inputQueue.remove();
    return success(pool);
  }

  @Override
  public @NonNull List<@NonNull String> suggestions(
      final @NonNull CommandContext<CommandSender> context, final @NonNull String input) {
    MapOrder mapOrder = PGM.get().getMapOrder();
    if (!(mapOrder instanceof MapPoolManager)) return Collections.emptyList();

    return getPools(context, (MapPoolManager) mapOrder)
        .map(MapPool::getName)
        .filter(name -> LiquidMetal.match(name, input))
        .collect(Collectors.toList());
  }

  private Stream<MapPool> getPools(CommandContext<CommandSender> context, MapPoolManager manager) {
    Optional<MapPoolType> type = context.getOptional(CommandKeys.POOL_TYPE);
    return manager
        .getMapPoolStream()
        .filter(mp -> type.map(filter -> filter == mp.getType()).orElse(true));
  }
}
