package tc.oc.pgm.action.actions;

import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.event.block.BlockFormEvent;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.region.Region;
import tc.oc.pgm.filters.query.BlockQuery;
import tc.oc.pgm.util.material.BlockMaterialData;

public class FillAction extends AbstractAction<Match> {

  private final Region region;
  private final BlockMaterialData materialData;
  private final @Nullable Filter filter;
  private final boolean update;
  private final boolean events;

  public FillAction(
      Region region,
      BlockMaterialData materialData,
      @Nullable Filter filter,
      boolean update,
      boolean events) {
    super(Match.class);
    this.region = region;
    this.materialData = materialData;
    this.filter = filter;
    this.update = update;
    this.events = events;
  }

  @Override
  public void trigger(Match match) {
    for (Block block : region.getBlocks(match.getWorld())) {
      if (filter != null && filter.query(new BlockQuery(block)).isDenied()) continue;

      if (!events) {
        materialData.applyTo(block, update);
      } else {
        BlockState newState = block.getState();
        materialData.applyTo(newState);

        BlockFormEvent event = new BlockFormEvent(block, newState);
        match.callEvent(event);
        if (event.isCancelled()) continue;
        newState.update(true, update);
      }
    }
  }
}
