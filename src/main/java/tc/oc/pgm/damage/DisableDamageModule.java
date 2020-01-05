package tc.oc.pgm.damage;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import java.util.logging.Logger;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.jdom2.Document;
import org.jdom2.Element;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.PlayerRelation;
import tc.oc.pgm.map.MapModule;
import tc.oc.pgm.map.MapModuleContext;
import tc.oc.pgm.module.ModuleDescription;
import tc.oc.pgm.util.XMLUtils;
import tc.oc.xml.InvalidXMLException;
import tc.oc.xml.Node;

@ModuleDescription(name = "DisableDamage")
public class DisableDamageModule extends MapModule<DisableDamageMatchModule> {
  protected final SetMultimap<DamageCause, PlayerRelation> causes;

  public DisableDamageModule(SetMultimap<DamageCause, PlayerRelation> causes) {
    this.causes = causes;
  }

  @Override
  public DisableDamageMatchModule createMatchModule(Match match) {
    return new DisableDamageMatchModule(match, this.causes);
  }

  // ---------------------
  // ---- XML Parsing ----
  // ---------------------

  public static DisableDamageModule parse(MapModuleContext context, Logger logger, Document doc)
      throws InvalidXMLException {
    SetMultimap<DamageCause, PlayerRelation> causes = HashMultimap.create();
    DamageParser parser = new DamageParser();

    for (Element damageCauseElement : doc.getRootElement().getChildren("disabledamage")) {
      for (Element damageEl : damageCauseElement.getChildren("damage")) {
        DamageCause cause = parser.parseDamageCause(damageEl);
        for (PlayerRelation damagerType : PlayerRelation.values()) {
          // Legacy syntax used "other" instead of "neutral"
          String attrName = damagerType.name().toLowerCase();
          Node attr =
              damagerType == PlayerRelation.NEUTRAL
                  ? Node.fromAttr(damageEl, attrName, "other")
                  : Node.fromAttr(damageEl, attrName);
          if (XMLUtils.parseBoolean(attr, true)) {
            causes.put(cause, damagerType);

            // Bukkit 1.7.10 changed TNT from BLOCK_EXPLOSION to ENTITY_EXPLOSION,
            // so we include them both to keep old maps working.
            if (cause == DamageCause.BLOCK_EXPLOSION) {
              causes.put(DamageCause.ENTITY_EXPLOSION, damagerType);
            }
          }
        }
      }
    }
    return new DisableDamageModule(causes);
  }
}
