package tc.oc.pgm.shops.menu;

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.bukkit.Material;

public class Category {

  // Max amount of icons a category can hold
  public static final int MAX_ICONS = 28;

  private String name;
  private Material material;
  private ImmutableList<Icon> icons;

  public Category(String name, Material material, List<Icon> icons) {
    this.name = name;
    this.material = material;
    this.icons = ImmutableList.copyOf(icons);
  }

  public String getName() {
    return name;
  }

  public Material getMaterial() {
    return material;
  }

  public ImmutableList<Icon> getIcons() {
    return icons;
  }

  @Override
  public String toString() {
    return String.format(
        "Category{name=%s, material=%s, icons=%s}",
        getName(), getMaterial().toString(), getIcons().size());
  }
}
