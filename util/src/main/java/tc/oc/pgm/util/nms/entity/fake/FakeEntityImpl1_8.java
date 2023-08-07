package tc.oc.pgm.util.nms.entity.fake;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import tc.oc.pgm.util.nms.NMSHacks;

public abstract class FakeEntityImpl1_8<T extends net.minecraft.server.v1_8_R3.Entity>
    implements FakeEntity {
  protected final T entity;

  protected FakeEntityImpl1_8(T entity) {
    this.entity = entity;
  }

  @Override
  public void spawn(Player viewer, Location location, Vector velocity) {
    entity.setPositionRotation(
        location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
    entity.motX = velocity.getX();
    entity.motY = velocity.getY();
    entity.motZ = velocity.getZ();
    Object packet = spawnPacket();
    NMSHacks.sendPacket(viewer, packet);
  }

  public abstract Object spawnPacket();

  @Override
  public int entityId() {
    return entity.getId();
  }
}
