package tc.oc.pgm.shops.menu;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.api.feature.FeatureInfo;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.features.SelfIdentifyingFeatureDefinition;

@FeatureInfo(name = "category")
public class Category extends SelfIdentifyingFeatureDefinition {

  // Max amount of icons a category can hold
  public static final int MAX_ICONS = 28;

  private final ItemStack categoryIcon;
  private final ImmutableList<Supplier<Icon>> icons;
  private ImmutableList<Icon> resolved;
  private final Filter filter;

  public Category(String id, ItemStack categoryIcon, Filter filter, List<Supplier<Icon>> icons) {
    super(id);
    this.categoryIcon = categoryIcon;
    this.filter = filter;
    this.icons = ImmutableList.copyOf(icons);
  }

  public ItemStack getCategoryIcon() {
    return categoryIcon;
  }

  public ImmutableList<Icon> getIcons() {
    if (resolved == null) resolved = ImmutableList.copyOf(Lists.transform(icons, Supplier::get));
    return resolved;
  }

  public ImmutableList<Icon> getVisibleIcons(MatchPlayer player) {
    return ImmutableList.copyOf(getIcons().stream()
        .filter(icon -> icon.getFilter().query(player).isAllowed())
        .collect(Collectors.toList()));
  }

  public Filter getFilter() {
    return filter;
  }

  @Override
  public String toString() {
    return String.format(
        "Category{id=%s, categoryIcon=%s, icons=%s}",
        getId(), getCategoryIcon().toString(), icons.size());
  }
}
