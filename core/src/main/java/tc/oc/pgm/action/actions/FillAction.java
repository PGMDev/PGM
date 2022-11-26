package tc.oc.pgm.action.actions;

import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.material.MaterialData;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.region.Region;
import tc.oc.pgm.filters.query.BlockQuery;

public class FillAction extends AbstractAction<Match> {

  private final Region region;
  private final MaterialData materialData;
  private final @Nullable Filter filter;
  private final boolean events;

  public FillAction(
      Region region, MaterialData materialData, @Nullable Filter filter, boolean events) {
    super(Match.class);
    this.region = region;
    this.materialData = materialData;
    this.filter = filter;
    this.events = events;
  }

  @Override
  public void trigger(Match match) {
    for (Block block : region.getBlocks(match.getWorld())) {
      if (filter != null && filter.query(new BlockQuery(block)).isDenied()) continue;

      BlockState newState = block.getState();
      newState.setMaterialData(materialData);

      if (events) {
        BlockFormEvent event = new BlockFormEvent(block, newState);
        match.callEvent(event);
        if (event.isCancelled()) continue;
      }

      newState.update(true, true);
    }
  }
}
