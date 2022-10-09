package tc.oc.pgm.filters.query;

import org.bukkit.block.BlockState;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.filter.query.BlockQuery;
import tc.oc.pgm.api.player.ParticipantState;

public class Queries {
  private Queries() {}

  public static BlockQuery block(
      @Nullable Event event, @Nullable ParticipantState player, BlockState block) {
    return player == null
        ? new tc.oc.pgm.filters.query.BlockQuery(event, block)
        : new PlayerBlockQuery(event, player, block);
  }
}
