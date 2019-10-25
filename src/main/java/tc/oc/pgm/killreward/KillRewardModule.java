package tc.oc.pgm.killreward;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.logging.Logger;
import org.bukkit.inventory.ItemStack;
import org.jdom2.Document;
import org.jdom2.Element;
import tc.oc.pgm.filters.Filter;
import tc.oc.pgm.filters.FilterModule;
import tc.oc.pgm.filters.StaticFilter;
import tc.oc.pgm.itemmeta.ItemModifyModule;
import tc.oc.pgm.kits.Kit;
import tc.oc.pgm.kits.KitModule;
import tc.oc.pgm.kits.KitNode;
import tc.oc.pgm.map.MapModule;
import tc.oc.pgm.map.MapModuleContext;
import tc.oc.pgm.match.Match;
import tc.oc.pgm.match.MatchModule;
import tc.oc.pgm.module.ModuleDescription;
import tc.oc.pgm.regions.RegionModule;
import tc.oc.pgm.util.XMLUtils;
import tc.oc.xml.InvalidXMLException;

@ModuleDescription(
    name = "Kill Reward",
    follows = {RegionModule.class, FilterModule.class},
    requires = {KitModule.class})
public class KillRewardModule extends MapModule {
  protected final ImmutableList<KillReward> rewards;

  public KillRewardModule(List<KillReward> rewards) {
    this.rewards = ImmutableList.copyOf(rewards);
  }

  @Override
  public MatchModule createMatchModule(Match match) {
    return new KillRewardMatchModule(match, this.rewards);
  }

  // ---------------------
  // ---- XML Parsing ----
  // ---------------------

  public static KillRewardModule parse(MapModuleContext context, Logger logger, Document doc)
      throws InvalidXMLException {
    ImmutableList.Builder<KillReward> rewards = ImmutableList.builder();

    // Must allow top-level children for legacy support
    for (Element elKillReward :
        XMLUtils.flattenElements(
            doc.getRootElement(),
            ImmutableSet.of("kill-rewards"),
            ImmutableSet.of("kill-reward", "killreward"),
            0)) {
      ImmutableList.Builder<ItemStack> items = ImmutableList.builder();
      for (Element itemEl : elKillReward.getChildren("item")) {
        items.add(context.getKitParser().parseItem(itemEl, false));
      }

      Filter filter =
          context.getFilterParser().parseFilterProperty(elKillReward, "filter", StaticFilter.ALLOW);
      Kit kit = context.getKitParser().parseKitProperty(elKillReward, "kit", KitNode.EMPTY);

      rewards.add(new KillReward(items.build(), filter, kit));
    }

    ImmutableList<KillReward> list = rewards.build();
    if (list.isEmpty()) {
      return null;
    } else {
      return new KillRewardModule(list);
    }
  }

  @Override
  public void postParse(MapModuleContext context, Logger logger, Document doc)
      throws InvalidXMLException {
    // Apply any item-mods to all reward items
    ItemModifyModule imm = context.getModule(ItemModifyModule.class);
    if (imm != null) {
      for (KillReward reward : rewards) {
        for (ItemStack stack : reward.items) {
          imm.applyRules(stack);
        }
      }
    }
  }
}
