package tc.oc.pgm.blockdrops;

import static tc.oc.pgm.util.nms.NMSHacks.NMS_HACKS;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BlockVector;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.event.BlockTransformEvent;
import tc.oc.pgm.api.filter.query.Query;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.api.region.Region;
import tc.oc.pgm.filters.matcher.StaticFilter;
import tc.oc.pgm.filters.query.Queries;
import tc.oc.pgm.kits.Kit;
import tc.oc.pgm.kits.KitNode;
import tc.oc.pgm.regions.Bounds;
import tc.oc.pgm.regions.FiniteBlockRegion;
import tc.oc.pgm.util.block.BlockStates;
import tc.oc.pgm.util.event.PlayerPunchBlockEvent;
import tc.oc.pgm.util.event.PlayerTrampleBlockEvent;
import tc.oc.pgm.util.material.BlockMaterialData;
import tc.oc.pgm.util.material.MaterialData;

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
  public BlockDropsRuleSet subsetAffecting(Match match, FiniteBlockRegion region) {
    ImmutableList.Builder<BlockDropsRule> subset = ImmutableList.builder();
    for (BlockDropsRule rule : this.rules) {
      if (rule.region == null) {
        subset.add(rule);
        continue;
      }

      Region.Static staticReg = region.getStatic(match);
      if (Bounds.disjoint(staticReg.getBounds(), region.getBounds())) continue;

      for (BlockVector block : region.getBlockVectors()) {
        if (staticReg.contains(block)) {
          subset.add(rule);
          break;
        }
      }
    }

    return new BlockDropsRuleSet(subset.build());
  }

  public BlockDrops getDrops(BlockState block, BlockMaterialData material) {
    return this.getDrops(null, block, material, null);
  }

  public BlockDrops getDrops(@Nullable Event event, BlockState block, ParticipantState player) {
    return this.getDrops(event, block, MaterialData.block(block), player);
  }

  public BlockDrops getDrops(
      @Nullable Event event,
      BlockState block,
      BlockMaterialData material,
      @Nullable ParticipantState playerState) {
    Map<ItemStack, Double> items = new LinkedHashMap<>();
    List<Kit> kits = new ArrayList<>();
    BlockMaterialData replacement = null;
    Float fallChance = null;
    Float landChance = null;
    double fallSpeed = 1;
    int experience = 0;
    boolean custom = false;
    block = BlockStates.cloneWithMaterial(block.getBlock(), material);

    boolean rightToolUsed = true;
    if (event instanceof BlockTransformEvent transformEvent) {
      Entity actor = transformEvent.getActor();
      if (actor instanceof Player p) {
        rightToolUsed = NMS_HACKS.canMineBlock(material, p);
      }
    } else if (event instanceof BlockBreakEvent breakEvent) {
      rightToolUsed = NMS_HACKS.canMineBlock(material, breakEvent.getPlayer());
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
