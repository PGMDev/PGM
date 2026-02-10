package tc.oc.pgm.shops.menu;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.stream.Collectors;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NonNull;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.player.MatchPlayer;

public record Category(
    String id, ItemStack categoryIcon, Filter filter, ImmutableList<Icon> icons) {

  // Max amount of icons a category can hold
  public static final int MAX_ICONS = 28;

  public Category(String id, ItemStack categoryIcon, Filter filter, List<Icon> icons) {
    this(id, categoryIcon, filter, ImmutableList.copyOf(icons));
  }

  @Deprecated
  public String getId() {
    return id();
  }

  @Deprecated
  public ItemStack getCategoryIcon() {
    return categoryIcon();
  }

  @Deprecated
  public ImmutableList<Icon> getIcons() {
    return icons();
  }

  @Deprecated
  public Filter getFilter() {
    return filter();
  }

  public ImmutableList<Icon> getVisibleIcons(MatchPlayer player) {
    return ImmutableList.copyOf(icons.stream()
        .filter(icon -> icon.getFilter().query(player).isAllowed())
        .collect(Collectors.toList()));
  }

  @Override
  public @NonNull String toString() {
    return String.format(
        "Category{id=%s, categoryIcon=%s, icons=%s}",
        id(), categoryIcon().toString(), icons().size());
  }
}
