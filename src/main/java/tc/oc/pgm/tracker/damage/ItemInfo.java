package tc.oc.pgm.tracker.damage;

import javax.annotation.Nullable;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import tc.oc.component.Component;
import tc.oc.component.types.PersonalizedText;
import tc.oc.component.types.PersonalizedTranslatable;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.util.components.Components;
import tc.oc.world.NMSHacks;

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
  public Component getLocalizedName() {
    if (getItem().hasItemMeta()) {
      String customName = getItem().getItemMeta().getDisplayName();
      if (customName != null) {
        return Components.fromLegacyText(customName);
      }
    }

    String key = NMSHacks.getTranslationKey(getItem());
    return key != null
        ? new PersonalizedTranslatable(key)
        : new PersonalizedText(getItem().getType().name());
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{item=" + getItem() + " owner=" + getOwner() + "}";
  }
}
