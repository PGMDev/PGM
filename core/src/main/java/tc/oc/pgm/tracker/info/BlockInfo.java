package tc.oc.pgm.tracker.info;

import javax.annotation.Nullable;
import net.kyori.text.Component;
import org.bukkit.block.BlockState;
import org.bukkit.material.MaterialData;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.api.tracker.info.PhysicalInfo;
import tc.oc.pgm.util.text.MinecraftTranslations;

public class BlockInfo extends OwnerInfoBase implements PhysicalInfo {

  private final MaterialData material;

  public BlockInfo(MaterialData material, @Nullable ParticipantState owner) {
    super(owner);
    this.material = material;
  }

  public BlockInfo(MaterialData material) {
    this(material, null);
  }

  public BlockInfo(BlockState block, @Nullable ParticipantState owner) {
    this(block.getMaterialData(), owner);
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
    return MinecraftTranslations.getMaterial(getMaterial().getItemType());
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{world=" + getMaterial() + " owner=" + getOwner() + "}";
  }
}
