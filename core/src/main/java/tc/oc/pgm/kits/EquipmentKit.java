package tc.oc.pgm.kits;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.kits.tag.TeamColorApplicator;
import tc.oc.pgm.util.inventory.EntityEquipmentUtil;
import tc.oc.pgm.util.inventory.Slot;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;

public class EquipmentKit extends AbstractKit {
  public record EquipmentItem(ItemStack stack, @Nullable Float dropChance) {}

  private final Map<Slot, EquipmentItem> slots;

  public EquipmentKit(Map<Slot, EquipmentItem> slots) {
    this.slots = slots;
  }

  public Collection<EquipmentItem> getEquipment() {
    return slots.values();
  }

  /**
   * If force is true, existing equipment is always replaced. If false, it is never replaced. TODO:
   * repair armor, upgrade world
   */
  @Override
  public void applyPostEvent(MatchPlayer player, boolean force, List<ItemStack> displacedItems) {
    this.slots.forEach((slot, item) -> {
      if (!(slot instanceof Slot.Player plSlot)) return;
      var wearing = plSlot.getItem(player);
      if (force || wearing == null) {
        TeamColorApplicator.apply(wearing = item.stack.clone(), player);
        plSlot.setItem(player, wearing);
      }
    });
  }

  @Override
  public void apply(LivingEntity entity) {
    slots.forEach((slot, item) -> {
      slot.setEquipment(entity, item.stack.clone());
      if (item.dropChance != null) slot.setDropChance(entity, item.dropChance);
    });
  }

  @Override
  public boolean mobCompatible() {
    return true;
  }

  @Override
  public void validateMob(Class<? extends LivingEntity> mobType, Node node)
      throws InvalidXMLException {
    var equipment = EntityEquipmentUtil.EQUIPMENT;
    if (slots.containsKey(Slot.Saddle.saddle()) && !equipment.canEquipSaddle(mobType)) {
      throw new InvalidXMLException("saddle is not supported for " + mobType.getSimpleName(), node);
    }
    if (slots.containsKey(Slot.Body.body()) && !equipment.canEquipBody(mobType)) {
      throw new InvalidXMLException("body is not supported for " + mobType.getSimpleName(), node);
    }
  }
}
