package tc.oc.pgm.projectile;

import static tc.oc.pgm.util.material.ColorUtils.COLOR_UTILS;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import tc.oc.pgm.api.event.BlockTransformEvent;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.events.ParticipantBlockTransformEvent;
import tc.oc.pgm.util.TimeUtils;
import tc.oc.pgm.util.bukkit.Sounds;
import tc.oc.pgm.util.nms.NMSHacks;

public class ActiveBridgeEgg {

  private final Match match;
  private final MatchPlayer player;
  private final UUID uuid;
  private final HashMap<UUID, ActiveBridgeEgg> registry;
  private final Entity proj;
  private final PlayerInteractEvent launch;
  private Location bridgePos;
  private Location prevPos;
  private final Location throwOrigin;
  private final int bridgeRange;
  private final List<Material> bridgeMaterial;
  private final boolean teamColor;
  private final boolean silent;
  private Future<?> runnableTask = null;
  private final BukkitRunnable runnable = new BridgeEggRunnable();
  private int ticksElapsed = 0;

  public ActiveBridgeEgg(
      Match match,
      MatchPlayer player,
      UUID uuid,
      HashMap<UUID, ActiveBridgeEgg> registry,
      Entity proj,
      PlayerInteractEvent launch,
      int bridgeRange,
      List<Material> bridgeMaterial,
      Location bridgePos,
      boolean teamColor,
      boolean silent) {
    this.match = match;
    this.player = player;
    this.uuid = uuid;
    this.registry = registry;
    this.proj = proj;
    this.launch = launch;
    this.throwOrigin = bridgePos.clone();
    this.bridgeRange = bridgeRange;
    this.bridgeMaterial = bridgeMaterial;
    this.bridgePos = bridgePos;
    this.prevPos = bridgePos.clone();
    this.teamColor = teamColor;
    this.silent = silent;
  }

  public boolean isOwnedBy(MatchPlayer player) {
    return this.player.equals(player);
  }

  public void start() {
    if (runnableTask != null && !this.runnableTask.isDone()) return;

    this.runnableTask = match
        .getExecutor(MatchScope.RUNNING)
        .scheduleAtFixedRate(this.runnable, 0, TimeUtils.TICK, TimeUnit.MILLISECONDS);
  }

  private class BridgeEggRunnable extends BukkitRunnable {
    @Override
    public void run() {
      if (proj.isDead()) {
        stop();
        return;
      }

      if (proj.getLocation().getY() <= NMSHacks.NMS_HACKS.getMinHeight(proj.getWorld()) - 16) {
        stop();
        return;
      }

      org.bukkit.Location currentPos = proj.getLocation();
      Location targetPos = currentPos.clone();
      targetPos.setY(currentPos.getY() - 2);
      Vector dir = targetPos.toVector().subtract(bridgePos.toVector()).normalize();
      bridgePos = bridgePos.add(dir);
      double disTraveled = throwOrigin.distance(bridgePos);

      if (disTraveled >= bridgeRange) {
        stop();
        return;
      }

      ticksElapsed++;
      if (ticksElapsed < 4) {
        prevPos = bridgePos.clone();
        return;
      }
      if (ticksElapsed > 600) {
        stop();
        return;
      }

      int y = (int) Math.floor(bridgePos.getY());
      int prevX = (int) Math.floor(prevPos.getX());
      int prevZ = (int) Math.floor(prevPos.getZ());
      int currX = (int) Math.floor(bridgePos.getX());
      int currZ = (int) Math.floor(bridgePos.getZ());
      Set<Location> toPlace = new HashSet<>();
      toPlace.add(new Location(bridgePos.getWorld(), currX, y, currZ));
      Location leader = new Location(bridgePos.getWorld(), currX, y, currZ);

      if (prevX != currX && prevZ != currZ) {
        toPlace.add(new Location(bridgePos.getWorld(), currX, y, prevZ));
        toPlace.add(new Location(bridgePos.getWorld(), prevX, y, currZ));
      }

      for (Location loc : toPlace) {
        Block block = loc.getBlock();
        Material material = bridgeMaterial.get(match.getRandom().nextInt(bridgeMaterial.size()));

        if (block.getType() == Material.AIR) {
          ParticipantState state = player.getParticipantState();
          BlockTransformEvent bte = state != null
              ? new ParticipantBlockTransformEvent(launch, block, material, state)
              : new BlockTransformEvent(launch, block, material);
          match.callEvent(bte);
          if (!bte.isCancelled()) {
            block.setType(material);
            if (!silent) {
              Sounds.play(player.getBukkit(), Sounds.BRIDGE_EGG, loc);
            }
            if (teamColor) {
              DyeColor color = player.getParty().getDyeColor();
              if (color != null) {
                COLOR_UTILS.setColor(block, color);
              }
            }

          } else if (loc.equals(leader)) {
            stop();
            return;
          }
        }
      }
      prevPos = bridgePos.clone();
    }
  }

  public void stop() {
    runnableTask.cancel(true);
    proj.remove();
    registry.remove(uuid);
  }
}
