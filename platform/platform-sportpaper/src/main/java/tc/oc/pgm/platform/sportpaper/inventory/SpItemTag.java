package tc.oc.pgm.platform.sportpaper.inventory;

import java.util.function.Function;
import net.minecraft.server.v1_8_R3.NBTBase;
import net.minecraft.server.v1_8_R3.NBTTagByte;
import net.minecraft.server.v1_8_R3.NBTTagByteArray;
import net.minecraft.server.v1_8_R3.NBTTagDouble;
import net.minecraft.server.v1_8_R3.NBTTagFloat;
import net.minecraft.server.v1_8_R3.NBTTagInt;
import net.minecraft.server.v1_8_R3.NBTTagIntArray;
import net.minecraft.server.v1_8_R3.NBTTagLong;
import net.minecraft.server.v1_8_R3.NBTTagShort;
import net.minecraft.server.v1_8_R3.NBTTagString;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftMetaItem;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.util.inventory.tag.ItemTag;

/**
 * An item tag that encodes data in an item meta's "unhandled" tag map, inspired by the modern
 * persistent data container API
 */
public class SpItemTag<T, N extends NBTBase> implements ItemTag<T> {
  private final String key;
  private final Codec<T, N> type;

  SpItemTag(String key, Codec<T, N> type) {
    this.key = "PGM:" + key;
    this.type = type;
  }

  @Nullable
  @Override
  @SuppressWarnings("unchecked")
  public T get(ItemStack item) {
    if (!item.hasItemMeta()) return null;
    var meta = item.getItemMeta();
    if (!(meta instanceof CraftMetaItem cbMeta)) return null;
    var tag = cbMeta.getUnhandledTags().get(key);

    if (tag == null) return null;

    return type.fromNbt.apply((N) tag);
  }

  @Override
  public void set(ItemStack item, T value) {
    var meta = item.hasItemMeta()
        ? item.getItemMeta()
        : Bukkit.getItemFactory().getItemMeta(item.getType());

    if (meta instanceof CraftMetaItem cbMeta) {
      cbMeta.getUnhandledTags().put(key, type.toNbt.apply(value));
      item.setItemMeta(cbMeta);
    }
  }

  @Override
  public void clear(ItemStack item) {
    var meta = item.getItemMeta();
    if (meta instanceof CraftMetaItem cbMeta) {
      cbMeta.getUnhandledTags().remove(key);
      item.setItemMeta(cbMeta);
    }
  }

  /** Represents a transform between a runtime type and its NBT representation */
  public record Codec<RT, NBT extends NBTBase>(Function<RT, NBT> toNbt, Function<NBT, RT> fromNbt) {
    public static Codec<Byte, NBTTagByte> BYTE = new Codec<>(NBTTagByte::new, NBTTagByte::f);
    public static Codec<Short, NBTTagShort> SHORT = new Codec<>(NBTTagShort::new, NBTTagShort::e);
    public static Codec<Integer, NBTTagInt> INTEGER = new Codec<>(NBTTagInt::new, NBTTagInt::d);
    public static Codec<Long, NBTTagLong> LONG = new Codec<>(NBTTagLong::new, NBTTagLong::c);
    public static Codec<Float, NBTTagFloat> FLOAT = new Codec<>(NBTTagFloat::new, NBTTagFloat::h);
    public static Codec<Double, NBTTagDouble> DOUBLE =
        new Codec<>(NBTTagDouble::new, NBTTagDouble::g);
    public static Codec<byte[], NBTTagByteArray> BYTE_ARRAY =
        new Codec<>(NBTTagByteArray::new, NBTTagByteArray::c);
    public static Codec<String, NBTTagString> STRING =
        new Codec<>(NBTTagString::new, NBTTagString::a_);
    public static Codec<Boolean, NBTTagByte> BOOLEAN =
        new Codec<>(b -> new NBTTagByte((byte) (b ? 1 : 0)), byteTag -> byteTag.f() != 0);
    public static Codec<int[], NBTTagIntArray> INT_ARRAY =
        new Codec<>(NBTTagIntArray::new, NBTTagIntArray::c);
  }
}
