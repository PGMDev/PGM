package tc.oc.pgm.blockdrops;

import tc.oc.pgm.filters.Filter;
import tc.oc.pgm.regions.Region;

public class BlockDropsRule {
  public final Filter filter;
  public final Region region;
  public final boolean dropOnWrongTool;
  public final boolean punch;
  public final boolean trample;
  public final BlockDrops drops;

  public BlockDropsRule(
      Filter filter,
      Region region,
      boolean dropOnWrongTool,
      boolean punch,
      boolean trample,
      BlockDrops drops) {
    this.filter = filter;
    this.region = region;
    this.dropOnWrongTool = dropOnWrongTool;
    this.punch = punch;
    this.trample = trample;
    this.drops = drops;
  }
}
