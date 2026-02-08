package tc.oc.pgm.shops.menu;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Getter;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.player.MatchPlayer;

@Getter
public class Category {

  // Max amount of icons a category can hold
  public static final int MAX_ICONS = 28;

  private final String id;
  private final ItemStack categoryIcon;
  private final ImmutableList<Icon> icons;
  private final Filter filter;

  public Category(String id, ItemStack categoryIcon, Filter filter, List<Icon> icons) {
    this.id = id;
    this.categoryIcon = categoryIcon;
    this.filter = filter;
    this.icons = ImmutableList.copyOf(icons);
  }

  public ImmutableList<Icon> getVisibleIcons(MatchPlayer player) {
    return ImmutableList.copyOf(icons.stream()
        .filter(icon -> icon.getFilter().query(player).isAllowed())
        .collect(Collectors.toList()));
  }

  @Override
  public String toString() {
    return String.format(
        "Category{id=%s, categoryIcon=%s, icons=%s}",
        getId(), getCategoryIcon().toString(), getIcons().size());
  }
}
