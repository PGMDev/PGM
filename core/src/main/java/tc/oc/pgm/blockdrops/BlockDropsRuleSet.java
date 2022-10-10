package tc.oc.pgm.blockdrops;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bukkit.block.BlockState;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;
import org.bukkit.util.BlockVector;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.filter.query.Query;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.filters.matcher.StaticFilter;
import tc.oc.pgm.filters.query.MaterialQuery;
import tc.oc.pgm.filters.query.Queries;
import tc.oc.pgm.kits.Kit;
import tc.oc.pgm.kits.KitNode;
import tc.oc.pgm.regions.FiniteBlockRegion;
import tc.oc.pgm.util.block.BlockStates;
import tc.oc.pgm.util.event.PlayerPunchBlockEvent;
import tc.oc.pgm.util.event.PlayerTrampleBlockEvent;
import tc.oc.pgm.util.nms.NMSHacks;

public class BlockDropsRuleSet {
  private final ImmutableList<BlockDropsRule> rules;

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
      for (BlockVector block : region.getBlockVectors()) {
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
    Map<ItemStack, Double> items = new LinkedHashMap<>();
    List<Kit> kits = new ArrayList<>();
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
      if (event instanceof PlayerPunchBlockEvent && !rule.punch) continue;
      if (event instanceof PlayerTrampleBlockEvent && !rule.trample) continue;
      if (rule.region != null && !rule.region.contains(block)) continue;

      if (rule.filter != null) {
        Query query = Queries.block(event, playerState, block);
        if (!rule.filter.query(query).isAllowed()) continue;
      }

      custom = true;

      if (rule.drops.kit != null) {
        kits.add(rule.drops.kit);
      }

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
        items.putAll(rule.drops.items);
        experience += rule.drops.experience;
      }
    }

    return custom
        ? new BlockDrops(
            items,
            new KitNode(kits, StaticFilter.ALLOW, null, null),
            experience,
            replacement,
            fallChance,
            landChance,
            fallSpeed)
        : null;
  }
}
