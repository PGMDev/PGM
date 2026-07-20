package tc.oc.pgm.platform.modern.modules.behavior;

import io.papermc.paper.entity.LookAnchor;
import net.minecraft.world.level.pathfinder.Path;
import org.bukkit.util.Vector;
import tc.oc.pgm.platform.modern.modules.mannequin.Mannequin;

public class PathWalking {

  private PathWalking() {}

  public static void step(Mannequin mannequin, Path path, double speed) {
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

    mannequin
        .getEntity()
        .lookAt(
            new Vector(nodePos.getX() + 0.5, nodePos.getY() + 1.62, nodePos.getZ() + 0.5)
                .toLocation(mannequin.getEntity().getWorld()),
            LookAnchor.EYES);

    mannequin.getEntity().setVelocity(movement);
  }
}
