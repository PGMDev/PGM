package tc.oc.pgm.entity.kits;

import java.util.Map;
import java.util.function.Consumer;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.util.inventory.ArmorType;

public record MobEquipmentKit(
    Map<ArmorType, Piece> armor,
    @Nullable Piece mainHand,
    @Nullable Piece offHand) implements MobKit {

  @Override
  public void apply(LivingEntity entity) {
    EntityEquipment equipment = entity.getEquipment();
    armor.forEach((type, piece) -> {
      switch (type) {
        case HELMET -> piece.apply(equipment::setHelmet, equipment::setHelmetDropChance);
        case CHESTPLATE ->
          piece.apply(equipment::setChestplate, equipment::setChestplateDropChance);
        case LEGGINGS -> piece.apply(equipment::setLeggings, equipment::setLeggingsDropChance);
        case BOOTS -> piece.apply(equipment::setBoots, equipment::setBootsDropChance);
      }
    });
    if (mainHand != null)
      mainHand.apply(equipment::setItemInHand, equipment::setItemInHandDropChance);
    if (offHand != null) {
      offHand.apply(
          s -> MobEquipmentUtil.EQUIPMENT.setOffHand(entity, s),
          c -> MobEquipmentUtil.EQUIPMENT.setOffHandDropChance(entity, c));
    }
  }

  public record Piece(ItemStack stack, @Nullable Float dropChance) {
    public void apply(Consumer<ItemStack> setStack, Consumer<Float> setDropChance) {
      setStack.accept(stack);
      if (dropChance != null) setDropChance.accept(dropChance);
    }
  }
}
