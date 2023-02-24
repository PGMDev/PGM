package tc.oc.pgm.killreward;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import org.bukkit.inventory.ItemStack;
import org.jdom2.Document;
import org.jdom2.Element;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.map.factory.MapModuleFactory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.filters.FilterModule;
import tc.oc.pgm.filters.matcher.StaticFilter;
import tc.oc.pgm.filters.matcher.player.KillStreakFilter;
import tc.oc.pgm.itemmeta.ItemModifyModule;
import tc.oc.pgm.kits.Kit;
import tc.oc.pgm.kits.KitModule;
import tc.oc.pgm.kits.KitNode;
import tc.oc.pgm.regions.RegionModule;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.XMLUtils;

public class KillRewardModule implements MapModule<KillRewardMatchModule> {
  protected final ImmutableList<KillReward> rewards;

  public KillRewardModule(List<KillReward> rewards) {
    this.rewards = ImmutableList.copyOf(rewards);
  }

  @Override
  public KillRewardMatchModule createMatchModule(Match match) {
    return new KillRewardMatchModule(match, this.rewards);
  }

  public static class Factory implements MapModuleFactory<KillRewardModule> {
    @Override
    public Collection<Class<? extends MapModule<?>>> getWeakDependencies() {
      return ImmutableList.of(RegionModule.class, FilterModule.class, ItemModifyModule.class);
    }

    @Override
    public Collection<Class<? extends MapModule<?>>> getSoftDependencies() {
      return ImmutableList.of(KitModule.class);
    }

    @Override
    public KillRewardModule parse(MapFactory factory, Logger logger, Document doc)
        throws InvalidXMLException {
      ImmutableList.Builder<KillReward> rewards = ImmutableList.builder();
      final Optional<ItemModifyModule> itemModifier =
          Optional.ofNullable(factory.getModule(ItemModifyModule.class));

      // Must allow top-level children for legacy support
      for (Element elKillReward :
          XMLUtils.flattenElements(
              doc.getRootElement(),
              ImmutableSet.of("kill-rewards", "killrewards"),
              ImmutableSet.of("kill-reward", "killreward"),
              0)) {
        ImmutableList.Builder<ItemStack> items = ImmutableList.builder();
        for (Element itemEl : elKillReward.getChildren("item")) {
          final ItemStack itemStack = factory.getKits().parseItem(itemEl, false);
          itemModifier.ifPresent(imm -> imm.applyRules(itemStack));
          items.add(itemStack);
        }

        Filter filter =
            factory.getFilters().parseFilterProperty(elKillReward, "filter", StaticFilter.ALLOW);
        Kit kit = factory.getKits().parseKitProperty(elKillReward, "kit", KitNode.EMPTY);

        rewards.add(new KillReward(items.build(), filter, kit));
      }

      ImmutableList<KillReward> list = rewards.build();
      if (list.isEmpty()
          && !factory.getFeatures().getAll(KillStreakFilter.class).iterator().hasNext()) {
        return null;
      } else {
        return new KillRewardModule(list);
      }
    }
  }

  @Override
  public void postParse(MapFactory factory, Logger logger, Document doc)
      throws InvalidXMLException {
    // Apply any item-mods to all reward items
    ItemModifyModule imm = factory.getModule(ItemModifyModule.class);
    if (imm != null) {
      for (KillReward reward : rewards) {
        for (ItemStack stack : reward.items) {
          imm.applyRules(stack);
        }
      }
    }
  }
}
