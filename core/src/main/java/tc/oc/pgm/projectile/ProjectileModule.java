package tc.oc.pgm.projectile;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.time.Duration;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.potion.PotionEffect;
import org.jdom2.Document;
import org.jdom2.Element;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.map.factory.MapModuleFactory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.filters.FilterModule;
import tc.oc.pgm.filters.parse.FilterParser;
import tc.oc.pgm.kits.KitParser;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;
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
      KitParser kitParser = factory.getKits();
      FilterParser filterParser = factory.getFilters();

      for (Element projectileElement :
          XMLUtils.flattenElements(doc.getRootElement(), "projectiles", "projectile")) {
        String id = XMLUtils.getRequiredAttribute(projectileElement, "id").getValue();
        String name = projectileElement.getAttributeValue("name");
        Double damage =
            XMLUtils.parseNumber(
                projectileElement.getAttribute("damage"), Double.class, (Double) null);
        double velocity =
            XMLUtils.parseNumber(
                Node.fromChildOrAttr(projectileElement, "velocity"), Double.class, 1.0);
        ClickAction clickAction =
            XMLUtils.parseEnum(
                Node.fromAttr(projectileElement, "click"),
                ClickAction.class,
                "click action",
                ClickAction.BOTH);
        Class<? extends Entity> entity =
            XMLUtils.parseEntityTypeAttribute(projectileElement, "projectile", Arrow.class);
        List<PotionEffect> potionKit = kitParser.parsePotions(projectileElement);
        Filter destroyFilter =
            filterParser.parseFilterProperty(projectileElement, "destroy-filter");
        Duration coolDown = XMLUtils.parseDuration(projectileElement.getAttribute("cooldown"));
        boolean throwable =
            XMLUtils.parseBoolean(projectileElement.getAttribute("throwable"), true);
        boolean precise = XMLUtils.parseBoolean(projectileElement.getAttribute("precise"), true);

        ProjectileDefinition projectileDefinition =
            new ProjectileDefinition(
                id,
                name,
                damage,
                velocity,
                clickAction,
                entity,
                potionKit,
                destroyFilter,
                coolDown,
                throwable,
                precise);

        factory.getFeatures().addFeature(projectileElement, projectileDefinition);
        projectiles.add(projectileDefinition);
      }

      return projectiles.isEmpty() ? null : new ProjectileModule(ImmutableSet.copyOf(projectiles));
    }
  }
}
