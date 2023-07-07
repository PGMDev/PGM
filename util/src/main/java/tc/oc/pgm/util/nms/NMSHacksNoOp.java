package tc.oc.pgm.util.nms;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffectType;
import tc.oc.pgm.util.attribute.AttributeMap;
import tc.oc.pgm.util.nms.attribute.AttributeMapNoOp;
import tc.oc.pgm.util.nms.entity.fake.FakeEntity;
import tc.oc.pgm.util.nms.entity.fake.FakeEntityNoOp;
import tc.oc.pgm.util.nms.entity.potion.EntityPotion;
import tc.oc.pgm.util.nms.entity.potion.EntityPotionBukkit;
import tc.oc.pgm.util.reflect.MinecraftReflectionUtils;
import tc.oc.pgm.util.reflect.ReflectionUtils;

public abstract class NMSHacksNoOp implements NMSHacksPlatform {

  @Override
  public boolean isCraftItemArrowEntity(Item item) {
    return item.getType() == EntityType.ARROW;
  }

  @Override
  public EntityPotion entityPotion(Location location, ItemStack potionItem) {
    return new EntityPotionBukkit(location, potionItem);
  }

  @Override
  public PotionEffectType getPotionEffectType(String key) {
    String strippedKey = key.toLowerCase().replace(" ", "").replace("_", "");

    // Some effects can be parsed by getByName, this only has cases for those that cannot
    switch (strippedKey) {
      case "slowness":
        return PotionEffectType.SLOW;
      case "haste":
        return PotionEffectType.FAST_DIGGING;
      case "miningfatigue":
        return PotionEffectType.SLOW_DIGGING;
      case "strength":
        return PotionEffectType.INCREASE_DAMAGE;
      case "instanthealth":
        return PotionEffectType.HEAL;
      case "instantdamage":
        return PotionEffectType.HARM;
      case "jumpboost":
        return PotionEffectType.JUMP;
      case "nausea":
        return PotionEffectType.CONFUSION;
      case "resistance":
        return PotionEffectType.DAMAGE_RESISTANCE;
      default:
        return PotionEffectType.getByName(key);
    }
  }

  @Override
  public Enchantment getEnchantment(String key) {
    String strippedKey = key.toLowerCase().replace(" ", "").replace("_", "");

    switch (strippedKey) {
      case "protection":
        return Enchantment.PROTECTION_ENVIRONMENTAL;
      case "fireprotection":
        return Enchantment.PROTECTION_FIRE;
      case "featherfalling":
        return Enchantment.PROTECTION_FALL;
      case "blastprotection":
        return Enchantment.PROTECTION_EXPLOSIONS;
      case "projectileprotection":
        return Enchantment.PROTECTION_PROJECTILE;
      case "respiration":
        return Enchantment.OXYGEN;
      case "aquaaffinity":
        return Enchantment.WATER_WORKER;
      case "thorns":
        return Enchantment.THORNS;
      case "depthstrider":
        return Enchantment.DEPTH_STRIDER;
      case "sharpness":
        return Enchantment.DAMAGE_ALL;
      case "smite":
        return Enchantment.DAMAGE_UNDEAD;
      case "baneofarthropods":
        return Enchantment.DAMAGE_ARTHROPODS;
      case "knockback":
        return Enchantment.KNOCKBACK;
      case "fireaspect":
        return Enchantment.FIRE_ASPECT;
      case "looting":
        return Enchantment.LOOT_BONUS_MOBS;
      case "efficiency":
        return Enchantment.DIG_SPEED;
      case "silktouch":
        return Enchantment.SILK_TOUCH;
      case "unbreaking":
        return Enchantment.DURABILITY;
      case "fortune":
        return Enchantment.LOOT_BONUS_BLOCKS;
      case "power":
        return Enchantment.ARROW_DAMAGE;
      case "punch":
        return Enchantment.ARROW_KNOCKBACK;
      case "flame":
        return Enchantment.ARROW_FIRE;
      case "infinity":
        return Enchantment.ARROW_INFINITE;
      case "luckofthesea":
        return Enchantment.LUCK;
      case "lure":
        return Enchantment.LURE;
      default:
        return Enchantment.getByName(key);
    }
  }

  static Class<?> craftWorldClass = MinecraftReflectionUtils.getCraftBukkitClass("CraftWorld");
  static Method getHandleMethod = ReflectionUtils.getMethod(craftWorldClass, "getHandle");
  static Class<?> nmsWorldClass = MinecraftReflectionUtils.getNMSClass("World");
  static Method getTimeMethod = ReflectionUtils.getMethod(nmsWorldClass, "getTime");

  @Override
  public long getMonotonicTime(World world) {
    Object handle = ReflectionUtils.callMethod(getHandleMethod, world);
    return (long) ReflectionUtils.callMethod(getTimeMethod, handle);
  }

  @Override
  public int getPing(Player player) {
    return 100;
  }

  @Override
  public Inventory createFakeInventory(Player viewer, Inventory realInventory) {
    return realInventory instanceof DoubleChestInventory
        ? Bukkit.createInventory(viewer, realInventory.getSize())
        : Bukkit.createInventory(viewer, realInventory.getType());
  }

  @Override
  public FakeEntity fakeWitherSkull(World world) {
    return new FakeEntityNoOp();
  }

  @Override
  public FakeEntity fakeArmorStand(World world, ItemStack head) {
    return new FakeEntityNoOp();
  }

  @Override
  public ItemStack craftItemCopy(ItemStack item) {
    return item.clone();
  }

  @Override
  public Set<MaterialData> getBlockStates(Material material) {
    // TODO: MaterialData is not version compatible
    Set<MaterialData> materialDataSet = new HashSet<>();
    for (byte i = 0; i < 16; i++) {
      materialDataSet.add(new MaterialData(material, i));
    }
    return materialDataSet;
  }

  @Override
  public void setBlockStateData(BlockState state, MaterialData materialData) {
    state.setType(materialData.getItemType());
    state.setData(materialData);
  }

  @Override
  public double getTPS() {
    return 20.0;
  }

  @Override
  public AttributeMap buildAttributeMap(Player player) {
    return new AttributeMapNoOp();
  }

  @Override
  public void postToMainThread(Plugin plugin, boolean priority, Runnable task) {
    // runs the task on the next tick, not a perfect replacement
    plugin.getServer().getScheduler().runTask(plugin, task);
  }
}
