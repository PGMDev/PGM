package tc.oc.pgm.util.inventory.tag;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

/**
 * An item tag that encodes data in an item's lore.
 *
 * <p>Bukkit 1.13 or earlier does not have access to a persistent item API, so we have to provide a
 * hacky way of attaching data to items.
 *
 * <p>Strings are encoded as hex and added as color codes in an item's lore. Clients are unable to
 * see the data since the codes are not attached to any string.
 */
final class LegacyItemTag implements ItemTag<String> {

  private static final AtomicInteger IDS = new AtomicInteger();
  private static final Charset CHARSET = StandardCharsets.US_ASCII;
  private static final String SEQUENCE = ChatColor.MAGIC + "%s" + ChatColor.MAGIC;
  private static final Pattern SPLITTER = Pattern.compile("(.{1})");
  private static final Pattern SEPARATOR = Pattern.compile(String.valueOf(ChatColor.COLOR_CHAR));
  private static final Pattern ESCAPE = Pattern.compile(String.valueOf('`'));

  private final String sequence; // A string that appears before and after the encoded data
  private final Pattern regex; // A pattern to match the data, 1st group is the encoded data

  LegacyItemTag() {
    this.sequence = String.format(SEQUENCE, nextId());
    this.regex = Pattern.compile(sequence + "(.*)" + sequence);
  }

  @Nullable
  @Override
  public String get(ItemStack item) {
    if (!item.hasItemMeta()) return null;
    final List<String> lore = item.getItemMeta().getLore();
    if (lore == null || lore.isEmpty()) return null;

    final Matcher matcher = regex.matcher(lore.get(0));
    return matcher.find() ? decode(matcher.group(1)) : null;
  }

  @Override
  public void set(ItemStack item, String value) {
    ItemMeta itemMeta = item.getItemMeta();
    if (!item.hasItemMeta()) {
      // Create missing item meta if none is found
      itemMeta = Bukkit.getItemFactory().getItemMeta(item.getType());
    }
    List<String> lore = itemMeta.getLore();

    // If the item has no lore, ensure there is at least 1 line.
    if (lore == null || lore.isEmpty()) {
      itemMeta.setLore(Collections.singletonList(""));
      lore = itemMeta.getLore();
    }

    lore.set(0, sequence + encode(value) + sequence + regex.matcher(lore.get(0)).replaceAll(""));
    itemMeta.setLore(lore);
    item.setItemMeta(itemMeta);
  }

  @Override
  public void clear(ItemStack item) {
    if (!item.hasItemMeta()) return;
    ItemMeta itemMeta = item.getItemMeta();
    final List<String> lore = itemMeta.getLore();
    if (lore == null || lore.isEmpty()) return;

    String line = lore.get(0);
    lore.set(0, line = regex.matcher(line).replaceAll(""));
    if (line.isEmpty()) lore.remove(0);

    itemMeta.setLore(lore);
    item.setItemMeta(itemMeta);
  }

  private static ChatColor nextId() {
    ChatColor[] colors = ChatColor.values();
    int id = IDS.getAndIncrement();

    // Unfortunately, there can only be as many item tags as there are color codes.
    if (id >= colors.length) throw new IllegalStateException("Exhausted item tag space");

    return colors[id];
  }

  private static String encode(String decoded) {
    if (decoded.isEmpty()) return decoded;
    return SPLITTER
        .matcher(
            Hex.encodeHexString(
                SEPARATOR.matcher(decoded).replaceAll(ESCAPE.pattern()).getBytes(CHARSET)))
        .replaceAll(SEPARATOR.pattern() + "$1");
  }

  @Nullable
  private static String decode(String encoded) {
    try {
      return ESCAPE
          .matcher(
              new String(
                  Hex.decodeHex(SEPARATOR.matcher(encoded).replaceAll("").toCharArray()), CHARSET))
          .replaceAll(SEPARATOR.pattern());
    } catch (DecoderException e) {
      return null; // Since items can have arbitrary lore, silence any errors.
    }
  }
}
