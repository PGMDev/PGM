package tc.oc.pgm.spawns.states;

import java.util.List;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.PermissionAttachment;
import tc.oc.block.BlockVectors;
import tc.oc.pgm.events.MatchBeginEvent;
import tc.oc.pgm.events.PlayerJoinPartyEvent;
import tc.oc.pgm.match.Competitor;
import tc.oc.pgm.match.MatchPlayer;
import tc.oc.pgm.spawns.ObserverToolFactory;
import tc.oc.pgm.spawns.Spawn;
import tc.oc.pgm.spawns.SpawnMatchModule;
import tc.oc.pgm.spawns.events.ObserverKitApplyEvent;

public class Observing extends State {

  private final boolean reset;
  private final boolean teleport;
  private PermissionAttachment permissionAttachment;

  public Observing(SpawnMatchModule smm, MatchPlayer player, boolean reset, boolean teleport) {
    super(smm, player);
    this.reset = reset;
    this.teleport = teleport;
  }

  @Override
  public void enterState() {
    super.enterState();

    permissionAttachment = bukkit.addAttachment(this.smm.getMatch().getPlugin());
    permissionAttachment.setPermission(smm.getObserverPermissions(), true);

    if (reset) player.reset();
    player.setDead(false);
    player.refreshGameMode();

    Spawn spawn = smm.getDefaultSpawn();

    if (teleport) {
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

      ItemStack book = toolFactory.getHowToBook(bukkit);
      if (toolFactory.canUseEditWand(bukkit)) {
        player.getInventory().setItem(1, toolFactory.getEditWand(bukkit));
        if (book != null) player.getInventory().setItem(28, book);
      } else {
        if (book != null) player.getInventory().setItem(1, book);
      }

      // Let other modules give observer items
      player.getMatch().callEvent(new ObserverKitApplyEvent(player));

      // Apply observer spawn kit, if there is one
      spawn.applyKit(player);
    }

    player.getBukkit().updateInventory();
    player.setVisible(true);
    player.refreshGameMode();

    // The player is not standing on anything, turn their flying on
    if (bukkit.getAllowFlight()) {
      Block block = bukkit.getLocation().subtract(0, 0.1, 0).getBlock();
      if (block == null || !BlockVectors.isSupportive(block.getType())) {
        bukkit.setFlying(true);
      }
    }
  }

  @Override
  public void leaveState(List<Event> events) {
    super.leaveState(events);
    if (permissionAttachment != null) bukkit.removeAttachment(permissionAttachment);
  }

  @Override
  public void onEvent(MatchBeginEvent event) {
    super.onEvent(event);
    if (player.isParticipatingType()) {
      transition(new Joining(smm, player));
    }
  }

  @Override
  public void onEvent(PlayerJoinPartyEvent event) {
    if (event.getNewParty() instanceof Competitor && event.getMatch().isRunning()) {
      transition(new Joining(smm, player));
    }
  }
}
