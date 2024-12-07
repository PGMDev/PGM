package tc.oc.pgm.spawns.states;

import java.util.EnumSet;
import java.util.Set;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.material.Door;
import tc.oc.pgm.api.match.event.MatchStartEvent;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.events.PlayerJoinPartyEvent;
import tc.oc.pgm.spawns.ObserverToolFactory;
import tc.oc.pgm.spawns.Spawn;
import tc.oc.pgm.spawns.SpawnMatchModule;
import tc.oc.pgm.spawns.events.ObserverKitApplyEvent;
import tc.oc.pgm.util.block.BlockVectors;
import tc.oc.pgm.util.material.Materials;

public class Observing extends State {

  // A set of item types which, when used to interact with the match environment by non-playing
  // users, can potentially cause client-server de-sync
  private static final Set<Material> BAD_TYPES =
      EnumSet.of(Materials.LILY_PAD, Material.BUCKET, Material.LAVA_BUCKET, Material.WATER_BUCKET);

  private static final double VOID_HEIGHT = -64;

  private final boolean reset;
  private final boolean teleport;

  public Observing(SpawnMatchModule smm, MatchPlayer player, boolean reset, boolean teleport) {
    super(smm, player);
    this.reset = reset;
    this.teleport = teleport;
    this.permission = new StatePermissions.Observer();
  }

  @Override
  public void enterState() {
    super.enterState();
    player.setDead(false);
    resetPlayer(smm, player, reset, teleport);
    player.setVisible(true);
    player.resetVisibility();
  }

  // Made static to allow for reuse
  public static void resetPlayer(
      SpawnMatchModule smm, MatchPlayer player, boolean reset, boolean teleport) {
    var bukkit = player.getBukkit();
    if (reset) player.reset();
    player.resetInteraction();
    bukkit.setGameMode(GameMode.CREATIVE);
    bukkit.setAllowFlight(true);

    Spawn spawn = smm.getDefaultSpawn();

    if (teleport || player.getBukkit().getLocation().getY() <= VOID_HEIGHT) {
      Location location = spawn.getSpawn(player);
      if (location != null) {
        PlayerRespawnEvent event = new PlayerRespawnEvent(player.getBukkit(), location, false);
        player.getMatch().callEvent(event);

        player.getBukkit().teleport(event.getRespawnLocation());
      }
    }

    if (reset) {
      // Give basic observer items
      ObserverToolFactory toolFactory = smm.getObserverToolFactory();
      player.getInventory().setItem(0, toolFactory.getTeleportTool(bukkit));

      if (toolFactory.canUseEditWand(bukkit)) {
        player.getInventory().setItem(1, toolFactory.getEditWand(bukkit));
      }

      // Let other modules give observer items
      player.getMatch().callEvent(new ObserverKitApplyEvent(player));

      // Apply observer spawn kit, if there is one
      spawn.applyKit(player);
    }

    player.getBukkit().updateInventory();

    // The player is not standing on anything, turn their flying on
    if (bukkit.getAllowFlight()) {
      Block block = bukkit.getLocation().subtract(0, 0.1, 0).getBlock();
      if (block == null || !BlockVectors.isSupportive(block.getType())) {
        bukkit.setFlying(true);
      }
    }
  }

  @Override
  public void onEvent(MatchStartEvent event) {
    super.onEvent(event);
    if (player.isParticipating()) {
      transition(new Joining(smm, player, 0, false));
    }
  }

  @Override
  public void onEvent(PlayerJoinPartyEvent event) {
    if (event.getNewParty() instanceof Competitor && event.getMatch().isRunning()) {
      transition(new Joining(smm, player, smm.getJoinPenalty(event), false));
    }
  }

  @Override
  public void onEvent(EntityDamageEvent event) {
    super.onEvent(event);
    event.setCancelled(true);
  }

  @Override
  public void onEvent(InventoryClickEvent event) {
    super.onEvent(event);

    var inv = event.getInventory();
    if (inv == null
        || !(inv instanceof PlayerInventory || inv.getType() == InventoryType.CRAFTING)
        || event.getCursor() == null) return;

    ItemStack item = event.getCursor();
    if (BAD_TYPES.contains(item.getType()) || item.getData() instanceof Door) {
      event.setCancelled(true);
    }
  }
}
