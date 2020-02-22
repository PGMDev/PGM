package tc.oc.pgm.blockdrops;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;
import tc.oc.pgm.api.event.BlockPunchEvent;
import tc.oc.pgm.api.event.BlockTrampleEvent;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.filters.Filter;
import tc.oc.pgm.filters.query.IQuery;
import tc.oc.pgm.filters.query.MaterialQuery;
import tc.oc.pgm.filters.query.Queries;
import tc.oc.pgm.regions.FiniteBlockRegion;
import tc.oc.util.Pair;
import tc.oc.util.bukkit.block.BlockStates;
import tc.oc.util.bukkit.world.NMSHacks;

public class BlockDropsRuleSet {
  private final ImmutableList<BlockDropsRule> rules;

  public BlockDropsRuleSet() {
    this(ImmutableList.<BlockDropsRule>of());
  }

  public BlockDropsRuleSet(List<BlockDropsRule> rules) {
    this.rules = ImmutableList.copyOf(rules);
  }

  public boolean isEmpty() {
    return this.rules.isEmpty();
  }

  public ImmutableList<BlockDropsRule> getRules() {
    return this.rules;
  }

  /** Return the subset of rules that may act on the given region */
  public BlockDropsRuleSet subsetAffecting(FiniteBlockRegion region) {
    ImmutableList.Builder<BlockDropsRule> subset = ImmutableList.builder();
    for (BlockDropsRule rule : this.rules) {
      for (Block block : region.getBlocks()) {
        if (rule.region == null || rule.region.contains(block)) {
          subset.add(rule);
          break;
        }
      }
    }

    return new BlockDropsRuleSet(subset.build());
  }

  /** Return the subset of rules that may act on any of the given world */
  public BlockDropsRuleSet subsetAffecting(Set<MaterialData> materials) {
    ImmutableList.Builder<BlockDropsRule> subset = ImmutableList.builder();
    for (BlockDropsRule rule : this.rules) {
      for (MaterialData material : materials) {
        if (rule.filter == null
            || rule.filter.query(MaterialQuery.get(material)) != Filter.QueryResponse.DENY) {
          subset.add(rule);
          break;
        }
      }
    }

    return new BlockDropsRuleSet(subset.build());
  }

  public BlockDrops getDrops(BlockState block, MaterialData material) {
    return this.getDrops(null, block, material, null);
  }

  public BlockDrops getDrops(@Nullable Event event, BlockState block, ParticipantState player) {
    return this.getDrops(event, block, block.getData(), player);
  }

  public BlockDrops getDrops(
      @Nullable Event event,
      BlockState block,
      MaterialData material,
      @Nullable ParticipantState playerState) {
    List<Pair<Double, ItemStack>> items = new ArrayList<>();
    MaterialData replacement = null;
    Float fallChance = null;
    Float landChance = null;
    double fallSpeed = 1;
    int experience = 0;
    boolean custom = false;
    block = BlockStates.cloneWithMaterial(block.getBlock(), material);

    boolean rightToolUsed;
    if (event instanceof BlockBreakEvent) {
      rightToolUsed =
          NMSHacks.canMineBlock(material, ((BlockBreakEvent) event).getPlayer().getItemInHand());
      ;
    } else {
      rightToolUsed = true;
    }

    for (BlockDropsRule rule : this.rules) {
      if (event instanceof BlockPunchEvent && !rule.punch) continue;
      if (event instanceof BlockTrampleEvent && !rule.trample) continue;
      if (rule.region != null && !rule.region.contains(block)) continue;

      if (rule.filter != null) {
        IQuery query = Queries.block(event, playerState, block);
        if (!rule.filter.query(query).isAllowed()) continue;
      }

      custom = true;

      if (rule.drops.replacement != null) {
        replacement = rule.drops.replacement;
      }

      if (rule.drops.fallChance != null) {
        fallChance = rule.drops.fallChance;
      }

      if (rule.drops.landChance != null) {
        landChance = rule.drops.landChance;
      }

      if (rule.drops.fallSpeed != null) {
        fallSpeed = rule.drops.fallSpeed;
      }

      if (rule.dropOnWrongTool || rightToolUsed) {
        items.addAll(rule.drops.items);
        experience += rule.drops.experience;
      }
    }

    return custom
        ? new BlockDrops(items, experience, replacement, fallChance, landChance, fallSpeed)
        : null;
  }
}
