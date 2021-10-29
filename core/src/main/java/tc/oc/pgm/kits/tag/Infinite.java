package tc.oc.pgm.kits.tag;

import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.util.inventory.tag.ItemTag;

import javax.annotation.Nullable;

public class Infinite {
    public final boolean infinite;

    public Infinite(boolean infinite) {
        this.infinite = infinite;
    }

    private static class Tag implements ItemTag<Infinite> {
        private static final ItemTag<String> TAG = ItemTag.newString("infinite");
        @Nullable
        @Override
        public Infinite get(ItemStack item) {
            String raw = TAG.get(item);
            if (raw == null) return null;

            String[] data = raw.split("-", 1);
            if (data.length != 1) return null;

            try {
                return new Infinite(Boolean.parseBoolean(data[0]));
            } catch (NumberFormatException e) {
                return null;
            }
        }

        @Override
        public void set(ItemStack item, Infinite infinite) {
            TAG.set(item, "infinite");
        }
        @Override
        public void clear(ItemStack item) {
            TAG.clear(item);
        }
    }
    public static final ItemTag<Infinite> ITEM_TAG = new Tag();
}
