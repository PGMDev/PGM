package tc.oc.pgm.shops.menu;

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.bukkit.inventory.ItemStack;

public class Category {

  // Max amount of icons a category can hold
  public static final int MAX_ICONS = 28;

  private final String id;
  private final ItemStack categoryIcon;
  private final ImmutableList<Icon> icons;

  public Category(String id, ItemStack categoryIcon, List<Icon> icons) {
    this.id = id;
    this.categoryIcon = categoryIcon;
    this.icons = ImmutableList.copyOf(icons);
  }

  public String getId() {
    return id;
  }

  public ItemStack getCategoryIcon() {
    return categoryIcon;
  }

  public ImmutableList<Icon> getIcons() {
    return icons;
  }

  @Override
  public String toString() {
    return String.format(
        "Category{id=%s, categoryIcon=%s, icons=%s}",
        getId(), getCategoryIcon().toString(), getIcons().size());
  }
}
