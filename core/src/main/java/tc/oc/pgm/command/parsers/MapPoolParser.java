package tc.oc.pgm.command.parsers;

import static org.incendo.cloud.parser.ArgumentParseResult.failure;
import static org.incendo.cloud.parser.ArgumentParseResult.success;
import static tc.oc.pgm.util.text.TextException.exception;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.parser.ArgumentParseResult;
import org.incendo.cloud.parser.ArgumentParser;
import org.incendo.cloud.suggestion.BlockingSuggestionProvider;
import org.jetbrains.annotations.NotNull;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.map.MapOrder;
import tc.oc.pgm.command.util.CommandKeys;
import tc.oc.pgm.rotation.MapPoolManager;
import tc.oc.pgm.rotation.pools.MapPool;
import tc.oc.pgm.rotation.pools.MapPoolType;
import tc.oc.pgm.util.LiquidMetal;
import tc.oc.pgm.util.StringUtils;

public final class MapPoolParser
    implements ArgumentParser<CommandSender, MapPool>,
        BlockingSuggestionProvider.Strings<CommandSender> {

  @Override
  public @NotNull ArgumentParseResult<MapPool> parse(
      final @NotNull CommandContext<CommandSender> context,
      final @NotNull CommandInput inputQueue) {
    MapOrder mapOrder = PGM.get().getMapOrder();
    if (!(mapOrder instanceof MapPoolManager)) return failure(exception("pool.mapPoolsDisabled"));

    final String input = inputQueue.peekString();

    MapPool pool =
        StringUtils.bestFuzzyMatch(
            input, getPools(context, (MapPoolManager) mapOrder).iterator(), MapPool::getName);
    if (pool == null) return failure(exception("pool.noPoolMatch"));
    inputQueue.readString();
    return success(pool);
  }

  @Override
  public @NotNull List<@NotNull String> stringSuggestions(
      final @NotNull CommandContext<CommandSender> context, final @NotNull CommandInput input) {
    final String next = input.readString();
    MapOrder mapOrder = PGM.get().getMapOrder();
    if (!(mapOrder instanceof MapPoolManager)) return Collections.emptyList();

    return getPools(context, (MapPoolManager) mapOrder)
        .map(MapPool::getName)
        .filter(name -> LiquidMetal.match(name, next))
        .collect(Collectors.toList());
  }

  private Stream<MapPool> getPools(CommandContext<CommandSender> context, MapPoolManager manager) {
    Optional<MapPoolType> type = context.optional(CommandKeys.POOL_TYPE);
    return manager
        .getMapPoolStream()
        .filter(mp -> type.map(filter -> filter == mp.getType()).orElse(true));
  }
}
