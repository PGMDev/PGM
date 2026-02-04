package tc.oc.pgm.platform.modern.modules.kits;

import io.papermc.paper.event.player.PlayerSwapWithEquipmentSlotEvent;
import java.util.Collection;
import java.util.List;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.factory.MatchModuleFactory;
import tc.oc.pgm.api.module.exception.ModuleLoadException;
import tc.oc.pgm.kits.KitMatchModule;

public class ModernKitMatchModule implements MatchModule, Listener {

  public static class Factory implements MatchModuleFactory<ModernKitMatchModule> {
    @Override
    public Collection<Class<? extends MatchModule>> getSoftDependencies() {
      return List.of(KitMatchModule.class); // Only load if kit module loads
    }

    @Override
    public ModernKitMatchModule createMatchModule(Match match) throws ModuleLoadException {
      return new ModernKitMatchModule(match);
    }
  }

  private final KitMatchModule kmm;

  public ModernKitMatchModule(Match match) {
    this.kmm = match.needModule(KitMatchModule.class);
  }

  @EventHandler(ignoreCancelled = true)
  public void onItemHotbarSwap(PlayerSwapHandItemsEvent event) {
    if (kmm.isLocked(event.getMainHandItem()) || kmm.isLocked(event.getOffHandItem())) {
      event.setCancelled(true);
      kmm.sendLockWarning(event.getPlayer());
    }
  }

  @EventHandler(ignoreCancelled = true)
  public void onItemArmorSwap(PlayerSwapWithEquipmentSlotEvent event) {
    if (kmm.isLocked(event.getItemInHand()) || kmm.isLocked(event.getItemToSwap())) {
      event.setCancelled(true);
      kmm.sendLockWarning(event.getPlayer());
    }
  }
}
