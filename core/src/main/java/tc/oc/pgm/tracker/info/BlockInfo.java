package tc.oc.pgm.tracker.info;

import net.kyori.adventure.text.Component;
import org.bukkit.block.BlockState;
import org.jspecify.annotations.Nullable;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.api.tracker.info.DamageInfo;
import tc.oc.pgm.api.tracker.info.PhysicalInfo;
import tc.oc.pgm.util.material.MaterialData;
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
    this(MaterialData.block(block), owner);
  }

  public BlockInfo(BlockState block) {
    this(block, null);
  }

  public MaterialData material() {
    return material;
  }

  @Deprecated
  public MaterialData getMaterial() {
    return material();
  }

  @Override
  public String identifier() {
    return material().getItemType().name();
  }

  @Override
  public Component name() {
    return MinecraftComponent.material(material().getItemType());
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{world=" + material() + " owner=" + owner() + "}";
  }

  @Nullable
  @Override
  public ParticipantState attacker() {
    return owner();
  }
}
