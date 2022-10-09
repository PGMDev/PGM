package tc.oc.pgm.tracker.info;

import net.kyori.adventure.text.Component;
import org.bukkit.block.BlockState;
import org.bukkit.material.MaterialData;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.api.tracker.info.DamageInfo;
import tc.oc.pgm.api.tracker.info.PhysicalInfo;
import tc.oc.pgm.util.text.MinecraftComponent;

public class BlockInfo extends OwnerInfoBase implements PhysicalInfo, DamageInfo {

  private final MaterialData material;

  public BlockInfo(MaterialData material, @Nullable ParticipantState owner) {
    super(owner);
    this.material = material;
  }

  public BlockInfo(MaterialData material) {
    this(material, null);
  }

  public BlockInfo(BlockState block, @Nullable ParticipantState owner) {
    this(block.getData(), owner);
  }

  public BlockInfo(BlockState block) {
    this(block, null);
  }

  public MaterialData getMaterial() {
    return material;
  }

  @Override
  public String getIdentifier() {
    return getMaterial().getItemType().name();
  }

  @Override
  public Component getName() {
    return MinecraftComponent.material(getMaterial().getItemType());
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
