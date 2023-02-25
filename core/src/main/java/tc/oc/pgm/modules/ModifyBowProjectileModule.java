package tc.oc.pgm.modules;

import java.util.HashSet;
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
import tc.oc.pgm.filters.matcher.StaticFilter;
import tc.oc.pgm.filters.parse.FilterParser;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.XMLUtils;

public class ModifyBowProjectileModule implements MapModule<ModifyBowProjectileMatchModule> {
  protected final Class<? extends Entity> cls;
  protected final float velocityMod;
  protected final Set<PotionEffect> potionEffects;
  protected final Filter pickupFilter;

  public ModifyBowProjectileModule(
      Class<? extends Entity> cls,
      float velocityMod,
      Set<PotionEffect> effects,
      Filter pickupFilter) {
    this.cls = cls;
    this.velocityMod = velocityMod;
    potionEffects = effects;
    this.pickupFilter = pickupFilter;
  }

  @Override
  public ModifyBowProjectileMatchModule createMatchModule(Match match) {
    return new ModifyBowProjectileMatchModule(
        match, this.cls, this.velocityMod, this.potionEffects, this.pickupFilter);
  }

  public static class Factory implements MapModuleFactory<ModifyBowProjectileModule> {
    @Override
    public ModifyBowProjectileModule parse(MapFactory factory, Logger logger, Document doc)
        throws InvalidXMLException {
      FilterParser filters = factory.getFilters();

      boolean changed = false;
      Class<? extends Entity> projectile = Arrow.class;
      float velocityMod = 1;
      Set<PotionEffect> potionEffects = new HashSet<>();
      Filter pickupFilter = StaticFilter.ALLOW;

      for (Element parent : doc.getRootElement().getChildren("modifybowprojectile")) {
        Element projectileElement = parent.getChild("projectile");
        if (projectileElement != null) {
          projectile = XMLUtils.parseEntityType(projectileElement);
          changed = true;
        }

        Element velocityModElement = parent.getChild("velocityMod");
        if (velocityModElement != null) {
          velocityMod = XMLUtils.parseNumber(velocityModElement, Float.class);
          changed = true;
        }

        for (Element potionElement : parent.getChildren("potion")) {
          potionEffects.add(XMLUtils.parsePotionEffect(potionElement));
          changed = true;
        }

        pickupFilter = filters.parseFilterProperty(parent, "pickup-filter", StaticFilter.ALLOW);
        changed |= pickupFilter != StaticFilter.ALLOW;
      }

      return !changed
          ? null
          : new ModifyBowProjectileModule(projectile, velocityMod, potionEffects, pickupFilter);
    }
  }
}
