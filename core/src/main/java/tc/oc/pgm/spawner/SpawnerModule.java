package tc.oc.pgm.spawner;

import com.google.common.collect.ImmutableList;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import org.bukkit.Material;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Wither;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.jdom2.Document;
import org.jdom2.Element;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.map.factory.MapModuleFactory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.region.Region;
import tc.oc.pgm.entity.SpawnableEntity;
import tc.oc.pgm.filters.FilterModule;
import tc.oc.pgm.filters.matcher.StaticFilter;
import tc.oc.pgm.regions.RegionModule;
import tc.oc.pgm.spawner.objects.SpawnableItem;
import tc.oc.pgm.spawner.objects.SpawnableMob;
import tc.oc.pgm.spawner.objects.SpawnablePotion;
import tc.oc.pgm.util.material.MaterialData;
import tc.oc.pgm.util.xml.InheritingElement;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.XMLFluentParser;
import tc.oc.pgm.util.xml.XMLUtils;

public class SpawnerModule implements MapModule<SpawnerMatchModule> {

  private static final Duration DEFAULT_DELAY = Duration.ofSeconds(10);
  private static final int SPLASH_BIT = 0x4000;
  private static final Set<Class<? extends LivingEntity>> EXCLUDED_MOB_TYPES =
      Set.of(Wither.class, EnderDragon.class);

  private final List<SpawnerDefinition> definitions;

  public SpawnerModule(List<SpawnerDefinition> definitions) {
    this.definitions = definitions;
  }

  @Override
  public SpawnerMatchModule createMatchModule(Match match) {
    List<Spawner> spawners = new ArrayList<>(this.definitions.size());
    for (SpawnerDefinition definition : this.definitions) {
      spawners.add(new Spawner(definition, match));
    }

    return new SpawnerMatchModule(match, spawners);
  }

  public static class Factory implements MapModuleFactory<SpawnerModule> {
    @Override
    public Collection<Class<? extends MapModule<?>>> getWeakDependencies() {
      return ImmutableList.of(RegionModule.class, FilterModule.class);
    }

    @Override
    public SpawnerModule parse(MapFactory factory, Logger logger, Document doc)
        throws InvalidXMLException {
      XMLFluentParser parser = factory.getParser();
      AtomicInteger spawnerIdSerial = new AtomicInteger(1);

      List<SpawnerDefinition> spawners = new ArrayList<>();
      for (Element el : XMLUtils.flattenElements(doc.getRootElement(), "spawners", "spawner")) {
        String id = parser
            .string(el, "id")
            .optional(() -> SpawnerDefinition.makeDefaultId(null, spawnerIdSerial));
        Region spawnRegion = parser.region(el, "spawn-region").randomPoints().required();
        Region playerRegion = parser.region(el, "player-region").orEverywhere();

        Duration parsedMinDelay = parser.duration(el, "min-delay").orNull();
        Duration parsedMaxDelay = parser.duration(el, "max-delay").orNull();
        Duration delay = parser
            .duration(el, "delay")
            .validate((d, n) -> {
              if (parsedMinDelay != null || parsedMaxDelay != null)
                throw new InvalidXMLException(
                    "'min-delay' and 'max-delay' cannot be combined with 'delay'", n);
            })
            .optional(DEFAULT_DELAY);

        Duration minDelay = parsedMinDelay != null ? parsedMinDelay : delay;
        Duration maxDelay = parsedMaxDelay != null ? parsedMaxDelay : delay;

        if (maxDelay.compareTo(minDelay) < 0) {
          throw new InvalidXMLException("Max-delay must be longer or equal to min-delay", el);
        }

        int maxEntities = parser.parseInt(el, "max-entities").optional(Integer.MAX_VALUE);
        Filter filter = parser.filter(el, "filter").orAllow();

        // Supporting both independently would require a proto bump; however we
        // can use respondsTo as an optimization to handle it as a match-only filter.
        boolean isMatchFilter = filter.respondsTo(Match.class);
        Filter playerFilter = isMatchFilter ? StaticFilter.ALLOW : filter;
        Filter matchFilter = isMatchFilter ? filter : StaticFilter.ALLOW;

        List<Spawnable> objects = new ArrayList<>();
        for (Element itemEl : XMLUtils.getChildren(el, "item")) {
          ItemStack stack = parser.item(itemEl).required();
          SpawnableItem item = new SpawnableItem(stack, id);
          objects.add(item);
        }

        for (Element potionEl : XMLUtils.getChildren(el, "potion")) {
          short dmg = parser.parseShort(potionEl, "damage").optional((short) 0);
          ItemStack potion =
              MaterialData.item(Material.POTION, (short) (dmg | SPLASH_BIT)).toItemStack(1);
          PotionMeta meta = (PotionMeta) potion.getItemMeta();
          for (Element potionChild : potionEl.getChildren("effect")) {
            meta.addCustomEffect(
                XMLUtils.parsePotionEffect(new InheritingElement(potionChild)), false);
          }
          if (!meta.hasCustomEffects()) {
            throw new InvalidXMLException("Expected child effects, but found none", el);
          }
          potion.setItemMeta(meta);
          objects.add(new SpawnablePotion(potion, id));
        }

        for (Element mobEl : XMLUtils.getChildren(el, "mob")) {
          objects.add(parseMob(mobEl, id, factory));
        }

        SpawnerDefinition spawnerDefinition = new SpawnerDefinition(
            id,
            objects,
            spawnRegion,
            playerRegion,
            matchFilter,
            playerFilter,
            minDelay,
            maxDelay,
            maxEntities);
        factory.getFeatures().addFeature(el, spawnerDefinition);
        spawners.add(spawnerDefinition);
      }

      return spawners.isEmpty() ? null : new SpawnerModule(spawners);
    }

    private static SpawnableMob parseMob(Element mobEl, String spawnerId, MapFactory factory)
        throws InvalidXMLException {
      var entity = SpawnableEntity.parse(mobEl, factory);
      if (EXCLUDED_MOB_TYPES.stream().anyMatch(c -> c.isAssignableFrom(entity.entityType()))) {
        throw new InvalidXMLException(
            "Spawner mob type " + entity.entityType().getSimpleName() + " cannot be spawned",
            mobEl);
      }
      return new SpawnableMob(entity, spawnerId);
    }
  }
}
