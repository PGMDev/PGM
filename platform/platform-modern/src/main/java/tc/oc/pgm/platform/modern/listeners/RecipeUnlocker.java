package tc.oc.pgm.platform.modern.listeners;

import net.minecraft.server.MinecraftServer;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.spigotmc.event.player.PlayerSpawnLocationEvent;

public class RecipeUnlocker implements Listener {

  // PlayerJoinEvent is too late as recipe init packet was already sent, so we use spawn location
  // event
  @EventHandler
  public void onPlayerJoin(PlayerSpawnLocationEvent event) {
    // Non-joined players haven't been added to the player list yet
    if (Bukkit.getPlayer(event.getPlayer().getUniqueId()) != null) return;

    var player = ((CraftPlayer) event.getPlayer()).getHandle();
    player
        .getRecipeBook()
        .known
        .addAll(MinecraftServer.getServer().getRecipeManager().byName.keySet());
  }
}
