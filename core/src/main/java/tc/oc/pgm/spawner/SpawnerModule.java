package tc.oc.pgm.spawner;

import com.google.common.collect.ImmutableList;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionType;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.map.factory.MapModuleFactory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.region.Region;
import tc.oc.pgm.filters.FilterModule;
import tc.oc.pgm.filters.FilterParser;
import tc.oc.pgm.filters.StaticFilter;
import tc.oc.pgm.kits.KitParser;
import tc.oc.pgm.regions.RegionModule;
import tc.oc.pgm.regions.RegionParser;
import tc.oc.pgm.spawner.objects.SpawnableItem;
import tc.oc.pgm.spawner.objects.SpawnablePotion;
import tc.oc.pgm.util.xml.InheritingElement;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.XMLUtils;

public class SpawnerModule implements MapModule {

  private final List<SpawnerDefinition> spawnerDefinitions = new ArrayList<>();

  @Override
  public MatchModule createMatchModule(Match match) {
    return new SpawnerMatchModule(match, spawnerDefinitions);
  }

  public static class Factory implements MapModuleFactory<SpawnerModule> {
    @Override
    public Collection<Class<? extends MapModule>> getWeakDependencies() {
      return ImmutableList.of(RegionModule.class, FilterModule.class);
    }

    @Override
    public SpawnerModule parse(MapFactory factory, Logger logger, Document doc)
        throws InvalidXMLException {
      SpawnerModule spawnerModule = new SpawnerModule();
      RegionParser regionParser = factory.getRegions();
      KitParser kitParser = factory.getKits();
      FilterParser filterParser = factory.getFilters();
      AtomicInteger numericId = new AtomicInteger(0);

      for (Element spawnerEl :
          XMLUtils.flattenElements(doc.getRootElement(), "spawners", "spawner")) {
        Region spawnRegion = regionParser.parseRequiredRegionProperty(spawnerEl, "spawn-region");
        Region playerRegion = regionParser.parseRequiredRegionProperty(spawnerEl, "player-region");
        Attribute delayAttr = spawnerEl.getAttribute("delay");
        Attribute minDelayAttr = spawnerEl.getAttribute("min-delay");
        Attribute maxDelayAttr = spawnerEl.getAttribute("max-delay");

        if ((minDelayAttr != null || maxDelayAttr != null) && delayAttr != null) {
          throw new InvalidXMLException(
              "Attribute 'minDelay' and 'maxDelay' cannot be combined with 'delay'", spawnerEl);
        }

        Duration delay = XMLUtils.parseDuration(delayAttr, Duration.ofSeconds(10));
        Duration minDelay = XMLUtils.parseDuration(minDelayAttr, delay);
        Duration maxDelay = XMLUtils.parseDuration(maxDelayAttr, delay);

        if (maxDelay.compareTo(minDelay) <= 0 && minDelayAttr != null && maxDelayAttr != null) {
          throw new InvalidXMLException("Max-delay must be longer than min-delay", spawnerEl);
        }

        int maxEntities =
            XMLUtils.parseNumber(
                spawnerEl.getAttribute("max-entities"), Integer.class, Integer.MAX_VALUE);
        Filter playerFilter =
            filterParser.parseFilterProperty(spawnerEl, "filter", StaticFilter.ALLOW);

        List<Spawnable> objects = new ArrayList<>();
        for (Element itemEl : XMLUtils.getChildren(spawnerEl, "item")) {
          ItemStack stack = kitParser.parseItem(itemEl, false);
          SpawnableItem item = new SpawnableItem(stack, "spawner-" + numericId.get());
          objects.add(item);
        }

        ImmutableList.Builder<PotionEffect> chBuilder = ImmutableList.builder();
        for (Element potionEl : XMLUtils.getChildren(spawnerEl, "potion")) {
          for (Element potionChild : potionEl.getChildren("effect")) {
            chBuilder.add(XMLUtils.parsePotionEffect(new InheritingElement(potionChild)));
          }
          ImmutableList<PotionEffect> potionChildren = chBuilder.build();
          if (potionChildren.isEmpty()) {
            throw new InvalidXMLException("Expected child effects, but found none", spawnerEl);
          }
          int potionName = 0;
          if (potionEl.getAttribute("damage") != null) {
            potionName = XMLUtils.parseNumber(potionEl.getAttribute("damage"), Integer.class, 0);
          } else {
            for (PotionEffect potionEffect : potionChildren) {
              // PotionType lists "true" potions, PotionEffectType lists all possible status effects
              // (ie wither)
              // Use the first listed PotionType for potion color
              if (PotionType.getByEffect(potionEffect.getType()) != null) {
                potionName = PotionType.getByEffect(potionEffect.getType()).getDamageValue();
                break;
              }
            }
          }
          objects.add(
              new SpawnablePotion(potionChildren, potionName, "spawner-" + numericId.get()));
        }

        SpawnerDefinition spawnerDefinition =
            new SpawnerDefinition(
                numericId.getAndIncrement(),
                objects,
                spawnRegion,
                playerRegion,
                playerFilter,
                delay,
                minDelay,
                maxDelay,
                maxEntities);
        factory.getFeatures().addFeature(spawnerEl, spawnerDefinition);
        spawnerModule.spawnerDefinitions.add(spawnerDefinition);
      }

      return spawnerModule.spawnerDefinitions.isEmpty() ? null : spawnerModule;
    }
  }
}
