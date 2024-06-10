package tc.oc.pgm.platform.entity.fake.wither;

import net.minecraft.server.v1_8_R3.EntityWitherSkull;
import net.minecraft.server.v1_8_R3.PacketPlayOutSpawnEntity;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.platform.entity.fake.FakeEntityImpl1_8;

public class FakeWitherSkull1_8 extends FakeEntityImpl1_8<EntityWitherSkull> {
  public FakeWitherSkull1_8(World world) {
    super(new EntityWitherSkull(((CraftWorld) world).getHandle()));
  }

  public Object spawnPacket() {
    return new PacketPlayOutSpawnEntity(entity, 66);
  }

  @Override
  public void wear(Player viewer, int slot, ItemStack item) {}
}
