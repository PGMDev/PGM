package tc.oc.pgm.action.actions;

import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.material.MaterialData;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.region.Region;

public class FillAction extends AbstractAction<Match> {

  private final Region region;
  private final MaterialData materialData;
  private final boolean events;

  public FillAction(Region region, MaterialData materialData, boolean events) {
    super(Match.class);
    this.region = region;
    this.materialData = materialData;
    this.events = events;
  }

  @Override
  public void trigger(Match match) {
    for (Block block : region.getBlocks(match.getWorld())) {
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
