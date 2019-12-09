package tc.oc.pgm.modules;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Projectile;
import org.bukkit.potion.PotionEffect;
import org.jdom2.Document;
import org.jdom2.Element;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.map.MapModule;
import tc.oc.pgm.map.MapModuleContext;
import tc.oc.pgm.map.ProtoVersions;
import tc.oc.pgm.match.MatchModule;
import tc.oc.pgm.module.ModuleDescription;
import tc.oc.pgm.util.XMLUtils;
import tc.oc.xml.InvalidXMLException;

@ModuleDescription(name = "Modify Bow Projectile")
public class ModifyBowProjectileModule extends MapModule {
  protected final Class<? extends Projectile> cls;
  protected final float velocityMod;
  protected final Set<PotionEffect> potionEffects;

  public ModifyBowProjectileModule(
      Class<? extends Projectile> cls, float velocityMod, Set<PotionEffect> effects) {
    this.cls = cls;
    this.velocityMod = velocityMod;
    potionEffects = effects;
  }

  @Override
  public MatchModule createMatchModule(Match match) {
    return new ModifyBowProjectileMatchModule(
        match, this.cls, this.velocityMod, this.potionEffects);
  }

  // ---------------------
  // ---- XML Parsing ----
  // ---------------------

  public static ModifyBowProjectileModule parse(
      MapModuleContext context, Logger logger, Document doc) throws InvalidXMLException {
    boolean changed = false;
    Class<? extends Projectile> projectile = Arrow.class;
    float velocityMod = 1;
    Set<PotionEffect> potionEffects = new HashSet<>();

    for (Element parent : doc.getRootElement().getChildren("modifybowprojectile")) {
      if (context.getProto().isNoOlderThan(ProtoVersions.FILTER_FEATURES)) {
        throw new InvalidXMLException(
            "Module is discontinued as of " + ProtoVersions.FILTER_FEATURES.toString(),
            doc.getRootElement().getChild("modifybowprojectile"));
      }

      Element projectileElement = parent.getChild("projectile");
      if (projectileElement != null) {
        projectile = XMLUtils.parseProjectileType(projectileElement);
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

    return !changed ? null : new ModifyBowProjectileModule(projectile, velocityMod, potionEffects);
  }
}
