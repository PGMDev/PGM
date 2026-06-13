package tc.oc.pgm.pickup;

import com.google.common.collect.ImmutableList;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.map.factory.MapModuleFactory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.region.Region;
import tc.oc.pgm.entity.SpawnableEntity;
import tc.oc.pgm.filters.FilterModule;
import tc.oc.pgm.kits.Kit;
import tc.oc.pgm.kits.KitModule;
import tc.oc.pgm.kits.KitNode;
import tc.oc.pgm.regions.RegionModule;
import tc.oc.pgm.util.bukkit.BukkitUtils;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;
import tc.oc.pgm.util.xml.XMLUtils;

@NullMarked
public class PickupModule implements MapModule<PickupMatchModule> {

  private final List<PickupDefinition> definitions;

  public PickupModule(List<PickupDefinition> definitions) {
    this.definitions = definitions;
  }

  @Override
  public PickupMatchModule createMatchModule(Match match) {
    List<Pickup> pickups = new ArrayList<>(definitions.size());
    for (PickupDefinition definition : definitions) {
      Pickup pickup = new Pickup(match, definition);
      if (definition.getId() != null) match.getFeatureContext().add(pickup);
      pickups.add(pickup);
    }
    return new PickupMatchModule(match, pickups);
  }

  public static class Factory implements MapModuleFactory<PickupModule> {
    @Override
    public Collection<Class<? extends MapModule<?>>> getSoftDependencies() {
      return ImmutableList.of(RegionModule.class, FilterModule.class, KitModule.class);
    }

    @Override
    public @Nullable PickupModule parse(MapFactory factory, Logger logger, Document doc)
        throws InvalidXMLException {
      List<PickupDefinition> definitions = new ArrayList<>();
      var parser = factory.getParser();

      for (Element el : XMLUtils.flattenElements(doc.getRootElement(), "pickups", "pickup")) {
        String id = parser.string(el, "id").orNull();
        String name = parser.string(el, "name").orNull();
        if (name != null) name = BukkitUtils.colorize(name);
        SpawnableEntity appearance = parseAppearance(el, factory);

        Filter spawnFilter = parser.filter(el, "spawn-filter").orAllow();
        Filter pickupFilter = parser.filter(el, "pickup-filter").orAllow();
        Region region = parser.region(el, "region").randomPoints().required();
        Kit kit = parser.kit(el, "kit").optional(KitNode.EMPTY);
        Duration respawn = parser.duration(el, "respawn-time").optional(Duration.ofSeconds(3));
        Duration cooldown = parser.duration(el, "pickup-time").optional(Duration.ofSeconds(3));
        boolean effects = parser.parseBool(el, "effects").optional(true);
        boolean sounds = parser.parseBool(el, "sounds").optional(true);

        PickupDefinition definition = new PickupDefinition(
            id,
            name,
            appearance,
            spawnFilter,
            pickupFilter,
            region,
            kit,
            respawn,
            cooldown,
            effects,
            sounds);
        factory.getFeatures().addFeature(el, definition);
        definitions.add(definition);
      }

      return definitions.isEmpty() ? null : new PickupModule(definitions);
    }

    private SpawnableEntity parseAppearance(Element el, MapFactory factory)
        throws InvalidXMLException {
      Node node = Node.fromChildOrAttr(el, "appearance");
      if (node == null) return new SpawnableEntity(EnderCrystal.class);

      SpawnableEntity appearance = node.isElement()
          ? SpawnableEntity.parse(node.getElement(), factory)
          : new SpawnableEntity(SpawnableEntity.parseType(node));

      Class<? extends Entity> type = appearance.entityType();
      if (!LivingEntity.class.isAssignableFrom(type)
          && !EnderCrystal.class.isAssignableFrom(type)) {
        throw new InvalidXMLException(
            "Pickup appearance '" + type.getSimpleName() + "' is not supported", node);
      }
      return appearance;
    }
  }
}
