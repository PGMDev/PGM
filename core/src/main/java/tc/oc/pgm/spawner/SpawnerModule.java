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
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.map.factory.MapModuleFactory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.filters.FilterModule;
import tc.oc.pgm.filters.FilterParser;
import tc.oc.pgm.filters.StaticFilter;
import tc.oc.pgm.kits.KitParser;
import tc.oc.pgm.regions.RegionModule;
import tc.oc.pgm.regions.RegionParser;
import tc.oc.pgm.spawner.objects.SpawnerObjectEntity;
import tc.oc.pgm.spawner.objects.SpawnerObjectItem;
import tc.oc.pgm.spawner.objects.SpawnerObjectPotion;
import tc.oc.pgm.spawner.objects.SpawnerObjectTNT;
import tc.oc.pgm.util.TimeUtils;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.XMLUtils;

public class SpawnerModule implements MapModule {

  private static final List<SpawnerDefinition> spawnerDefinitions = new ArrayList<>();
  public static final String METADATA_KEY = "PGM_SPAWNER_OBJECT";

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

      for (Element element :
          XMLUtils.flattenElements(doc.getRootElement(), "spawners", "spawner")) {
        SpawnerDefinition spawnerDefinition = new SpawnerDefinition();
        spawnerDefinition.spawnRegion =
            regionParser.parseRequiredRegionProperty(element, "spawn-region");
        spawnerDefinition.playerRegion =
            regionParser.parseRequiredRegionProperty(element, "player-region");
        spawnerDefinition.id = element.getAttributeValue("id");
        Attribute delay = element.getAttribute("delay");
        Attribute minDelay = element.getAttribute("min-delay");
        Attribute maxDelay = element.getAttribute("max-delay");

        if ((minDelay != null || maxDelay != null) && delay != null) {
          throw new InvalidXMLException(
              "Attribute 'minDelay' and 'maxDelay' cannot be combined with 'delay'", element);
        }

        spawnerDefinition.delay = XMLUtils.parseDuration(delay, Duration.ofSeconds(10));
        spawnerDefinition.minDelay = XMLUtils.parseDuration(minDelay, spawnerDefinition.delay);
        spawnerDefinition.maxDelay = XMLUtils.parseDuration(maxDelay, spawnerDefinition.delay);

        if (spawnerDefinition.maxDelay.compareTo(spawnerDefinition.minDelay) < 0) {
          throw new InvalidXMLException("Max delay cannot be smaller than min delay", element);
        }

        spawnerDefinition.maxEntities =
            XMLUtils.parseNumber(
                element.getAttribute("max-entities"), Integer.class, Integer.MAX_VALUE);
        spawnerDefinition.filter = filterParser.parseFilterProperty(element, "filter", StaticFilter.ALLOW);

        List<SpawnerObject> objects = new ArrayList<>();
        for (Element object : XMLUtils.getChildren(element, "entity", "item", "tnt", "effect")) {
          int count;
          switch (object.getName()) {
            case "entity":
              count = XMLUtils.parseNumber(object.getAttribute("count"), Integer.class, 1);
              SpawnerObjectEntity entity =
                  new SpawnerObjectEntity(XMLUtils.parseEntityType(object), count);
              objects.add(entity);
              break;
            case "tnt":
              Duration fuse = XMLUtils.parseDuration(object.getAttribute("fuse"));
              float power = XMLUtils.parseNumber(object.getAttribute("power"), Float.class);
              count = XMLUtils.parseNumber(object.getAttribute("count"), Integer.class, 1);
              SpawnerObjectTNT tnt =
                  new SpawnerObjectTNT(power, (int) TimeUtils.toTicks(fuse), count);
              objects.add(tnt);
              break;
            case "effect":
              PotionEffect effect = XMLUtils.parsePotionEffect(object);
              count = XMLUtils.parseNumber(object.getAttribute("count"), Integer.class, 1);
              SpawnerObjectPotion potion = new SpawnerObjectPotion(count, effect);
              objects.add(potion);
              break;
            case "item":
              ItemStack stack = kitParser.parseItem(object, false);
              SpawnerObjectItem item = new SpawnerObjectItem(stack);
              objects.add(item);
              break;
          }
        }
        spawnerDefinition.objects = objects;
        spawnerDefinitions.add(spawnerDefinition);
      }

      return spawnerDefinitions.isEmpty() ? null : spawnerModule;
    }
  }
}
