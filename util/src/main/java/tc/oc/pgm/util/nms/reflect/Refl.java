package tc.oc.pgm.util.nms.reflect;

import java.util.Map;
import java.util.concurrent.Callable;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

public interface Refl {

  @Reflect.CB("inventory.CraftMetaItem.unhandledTags")
  Map<String, Object> getUnhandledTags(ItemMeta item);

  @Reflect.CB("inventory.CraftMetaSkull.profile")
  void setSkullProfile(SkullMeta meta, Object profile);

  @Reflect.CB("entity.CraftPlayer.getHandle()")
  Object getPlayerHandle(Player player);

  @Reflect.NMS("EntityPlayer.ping")
  int getPlayerPing(Object handle);

  @Reflect.CB("CraftWorld.getHandle()")
  Object getWorldHandle(World world);

  @Reflect.NMS("World.getTime()")
  long getWorldTime(Object handle);

  @Reflect.CB("CraftServer.getHandle()")
  Object getCraftServerHandle(Server server);

  @Reflect.CB("entity.CraftLivingEntity.getHandle()")
  Object getCraftEntityHandle(LivingEntity entity);

  @Reflect.NMS("EntityLiving.getAbsorptionHearts()")
  float getAbsorptionHearts(Object entity);

  @Reflect.NMS(value = "EntityLiving.setAbsorptionHearts()", parameters = float.class)
  void setAbsorptionHearts(Object entity, float hearts);

  @Reflect.NMS("DedicatedPlayerList.getServer()")
  Object getNMSServer(Object handle);

  @Reflect.NMS(value = "MinecraftServer.a()", parameters = Callable.class)
  void addCallableToMainThread(Object nmsServer, Callable<Object> callable);

  @Reflect.NMS(value = "Item.canDestroySpecialBlock()", parameters = IBlockData.class)
  boolean canDestroySpecialBlock(Object nmsItem, Object blockData);

  @Reflect.NMS("Material.isAlwaysDestroyable")
  boolean isAlwaysDestroyable(Object material);

  @Reflect.CB("inventory.CraftItemStack")
  interface CraftItemStack {
    @Reflect.StaticMethod(value = "asCraftCopy", parameters = ItemStack.class)
    ItemStack asCraftCopy(ItemStack itemStack);
  }

  @Reflect.NMS("NBTBase")
  interface NBTBase {}

  @Reflect.NMS("NBTTagString")
  interface NBTTagString {

    @Reflect.Constructor(String.class)
    Object build(String string);

    @Reflect.Method("a_")
    @Reflect.Method("c_")
    String getString(Object item);
  };

  @Reflect.NMS("NBTTagList")
  interface NBTTagList {

    @Reflect.Field("list")
    Object getListField(Object self);

    @Reflect.Constructor
    Object build();

    @Reflect.Method("size")
    int size(Object self);

    @Reflect.Method(value = "get", parameters = int.class)
    Object get(Object self, int index);

    @Reflect.Method(value = "add", parameters = NBTBase.class)
    void add(Object self, Object value);

    @Reflect.Method("isEmpty")
    boolean isEmpty(Object self);
  }

  @Reflect.NMS("NBTTagCompound")
  interface NBTTagCompound {

    @Reflect.Constructor
    Object build();

    @Reflect.Method(value = "getString", parameters = String.class)
    String getString(Object self, String parameter);

    @Reflect.Method(value = "getLong", parameters = String.class)
    long getLong(Object self, String parameter);

    @Reflect.Method(value = "getDouble", parameters = String.class)
    double getDouble(Object self, String parameter);

    @Reflect.Method(value = "getInt", parameters = String.class)
    int getInt(Object self, String parameter);

    @Reflect.Method(
        value = "setString",
        parameters = {String.class, String.class})
    void setString(Object self, String parameter, String value);

    @Reflect.Method(
        value = "setLong",
        parameters = {String.class, long.class})
    void setLong(Object self, String parameter, long value);

    @Reflect.Method(
        value = "setDouble",
        parameters = {String.class, double.class})
    void setDouble(Object self, String parameter, double value);

    @Reflect.Method(
        value = "setInt",
        parameters = {String.class, int.class})
    void setInt(Object self, String parameter, int value);
  }

  @Reflect.CB("util.CraftMagicNumbers")
  interface CraftMagicNumbers {
    @Reflect.StaticMethod(value = "getItem", parameters = Material.class)
    Object getItem(Material material);

    @Reflect.StaticMethod(value = "getBlock", parameters = Material.class)
    Object getBlock(Material material);
  }

  @Reflect.NMS("Block")
  interface Block {

    @Reflect.StaticMethod(value = "getByName", parameters = String.class)
    Object getBlockByName(String name);

    @Reflect.StaticMethod(value = "getId", parameters = Block.class)
    int getId(Object self);

    @Reflect.Method("getBlockData")
    Object getBlockData(Object self);

    @Reflect.Method("q")
    @Reflect.Method(value = "q", parameters = IBlockData.class)
    Object getMaterial(Object self);
  }

  @Reflect.NMS("IBlockData")
  interface IBlockData {
    @Reflect.Method("getMaterial")
    Object getMaterial(Object self);
  }
}
