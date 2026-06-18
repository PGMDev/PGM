package tc.oc.pgm.projectile;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import org.bukkit.Material;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Explosive;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Fireball;
import org.bukkit.potion.PotionEffect;
import org.jdom2.Document;
import org.jdom2.Element;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.map.factory.MapModuleFactory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.filters.FilterModule;
import tc.oc.pgm.projectile.projectiles.BridgeEggProjectile;
import tc.oc.pgm.projectile.projectiles.EntityProjectile;
import tc.oc.pgm.projectile.projectiles.PgmProjectile;
import tc.oc.pgm.util.MethodParser;
import tc.oc.pgm.util.MethodParsers;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;
import tc.oc.pgm.util.xml.XMLFluentParser;
import tc.oc.pgm.util.xml.XMLUtils;

public class ProjectileModule implements MapModule<ProjectileMatchModule> {
  private final ImmutableSet<ProjectileDefinition> projectileDefinitions;

  public ProjectileModule(ImmutableSet<ProjectileDefinition> projectileDefinitions) {
    this.projectileDefinitions = projectileDefinitions;
  }

  @Override
  public ProjectileMatchModule createMatchModule(Match match) {
    return new ProjectileMatchModule(match, this.projectileDefinitions);
  }

  public static class Factory implements MapModuleFactory<ProjectileModule> {
    @Override
    public Collection<Class<? extends MapModule<?>>> getSoftDependencies() {
      return ImmutableList.of(FilterModule.class);
    }

    @Override
    public ProjectileModule parse(MapFactory factory, Logger logger, Document doc)
        throws InvalidXMLException {
      Set<ProjectileDefinition> projectiles = new HashSet<>();
      var parser = factory.getParser();
      var projParser = new ProjectileParser(parser, doc);

      for (Element el :
          XMLUtils.flattenElements(doc.getRootElement(), "projectiles", "projectile")) {
        String id = parser.string(el, "id").required();
        String name = parser.string(el, "name").orNull();
        Double damage = parser.parseDouble(el, "damage").orNull();
        double velocity = parser.parseDouble(el, "velocity").optional(1.0);
        var clickAction =
            parser.parseEnum(ClickAction.class, el, "click").optional(ClickAction.BOTH);
        var projectile = projParser.parseDynamic(el);
        List<PotionEffect> potionKit = factory.getKits().parsePotions(el);
        Filter destroyFilter = parser.filter(el, "destroy-filter").orNull();
        Duration coolDown = parser.duration(el, "cooldown").attr().orNull();
        boolean throwable = parser.parseBool(el, "throwable").attr().optional(true);

        ProjectileDefinition projectileDefinition = new ProjectileDefinition(
            id,
            name,
            damage,
            velocity,
            clickAction,
            projectile,
            potionKit,
            destroyFilter,
            coolDown,
            throwable);

        factory.getFeatures().addFeature(el, projectileDefinition);
        projectiles.add(projectileDefinition);
      }

      return projectiles.isEmpty() ? null : new ProjectileModule(ImmutableSet.copyOf(projectiles));
    }
  }

  public static class ProjectileParser {
    private final XMLFluentParser parser;
    private final MethodParsers<PgmProjectile> methodParsers;
    private final Document doc;

    private ProjectileParser(XMLFluentParser parser, Document doc) {
      this.parser = parser;
      this.methodParsers = MethodParsers.<PgmProjectile>byAttrParser(this, "projectile")
          .withFallback(this::parsePlainEntity);
      this.doc = doc;
    }

    public PgmProjectile parseDynamic(Element el) throws InvalidXMLException {
      return methodParsers.parse(el);
    }

    @MethodParser("bridge-egg")
    public PgmProjectile parseBridgeEgg(Element el) throws InvalidXMLException {
      int bridgeRange = parser.parseInt(el, "bridge-range").required();

      List<Material> bridgeMaterials = new ArrayList<>();
      String bridgeMaterialId = el.getAttributeValue("bridge-material");
      Element bridgeMaterialEl = null;
      for (Element filter : XMLUtils.flattenElements(doc.getRootElement(), "filters")) {
        if (bridgeMaterialId != null && bridgeMaterialId.equals(filter.getAttributeValue("id"))) {
          bridgeMaterialEl = filter;
          break;
        }
      }
      if (bridgeMaterialEl != null) {
        for (Element child : bridgeMaterialEl.getChildren()) {
          Material material = XMLUtils.parseMaterial(new Node(child));
          if (!material.isBlock()) {
            throw new InvalidXMLException(
                "'" + material + "' is not a valid block for 'bridge-material'", child);
          }
          bridgeMaterials.add(material);
        }
      }

      boolean teamColor = parser.parseBool(el, "team-color").attr().orFalse();
      boolean silent = parser.parseBool(el, "silent").attr().orFalse();

      return new BridgeEggProjectile(bridgeRange, bridgeMaterials, teamColor, silent);
    }

    public PgmProjectile parsePlainEntity(Element el) throws InvalidXMLException {
      var entity = parser
          .<Class<? extends Entity>>node(XMLUtils::parseEntityType, el, "projectile")
          .optional(Arrow.class);
      var blockMaterial = entity.isAssignableFrom(FallingBlock.class)
          ? parser.blockMaterialData(el, "material").required()
          : null;
      var power = entity.isAssignableFrom(Explosive.class)
          ? parser.parseFloat(el, "power").orNull()
          : null;
      var precise = entity.isAssignableFrom(Fireball.class)
          ? parser.parseBool(el, "precise").attr().optional(true)
          : false;

      return new EntityProjectile(entity, blockMaterial, power, precise);
    }
  }
}
