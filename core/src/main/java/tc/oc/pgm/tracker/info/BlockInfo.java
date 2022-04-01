package tc.oc.pgm.tracker.info;

import javax.annotation.Nullable;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.api.tracker.info.DamageInfo;
import tc.oc.pgm.api.tracker.info.PhysicalInfo;
import tc.oc.pgm.util.text.MinecraftComponent;

public class BlockInfo extends OwnerInfoBase implements PhysicalInfo, DamageInfo {

  private final Material material;

  public BlockInfo(Material material, @Nullable ParticipantState owner) {
    super(owner);
    this.material = material;
  }

  public BlockInfo(BlockState block, @Nullable ParticipantState owner) {
    this(block.getType(), owner);
  }

  public BlockInfo(BlockState block) {
    this(block, null);
  }

  public Material getMaterial() {
    return material;
  }

  @Override
  public String getIdentifier() {
    return getMaterial().name();
  }

  @Override
  public Component getName() {
    return MinecraftComponent.material(getMaterial());
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{world=" + getMaterial() + " owner=" + getOwner() + "}";
  }

  @Nullable
  @Override
  public ParticipantState getAttacker() {
    return getOwner();
  }
}
