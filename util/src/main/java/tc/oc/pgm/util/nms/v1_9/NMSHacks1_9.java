package tc.oc.pgm.util.nms.v1_9;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.MaterialData;
import org.bukkit.plugin.Plugin;
import tc.oc.pgm.util.attribute.AttributeMap;
import tc.oc.pgm.util.attribute.AttributeModifier;
import tc.oc.pgm.util.nms.attribute.AttributeMapBukkit;
import tc.oc.pgm.util.nms.reflect.Refl;
import tc.oc.pgm.util.nms.reflect.ReflectionProxy;

public class NMSHacks1_9 extends NMSHacksProtocolLib {
  static Refl refl = ReflectionProxy.getProxy(Refl.class);
  static Refl.NBTTagString nbtTagString = ReflectionProxy.getProxy(Refl.NBTTagString.class);
  static Refl.NBTTagList nbtTagList = ReflectionProxy.getProxy(Refl.NBTTagList.class);
  static Refl.NBTTagCompound nbtTagCompound = ReflectionProxy.getProxy(Refl.NBTTagCompound.class);
  static Refl.CraftMagicNumbers craftMagicNumbers =
      ReflectionProxy.getProxy(Refl.CraftMagicNumbers.class);
  static Refl.Block reflBlock = ReflectionProxy.getProxy(Refl.Block.class);
  static Refl.CraftItemStack craftItemStack = ReflectionProxy.getProxy(Refl.CraftItemStack.class);

  @Override
  public Set<Material> getMaterialCollection(ItemMeta itemMeta, String key) {
    Map<String, Object> unhandledTags = refl.getUnhandledTags(itemMeta);
    if (!unhandledTags.containsKey(key)) return EnumSet.noneOf(Material.class);
    EnumSet<Material> materialSet = EnumSet.noneOf(Material.class);
    Object canDestroyList = unhandledTags.get(key);

    for (Object item : (List<Object>) nbtTagList.getListField(canDestroyList)) {
      String blockString = nbtTagString.getString(item);
      Object nmsBlock = reflBlock.getBlockByName(blockString);
      int id = reflBlock.getId(nmsBlock);
      Material material = Material.getMaterial(id);
      materialSet.add(material);
    }

    return materialSet;
  }

  @Override
  public void setMaterialCollection(
      ItemMeta itemMeta, Collection<Material> materials, String collectionName) {
    Map<String, Object> unhandledTags = refl.getUnhandledTags(itemMeta);
    Object canDestroyList =
        unhandledTags.containsKey(collectionName)
            ? unhandledTags.get(collectionName)
            : nbtTagList.build();
    for (Material material : materials) {
      Object block = craftMagicNumbers.getBlock(material);

      if (block != null) {
        String blockString = block.toString(); // Format: Block{what we want}
        blockString = blockString.substring(6, blockString.length() - 1);
        Object nbtString = nbtTagString.build(blockString);
        nbtTagList.add(canDestroyList, nbtString);
      }
    }
    if (!nbtTagList.isEmpty(canDestroyList)) unhandledTags.put(collectionName, canDestroyList);
  }

  @Override
  public boolean canMineBlock(MaterialData blockMaterial, ItemStack tool) {
    if (!blockMaterial.getItemType().isBlock()) {
      throw new IllegalArgumentException("Material '" + blockMaterial + "' is not a block");
    }

    Object nmsBlock = craftMagicNumbers.getBlock(blockMaterial.getItemType());
    Object nmsTool = tool == null ? null : craftMagicNumbers.getItem(tool.getType());

    Object iBlockData = reflBlock.getBlockData(nmsBlock);

    boolean alwaysDestroyable = refl.isAlwaysDestroyable(reflBlock.getMaterial(nmsBlock));
    boolean toolCanDestroy = nmsTool != null && refl.canDestroySpecialBlock(nmsTool, iBlockData);
    return nmsBlock != null && (alwaysDestroyable || toolCanDestroy);
  }

  @Override
  public AttributeMap buildAttributeMap(Player player) {
    return new AttributeMapBukkit(player);
  }

  @Override
  public void applyAttributeModifiers(
      SetMultimap<String, AttributeModifier> attributeModifiers, ItemMeta meta) {
    Object list = nbtTagList.build();
    if (!attributeModifiers.isEmpty()) {
      for (Map.Entry<String, AttributeModifier> entry : attributeModifiers.entries()) {
        AttributeModifier modifier = entry.getValue();
        Object tag = nbtTagCompound.build();
        nbtTagCompound.setString(tag, "Name", modifier.getName());
        nbtTagCompound.setDouble(tag, "Amount", modifier.getAmount());
        nbtTagCompound.setInt(tag, "Operation", modifier.getOperation().ordinal());
        nbtTagCompound.setLong(tag, "UUIDMost", modifier.getUniqueId().getMostSignificantBits());
        nbtTagCompound.setLong(tag, "UUIDLeast", modifier.getUniqueId().getLeastSignificantBits());
        nbtTagCompound.setString(tag, "AttributeName", entry.getKey());
        nbtTagList.add(list, tag);
      }

      Map<String, Object> unhandledTags = refl.getUnhandledTags(meta);

      unhandledTags.put("AttributeModifiers", list);
    }
  }

  @Override
  public SetMultimap<String, AttributeModifier> getAttributeModifiers(ItemMeta meta) {
    Map<String, Object> unhandledTags = refl.getUnhandledTags(meta);
    if (unhandledTags.containsKey("AttributeModifiers")) {
      final SetMultimap<String, AttributeModifier> attributeModifiers = HashMultimap.create();
      final Object modTags = unhandledTags.get("AttributeModifiers");
      for (int i = 0; i < nbtTagList.size(modTags); i++) {
        final Object modTag = nbtTagList.get(modTags, i);
        attributeModifiers.put(
            nbtTagCompound.getString(modTag, "AttributeName"),
            new AttributeModifier(
                new UUID(
                    nbtTagCompound.getLong(modTag, "UUIDMost"),
                    nbtTagCompound.getLong(modTag, "UUIDLeast")),
                nbtTagCompound.getString(modTag, "Name"),
                nbtTagCompound.getDouble(modTag, "Amount"),
                AttributeModifier.Operation.fromOpcode(
                    nbtTagCompound.getInt(modTag, "Operation"))));
      }
      return attributeModifiers;
    } else {
      return HashMultimap.create();
    }
  }

  @Override
  public ItemStack craftItemCopy(ItemStack item) {
    return craftItemStack.asCraftCopy(item);
  }

  @Override
  public void postToMainThread(Plugin plugin, boolean priority, Runnable task) {
    Server bukkitServer = plugin.getServer();
    Object nmsServer = refl.getNMSServer(refl.getCraftServerHandle(bukkitServer));
    refl.addCallableToMainThread(nmsServer, Executors.callable(task));
  }
}
