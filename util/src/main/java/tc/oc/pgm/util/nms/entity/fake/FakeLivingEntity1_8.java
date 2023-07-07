package tc.oc.pgm.util.nms.entity.fake;

import net.minecraft.server.v1_8_R3.EntityLiving;
import net.minecraft.server.v1_8_R3.PacketPlayOutSpawnEntityLiving;

public class FakeLivingEntity1_8<T extends EntityLiving> extends FakeEntityImpl1_8<T> {

  protected FakeLivingEntity1_8(T entity) {
    super(entity);
  }

  public Object spawnPacket() {
    return new PacketPlayOutSpawnEntityLiving(entity);
  }
}
