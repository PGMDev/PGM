package tc.oc.pgm.platform.modern.modules;

import io.papermc.paper.event.entity.EntityInsideBlockEvent;
import io.papermc.paper.event.entity.EntityKnockbackEvent;
import java.util.Collection;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.AreaEffectCloudApplyEvent;
import org.bukkit.event.world.GenericGameEvent;
import org.jspecify.annotations.NullMarked;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.match.factory.MatchModuleFactory;
import tc.oc.pgm.api.module.exception.ModuleLoadException;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.modules.EventFilterMatchModule;
import tc.oc.pgm.util.MatchPlayers;

@NullMarked
@ListenerScope(MatchScope.LOADED)
public class ModernEventFilterMatchModule implements MatchModule, Listener {

  public static class Factory implements MatchModuleFactory<ModernEventFilterMatchModule> {
    @Override
    public Collection<Class<? extends MatchModule>> getSoftDependencies() {
      return List.of(EventFilterMatchModule.class);
    }

    @Override
    public ModernEventFilterMatchModule createMatchModule(Match match) throws ModuleLoadException {
      return new ModernEventFilterMatchModule(match);
    }
  }

  private final Match match;
  private final EventFilterMatchModule efmm;

  public ModernEventFilterMatchModule(Match match) {
    this.match = match;
    this.efmm = match.needModule(EventFilterMatchModule.class);
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onPotionLinger(AreaEffectCloudApplyEvent event) {
    event
        .getAffectedEntities()
        .removeIf(entity ->
            entity instanceof Player && !MatchPlayers.canInteract(match.getParticipant(entity)));
  }

  // Prevent observers from triggering sculk sensors and sculk shriekers
  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
  public void onGameEvent(GenericGameEvent event) {
    efmm.cancelUnlessInteracting(event, event.getEntity());
  }

  // Prevent observers from receiving explosion knockback
  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
  public void onKnockback(EntityKnockbackEvent event) {
    if (event.getCause() != EntityKnockbackEvent.Cause.EXPLOSION) return;
    efmm.cancelUnlessInteracting(event, event.getEntity());
  }

  // Prevent observers from triggering tripwires
  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
  public void onInsideBlock(EntityInsideBlockEvent event) {
    if (event.getBlock().getType() != Material.TRIPWIRE) return;
    efmm.cancelUnlessInteracting(event, event.getEntity());
  }
}
