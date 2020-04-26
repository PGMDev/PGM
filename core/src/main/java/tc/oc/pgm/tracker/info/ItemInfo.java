package tc.oc.pgm.tracker.info;

import javax.annotation.Nullable;
import net.kyori.text.Component;
import net.kyori.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.api.tracker.info.PhysicalInfo;
import tc.oc.pgm.util.text.MinecraftTranslations;

public class ItemInfo extends OwnerInfoBase implements PhysicalInfo {

  private static final ItemStack AIR_STACK = new ItemStack(Material.AIR);

  private final ItemStack item;

  public ItemInfo(@Nullable ItemStack item, @Nullable ParticipantState owner) {
    super(owner);
    this.item = item != null ? item : AIR_STACK;
  }

  public ItemInfo(@Nullable ItemStack item) {
    this(item, null);
  }

  public ItemStack getItem() {
    return item;
  }

  @Override
  public String getIdentifier() {
    return getItem().getType().name();
  }

  @Override
  public Component getName() {
    if (getItem().hasItemMeta()) {
      String customName = getItem().getItemMeta().getDisplayName();
      if (customName != null) {
        return LegacyComponentSerializer.legacy().deserialize(customName);
      }
    }

    return MinecraftTranslations.getMaterial(getItem().getType());
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{item=" + getItem() + " owner=" + getOwner() + "}";
  }
}
