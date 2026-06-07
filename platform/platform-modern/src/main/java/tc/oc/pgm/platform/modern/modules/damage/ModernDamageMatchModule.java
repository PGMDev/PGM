package tc.oc.pgm.platform.modern.modules.damage;

import static org.bukkit.event.entity.EntityDamageEvent.DamageCause.MAGIC;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.AreaEffectCloudApplyEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionType;
import org.jspecify.annotations.NullMarked;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.match.factory.MatchModuleFactory;
import tc.oc.pgm.api.module.exception.ModuleLoadException;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.api.tracker.info.DamageInfo;
import tc.oc.pgm.damage.DamageMatchModule;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.util.bukkit.PotionClassification;

@NullMarked
@ListenerScope(MatchScope.RUNNING)
public class ModernDamageMatchModule implements MatchModule, Listener {

  public static class Factory implements MatchModuleFactory<ModernDamageMatchModule> {
    @Override
    public Collection<Class<? extends MatchModule>> getSoftDependencies() {
      return List.of(DamageMatchModule.class);
    }

    @Override
    public ModernDamageMatchModule createMatchModule(Match match) throws ModuleLoadException {
      return new ModernDamageMatchModule(match);
    }
  }

  private final Match match;
  private final DamageMatchModule dmm;

  public ModernDamageMatchModule(Match match) {
    this.match = match;
    this.dmm = match.needModule(DamageMatchModule.class);
  }

  @EventHandler(ignoreCancelled = true)
  public void onPotionLinger(AreaEffectCloudApplyEvent event) {
    AreaEffectCloud cloud = event.getEntity();
    if (!isHarmful(cloud)) return;

    event.getAffectedEntities().removeIf(entity -> {
      ParticipantState victim = match.getParticipantState(entity);
      if (victim == null) return false;

      DamageInfo damageInfo = dmm.tracker().resolveDamage(MAGIC, entity, cloud);
      return dmm.queryDamage(event, victim, damageInfo).isDenied();
    });
  }

  private static boolean isHarmful(AreaEffectCloud cloud) {
    List<PotionEffect> effects = new ArrayList<>(cloud.getCustomEffects());
    PotionType base = cloud.getBasePotionType();
    if (base != null) effects.addAll(base.getPotionEffects());
    return PotionClassification.isHarmful(effects);
  }
}
