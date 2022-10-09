package tc.oc.pgm.kits;

import java.util.List;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.features.FeatureDefinitionContext;
import tc.oc.pgm.features.XMLFeatureReference;
import tc.oc.pgm.util.xml.Node;

public class XMLKitReference extends XMLFeatureReference<KitDefinition> implements Kit {

  public XMLKitReference(FeatureDefinitionContext context, Node node, Class<KitDefinition> type) {
    this(context, node, null, type);
  }

  public XMLKitReference(
      FeatureDefinitionContext context, Node node, @Nullable String id, Class<KitDefinition> type) {
    super(context, node, id, type);
  }

  @Override
  public void apply(MatchPlayer player, boolean force, List<ItemStack> displacedItems) {
    get().apply(player, force, displacedItems);
  }

  @Override
  public void applyLeftover(MatchPlayer player, List<ItemStack> leftover) {
    get().applyLeftover(player, leftover);
  }

  @Override
  public boolean isRemovable() {
    return get().isRemovable();
  }

  @Override
  public void remove(MatchPlayer player) {
    get().remove(player);
  }
}
