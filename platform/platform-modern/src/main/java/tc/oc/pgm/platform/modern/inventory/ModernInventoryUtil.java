package tc.oc.pgm.platform.modern.inventory;

import static tc.oc.pgm.util.platform.Supports.Variant.PAPER;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.item.ItemParser;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import org.bukkit.Material;
import org.bukkit.craftbukkit.CraftRegistry;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.craftbukkit.inventory.CraftItemType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import tc.oc.pgm.util.bukkit.ComponentApplicator;
import tc.oc.pgm.util.inventory.InventoryUtils;
import tc.oc.pgm.util.platform.Supports;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;

@Supports(value = PAPER, minVersion = "1.21.11")
public class ModernInventoryUtil implements InventoryUtils.InventoryUtilsPlatform {

  @Override
  public Collection<PotionEffect> getPotionEffects(ItemStack item) {
    if (item.getItemMeta() instanceof PotionMeta potion) {
      var base = potion.getBasePotionType();
      if (base != null) return base.getPotionEffects();
    }
    return Collections.emptyList();
  }

  @Override
  public boolean isUnbreakable(ItemMeta meta) {
    return meta.isUnbreakable();
  }

  @Override
  public void setUnbreakable(ItemMeta meta, boolean unbreakable) {
    meta.setUnbreakable(unbreakable);
  }

  @Override
  public boolean openVillager(Villager villager, Player viewer) {
    viewer.openMerchant((Villager) villager.copy(), true);
    return true;
  }

  @Override
  public ItemStack craftItemCopy(ItemStack item) {
    return CraftItemStack.asCraftCopy(item);
  }

  @Override
  public EquipmentSlot getUsedHand(Event event) {
    return switch (event) {
      case PlayerItemConsumeEvent e -> e.getHand();
      case PlayerInteractEvent e -> e.getHand();
      case BlockPlaceEvent e -> e.getHand();
      default -> EquipmentSlot.HAND;
    };
  }

  @Override
  @SuppressWarnings("removal")
  public void setCanDestroy(ItemMeta itemMeta, Set<Material> materials) {
    itemMeta.setCanDestroy(materials);
  }

  @Override
  @SuppressWarnings("removal")
  public Set<Material> getCanDestroy(ItemMeta itemMeta) {
    return itemMeta.getCanDestroy();
  }

  @Override
  @SuppressWarnings("removal")
  public void setCanPlaceOn(ItemMeta itemMeta, Set<Material> materials) {
    itemMeta.setCanPlaceOn(materials);
  }

  @Override
  @SuppressWarnings("removal")
  public Set<Material> getCanPlaceOn(ItemMeta itemMeta) {
    return itemMeta.getCanPlaceOn();
  }

  @Override
  public boolean isViewable(Inventory inventory) {
    return inventory.getType().isCreatable();
  }

  private static final Registry<Item> ITEMS =
      CraftRegistry.getMinecraftRegistry().lookupOrThrow(Registries.ITEM);
  private static final ItemParser ITEM_PARSER =
      new ItemParser(Commands.createValidationContext(CraftRegistry.getMinecraftRegistry()));

  @Override
  public ComponentApplicator buildComponentApplicator(Material type, Node components)
      throws InvalidXMLException {
    try {
      var nmsItem = CraftItemType.bukkitToMinecraft(type);
      var key = ITEMS.getKey(nmsItem);
      if (key == null) throw new IllegalStateException("Invalid material: " + type);

      var str = new StringReader(key.toShortString() + "[" + components.getValueNormalize() + "]");
      var patch = ITEM_PARSER.parse(str).components();
      return is -> CraftItemStack.unwrap(is).applyComponents(patch);
    } catch (CommandSyntaxException ex) {
      throw new InvalidXMLException("Failed to parse components", components, ex);
    }
  }
}
