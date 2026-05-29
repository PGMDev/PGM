package tc.oc.pgm.platform.modern.listeners;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.protocol.game.ClientboundRecipeBookAddPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.platform.modern.packets.PacketSender;

public class RecipeUnlocker implements PacketSender, Listener {

  private static final List<ClientboundRecipeBookAddPacket.Entry> ENTRIES = new ArrayList<>();
  private static final RecipeManager RECIPE_MANAGER =
      MinecraftServer.getServer().getRecipeManager();

  static {
    for (RecipeHolder<?> recipe : RECIPE_MANAGER.getRecipes()) {
      RECIPE_MANAGER.listDisplaysForRecipe(
          recipe.id(),
          display -> ENTRIES.add(new ClientboundRecipeBookAddPacket.Entry(display, false, false)));
    }
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onPlayerJoin(PlayerJoinEvent event) {
    var player = event.getPlayer();
    PGM.get().getExecutor().submit(() -> {
      if (!player.isConnected()) return;

      send(new ClientboundRecipeBookAddPacket(ENTRIES, true), player);
      ((CraftPlayer) player)
          .getHandle()
          .getRecipeBook()
          .known
          .addAll(RECIPE_MANAGER.recipes.byKey.keySet());
    });
  }
}
