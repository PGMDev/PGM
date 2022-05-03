package tc.oc.pgm.spawner;

import com.google.common.collect.ImmutableList;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
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
import tc.oc.pgm.util.TimeUtils;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;
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

      int numericId = 0;
      for (Element element :
          XMLUtils.flattenElements(doc.getRootElement(), "spawners", "spawner")) {
        Region spawnRegion = regionParser.parseRequiredRegionProperty(element, "spawn-region");
        Region playerRegion = regionParser.parseRequiredRegionProperty(element, "player-region");
        Attribute delayAttr = element.getAttribute("delay");
        Attribute minDelayAttr = element.getAttribute("min-delay");
        Attribute maxDelayAttr = element.getAttribute("max-delay");

        if ((minDelayAttr != null || maxDelayAttr != null) && delayAttr != null) {
          throw new InvalidXMLException(
              "Attribute 'minDelay' and 'maxDelay' cannot be combined with 'delay'", element);
        }

        Duration delay = XMLUtils.parseDuration(delayAttr, Duration.ofSeconds(10));
        Duration minDelay = XMLUtils.parseDuration(minDelayAttr, delay);
        Duration maxDelay = XMLUtils.parseDuration(maxDelayAttr, delay);

        if (maxDelay.compareTo(minDelay) <= 0 && minDelayAttr != null && maxDelayAttr != null) {
          throw new InvalidXMLException("Max-delay must be longer than min-delay", element);
        }

        int maxEntities =
            XMLUtils.parseNumber(
                element.getAttribute("max-entities"), Integer.class, Integer.MAX_VALUE);
        Filter playerFilter =
            filterParser.parseFilterProperty(element, "filter", StaticFilter.ALLOW);

        List<Spawnable> objects = new ArrayList<>();
        for (Element spawnable :
            XMLUtils.getChildren(
                element, "item")) { // TODO Add more types of spawnables once entity parser is built
          ItemStack stack = kitParser.parseItem(spawnable, false);
          SpawnableItem item = new SpawnableItem(stack, numericId);
          objects.add(item);
        }

        List<PotionEffect> thrownPotion = new ArrayList<>();
        for (Element spawnable : XMLUtils.getChildren(element, "potion")) {
          // if <potion> mentions amplifier or duration attribute, merge it with lower <effect>
          Duration duration = null;
          Integer amplifier = null;
          if (spawnable.getAttribute("duration") != null) {
            duration = XMLUtils.parseSecondDuration(Node.fromAttr(spawnable, "duration"));
          }
          if (spawnable.getAttribute("amplifier") != null) {
            amplifier = XMLUtils.parseNumber(spawnable.getAttribute("amplifier"), Integer.class) - 1;
          }
          for (Element effectEl : XMLUtils.getChildren(spawnable, "effect")) {
            Attribute presetDuration = effectEl.getAttribute("duration");
            Attribute presetAmplifier = effectEl.getAttribute("amplifier");
            PotionEffect presetPotion = XMLUtils.parsePotionEffect(effectEl);
            if (duration == null && amplifier == null && presetDuration != null) {
              thrownPotion.add(XMLUtils.parsePotionEffect(effectEl));
            } else if (duration != null) {
              // if <effect> mentions duration and does not mention amplifier
              if (presetDuration != null && presetAmplifier == null) {
                PotionEffect potionEffect =
                    new PotionEffect(
                        presetPotion.getType(),
                        (int) TimeUtils.toTicks(XMLUtils.parseSecondDuration(Node.fromAttr(effectEl, "duration"))),
                        presetPotion.getAmplifier());
                thrownPotion.add(potionEffect);
              }
              // if <effect> does not have duration and has amplifier
              else if (presetDuration == null && presetAmplifier != null) {
                PotionEffect potionEffect =
                    new PotionEffect(
                        presetPotion.getType(),
                        (int) TimeUtils.toTicks(duration),
                        XMLUtils.parseNumber(presetAmplifier, Integer.class));
                thrownPotion.add(potionEffect);
              }
              // if <effect> has neither attributes
              else if (presetDuration == null && amplifier != null) {
                PotionEffect potionEffect =
                    new PotionEffect(
                        presetPotion.getType(), (int) TimeUtils.toTicks(duration), amplifier);
                thrownPotion.add(potionEffect);
              }
            } else {
              throw new InvalidXMLException(
                  "<effect> must have a duration assigned to it by a duration attribute or <potion>",
                  effectEl);
            }
          }
          objects.add(new SpawnablePotion(thrownPotion, numericId));
        }

        SpawnerDefinition spawnerDefinition =
            new SpawnerDefinition(
                objects,
                spawnRegion,
                playerRegion,
                playerFilter,
                delay,
                minDelay,
                maxDelay,
                maxEntities,
                numericId);
        factory.getFeatures().addFeature(element, spawnerDefinition);
        spawnerModule.spawnerDefinitions.add(spawnerDefinition);
        numericId++;
      }

      return spawnerModule.spawnerDefinitions.isEmpty() ? null : spawnerModule;
    }
  }
}
