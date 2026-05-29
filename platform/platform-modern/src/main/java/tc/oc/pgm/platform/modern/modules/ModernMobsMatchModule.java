package tc.oc.pgm.platform.modern.modules;

import java.util.Collection;
import java.util.List;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.match.event.MatchFinishEvent;
import tc.oc.pgm.api.match.factory.MatchModuleFactory;
import tc.oc.pgm.api.module.exception.ModuleLoadException;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.events.PlayerChangePartyEvent;
import tc.oc.pgm.modules.MobsMatchModule;

@NullMarked
@ListenerScope(MatchScope.LOADED)
public class ModernMobsMatchModule implements MatchModule, Listener {

  public static class Factory implements MatchModuleFactory<ModernMobsMatchModule> {
    @Override
    public Collection<Class<? extends MatchModule>> getSoftDependencies() {
      return List.of(MobsMatchModule.class);
    }

    @Override
    public ModernMobsMatchModule createMatchModule(Match match) throws ModuleLoadException {
      return new ModernMobsMatchModule();
    }
  }

  // If a player leaves a team and becomes an observer, remove
  // them as a target for any mob that exists in the match.
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPartyChange(PlayerChangePartyEvent event) {
    if (event.getNewParty() == null || !event.getNewParty().isObserving()) return;
    event.addHandler(() -> clearMobTargets(event.getMatch(), event.getPlayer().getBukkit()));
  }

  // Remove match participants as mob targets on match end
  @EventHandler(priority = EventPriority.MONITOR)
  public void onMatchFinish(MatchFinishEvent event) {
    clearMobTargets(event.getMatch(), null);
  }

  private void clearMobTargets(Match match, @Nullable Player targetPlayer) {
    for (var entity : match.getWorld().getLivingEntities()) {
      if (entity instanceof Mob mob
          && mob.getTarget() instanceof Player target
          && (targetPlayer == null || targetPlayer.equals(target))) {
        mob.setTarget(null);
      }
    }
  }
}
