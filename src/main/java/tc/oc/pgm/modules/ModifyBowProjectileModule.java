package tc.oc.pgm.modules;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.potion.PotionEffect;
import org.jdom2.Document;
import org.jdom2.Element;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.MapProtos;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.map.factory.MapModuleFactory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.util.XMLUtils;
import tc.oc.xml.InvalidXMLException;

public class ModifyBowProjectileModule implements MapModule {
  protected final Class<? extends Entity> cls;
  protected final float velocityMod;
  protected final Set<PotionEffect> potionEffects;

  public ModifyBowProjectileModule(
      Class<? extends Entity> cls, float velocityMod, Set<PotionEffect> effects) {
    this.cls = cls;
    this.velocityMod = velocityMod;
    potionEffects = effects;
  }

  @Override
  public MatchModule createMatchModule(Match match) {
    return new ModifyBowProjectileMatchModule(
        match, this.cls, this.velocityMod, this.potionEffects);
  }

  public static class Factory implements MapModuleFactory<ModifyBowProjectileModule> {
    @Override
    public ModifyBowProjectileModule parse(MapFactory factory, Logger logger, Document doc)
        throws InvalidXMLException {
      boolean changed = false;
      Class<? extends Entity> projectile = Arrow.class;
      float velocityMod = 1;
      Set<PotionEffect> potionEffects = new HashSet<>();

      for (Element parent : doc.getRootElement().getChildren("modifybowprojectile")) {
        if (factory.getProto().isNoOlderThan(MapProtos.FILTER_FEATURES)) {
          throw new InvalidXMLException(
              "Module is discontinued as of " + MapProtos.FILTER_FEATURES.toString(),
              doc.getRootElement().getChild("modifybowprojectile"));
        }

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
      }

      return !changed
          ? null
          : new ModifyBowProjectileModule(projectile, velocityMod, potionEffects);
    }
  }
}
