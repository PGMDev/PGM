package tc.oc.pgm.platform.modern.modules.behavior;

import io.papermc.paper.entity.LookAnchor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.pathfinder.Path;
import org.bukkit.Tag;
import org.bukkit.block.data.type.Door;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.util.Vector;
import tc.oc.pgm.platform.modern.modules.mannequin.Mannequin;

public class PathWalking {

  private PathWalking() {}

  public static void step(Mannequin mannequin, Path path, double speed, boolean openDoors) {
    if (path == null || path.isDone()) return;
    if (!mannequin.getEntity().isOnGround()) return;
    if (mannequin.getEntity().getNoDamageTicks() > 12) return; // Take kb before pathing again

    var nodePos = path.getNextNodePos();
    var loc = mannequin.getLocation();
    var direction =
        new Vector(nodePos.getX() + 0.5 - loc.getX(), 0, nodePos.getZ() + 0.5 - loc.getZ());

    if (direction.lengthSquared() < 0.25) {
      path.advance();
      return;
    }

    double distSq = direction.lengthSquared();
    Vector movement = direction.normalize().multiply(speed);
    if (nodePos.getY() - loc.getY() > 0.5 && distSq > 1.0) {
      movement.setY(0.42);
    }

    if (openDoors) {
      var world = mannequin.getEntity().getWorld();
      var doorBlock = world.getBlockAt(nodePos.getX(), nodePos.getY(), nodePos.getZ());
      var blockData = doorBlock.getBlockData();
      if (blockData instanceof Door door
          && !door.isOpen()
          && Tag.WOODEN_DOORS.isTagged(doorBlock.getType())) {

        var level = ((CraftWorld) world).getHandle();
        var pos = new BlockPos(nodePos.getX(), nodePos.getY(), nodePos.getZ());
        var state = level.getBlockState(pos);
        if (state.getBlock() instanceof DoorBlock nms) {
          nms.setOpen(null, level, state, pos, true); // Opens + plays appropriate door sound
        }
      }
    }

    mannequin
        .getEntity()
        .lookAt(
            new Vector(nodePos.getX() + 0.5, nodePos.getY() + 1.62, nodePos.getZ() + 0.5)
                .toLocation(mannequin.getEntity().getWorld()),
            LookAnchor.EYES);

    mannequin.getEntity().setVelocity(movement);
  }
}
