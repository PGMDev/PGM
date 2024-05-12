package tc.oc.pgm.util.nms;

import com.google.common.collect.SetMultimap;
import com.mojang.authlib.GameProfile;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.material.MaterialData;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import tc.oc.pgm.util.attribute.AttributeMap;
import tc.oc.pgm.util.attribute.AttributeModifier;
import tc.oc.pgm.util.nms.attribute.AttributeMapNoOp;
import tc.oc.pgm.util.nms.entity.fake.FakeEntity;
import tc.oc.pgm.util.nms.entity.fake.FakeEntityNoOp;
import tc.oc.pgm.util.nms.entity.potion.EntityPotion;
import tc.oc.pgm.util.nms.entity.potion.EntityPotionBukkit;
import tc.oc.pgm.util.nms.reflect.Refl;
import tc.oc.pgm.util.nms.reflect.ReflectionProxy;
import tc.oc.pgm.util.skin.Skin;
import tc.oc.pgm.util.skin.Skins;

public abstract class NMSHacksNoOp implements NMSHacksPlatform {
  static Refl refl = ReflectionProxy.getProxy(Refl.class);

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

  @Override
  public long getMonotonicTime(World world) {
    return refl.getWorldTime(refl.getWorldHandle(world));
  }

  @Override
  public int getPing(Player player) {
    return refl.getPlayerPing(refl.getPlayerHandle(player));
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
  public Set<Block> getBlocks(Chunk bukkitChunk, Material material) {
    Set<Block> blockSet = new HashSet<>();
    ChunkSnapshot chunkSnapshot = bukkitChunk.getChunkSnapshot();
    for (int x = 0; x < 16; x++) {
      for (int z = 0; z < 16; z++) {
        int highestBlockY = chunkSnapshot.getHighestBlockYAt(x, z);
        for (int y = 0; y < highestBlockY; y++) {
          Block block = bukkitChunk.getBlock(x, y, z);
          if (block.getType() == material) {
            blockSet.add(block);
          }
        }
      }
    }

    return blockSet;
  }

  @Override
  public void setSkullMetaOwner(SkullMeta meta, String name, UUID uuid, Skin skin) {
    GameProfile gameProfile = new GameProfile(uuid, name);
    Skins.setProperties(skin, gameProfile.getProperties());
    refl.setSkullProfile(meta, gameProfile);
  }

  @Override
  public WorldCreator detectWorld(String worldName) {
    return null; // Usage handles this nicely
  }

  @Override
  public void setAbsorption(LivingEntity entity, double health) {
    refl.setAbsorptionHearts(refl.getCraftEntityHandle(entity), (float) health);
  }

  @Override
  public double getAbsorption(LivingEntity entity) {
    return refl.getAbsorptionHearts(refl.getCraftEntityHandle(entity));
  }

  @Override
  public Skin getPlayerSkinForViewer(Player player, Player viewer) {
    return getPlayerSkin(player); // not possible here outside of sportpaper
  }

  @Override
  public String getPlayerName(UUID uuid) {
    return Bukkit.getOfflinePlayer(uuid).getName();
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
  public boolean teleportRelative(
      Player player,
      Vector deltaPos,
      float deltaYaw,
      float deltaPitch,
      PlayerTeleportEvent.TeleportCause cause) {
    // From = Players current Location
    Location from = player.getLocation();
    // To = Players new Location if Teleport is Successful
    Location to = from.clone().add(deltaPos);
    to.setYaw(to.getYaw() + deltaYaw);
    to.setPitch(to.getPitch() + deltaPitch);

    return player.teleport(to, cause);
  }

  @Override
  public void resetDimension(World world) {
    // NoOp Other dimensions dont currently work in PGM
  }

  @Override
  public void setCanDestroy(ItemMeta itemMeta, Collection<Material> materials) {
    setMaterialCollection(itemMeta, materials, "CanDestroy");
  }

  @Override
  public Set<Material> getCanDestroy(ItemMeta itemMeta) {
    return getMaterialCollection(itemMeta, "CanDestroy");
  }

  @Override
  public void setCanPlaceOn(ItemMeta itemMeta, Collection<Material> materials) {
    setMaterialCollection(itemMeta, materials, "CanPlaceOn");
  }

  @Override
  public Set<Material> getCanPlaceOn(ItemMeta itemMeta) {
    return getMaterialCollection(itemMeta, "CanPlaceOn");
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
  public void copyAttributeModifiers(ItemMeta destination, ItemMeta source) {
    SetMultimap<String, AttributeModifier> attributeModifiers = getAttributeModifiers(source);
    attributeModifiers.putAll(getAttributeModifiers(destination));
    applyAttributeModifiers(attributeModifiers, destination);
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

  @Override
  public int getMaxWorldSize(World world) {
    return 29999984; // Vanilla's default
  }
}
