package tc.oc.pgm.tracker.damage;

import javax.annotation.Nullable;
import org.bukkit.block.BlockState;
import org.bukkit.material.MaterialData;
import tc.oc.component.Component;
import tc.oc.component.types.PersonalizedText;
import tc.oc.component.types.PersonalizedTranslatable;
import tc.oc.pgm.match.ParticipantState;
import tc.oc.world.NMSHacks;

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
  public Component getLocalizedName() {
    String key = NMSHacks.getTranslationKey(getMaterial());
    return key != null
        ? new PersonalizedTranslatable(key)
        : new PersonalizedText(getMaterial().getItemType().name());
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{world=" + getMaterial() + " owner=" + getOwner() + "}";
  }
}
