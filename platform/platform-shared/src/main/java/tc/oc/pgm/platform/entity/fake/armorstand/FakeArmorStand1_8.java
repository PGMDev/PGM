package tc.oc.pgm.platform.entity.fake.armorstand;

import net.minecraft.server.v1_8_R3.EntityArmorStand;
import net.minecraft.server.v1_8_R3.NBTTagCompound;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import tc.oc.pgm.platform.entity.fake.FakeLivingEntity1_8;

public class FakeArmorStand1_8 extends FakeLivingEntity1_8<EntityArmorStand> {

  private final ItemStack head;

  public FakeArmorStand1_8(World world, ItemStack head) {
    super(new EntityArmorStand(((CraftWorld) world).getHandle()));
    this.head = head;

    entity.setInvisible(true);
    NBTTagCompound tag = entity.getNBTTag();
    if (tag == null) {
      tag = new NBTTagCompound();
    }
    entity.c(tag);
    tag.setBoolean("Silent", true);
    tag.setBoolean("Invulnerable", true);
    tag.setBoolean("NoGravity", true);
    tag.setBoolean("NoAI", true);
    entity.f(tag);
  }

  @Override
  public void spawn(Player viewer, Location location, Vector velocity) {
    super.spawn(viewer, location, velocity);
    if (head != null) wear(viewer, 4, head);
  }
}
