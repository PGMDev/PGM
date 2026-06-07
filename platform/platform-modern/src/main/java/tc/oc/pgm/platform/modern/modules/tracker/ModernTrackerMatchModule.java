package tc.oc.pgm.platform.modern.modules.tracker;

import java.util.Collection;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import org.bukkit.craftbukkit.damage.CraftDamageSource;
import org.bukkit.craftbukkit.entity.CraftEnderCrystal;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPlaceEvent;
import org.jspecify.annotations.NullMarked;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.match.factory.MatchModuleFactory;
import tc.oc.pgm.api.module.exception.ModuleLoadException;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.platform.modern.listeners.TntListener;
import tc.oc.pgm.tracker.TrackerMatchModule;
import tc.oc.pgm.tracker.info.EntityInfo;

@NullMarked
@ListenerScope(MatchScope.RUNNING)
public class ModernTrackerMatchModule implements MatchModule, Listener {

  public static class Factory implements MatchModuleFactory<ModernTrackerMatchModule> {
    @Override
    public Collection<Class<? extends MatchModule>> getSoftDependencies() {
      return List.of(TrackerMatchModule.class);
    }

    @Override
    public ModernTrackerMatchModule createMatchModule(Match match) throws ModuleLoadException {
      return new ModernTrackerMatchModule(match);
    }
  }

  private final Match match;
  private final TrackerMatchModule tmm;

  public ModernTrackerMatchModule(Match match) {
    this.match = match;
    this.tmm = match.needModule(TrackerMatchModule.class);
  }

  /**
   * causingEntity needs to be null to ensure self-damage is not negated when friendly fire is
   * disabled. We have to do something similar for TNT, see {@link TntListener#onTntSpawn}; however,
   * as causingEntity is private and final, we have to cancel the original event and replace the end
   * crystal's DamageSource and then call hurtServer.
   */
  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onCrystalDamage(EntityDamageByEntityEvent event) {
    if (!(event.getEntity() instanceof EnderCrystal crystal)) return;
    // Only intercept when a player has damaged the end crystal
    if (!(event.getDamageSource().getCausingEntity() instanceof Player)) return;

    ParticipantState owner = tmm.getEntityTracker().getOwner(event.getDamager());
    if (owner == null) return;

    var nmsCrystal = ((CraftEnderCrystal) crystal).getHandle();
    var handle = ((CraftDamageSource) event.getDamageSource()).getHandle();

    // Reconstruct the damage source one-to-one, but setting causingEntity as null
    var damageSource = new DamageSource(
        handle.typeHolder(), handle.getDirectEntity(), null, handle.sourcePositionRaw());
    var knownCause = handle.knownCause();
    if (knownCause != null) damageSource = damageSource.knownCause(knownCause);
    if (handle.isCritical()) damageSource = damageSource.critical();

    event.setCancelled(true);
    nmsCrystal.hurtServer(
        (ServerLevel) nmsCrystal.level(), damageSource, (float) event.getDamage());
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onCrystalPlace(EntityPlaceEvent event) {
    if (!(event.getEntity() instanceof EnderCrystal crystal)) return;

    Player player = event.getPlayer();
    if (player == null) return;

    ParticipantState placer = match.getParticipantState(player);
    if (placer == null) return;

    tmm.getEntityTracker().trackEntity(crystal, new EntityInfo(crystal, placer));
  }
}
