package tc.oc.pgm.kits;

import com.google.common.collect.SetMultimap;
import java.util.List;
import java.util.Map;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.api.attribute.Attribute;
import tc.oc.pgm.api.attribute.AttributeInstance;
import tc.oc.pgm.api.attribute.AttributeModifier;
import tc.oc.pgm.api.player.MatchPlayer;

public class AttributeKit extends AbstractKit {
  private final SetMultimap<String, AttributeModifier> modifiers;

  public AttributeKit(SetMultimap<String, AttributeModifier> modifiers) {
    this.modifiers = modifiers;
  }

  @Override
  protected void applyPostEvent(MatchPlayer player, boolean force, List<ItemStack> displacedItems) {
    for (Map.Entry<String, AttributeModifier> entry : modifiers.entries()) {
      AttributeInstance attributeValue = player.getAttribute(Attribute.byName(entry.getKey()));
      if (attributeValue != null && !attributeValue.getModifiers().contains(entry.getValue())) {
        attributeValue.addModifier(entry.getValue());
      }
    }
  }

  @Override
  public boolean isRemovable() {
    return true;
  }

  @Override
  public void remove(MatchPlayer player) {
    for (Map.Entry<String, AttributeModifier> entry : modifiers.entries()) {
      AttributeInstance attributeValue = player.getAttribute(Attribute.byName(entry.getKey()));
      if (attributeValue != null && attributeValue.getModifiers().contains(entry.getValue())) {
        attributeValue.removeModifier(entry.getValue());
      }
    }
  }
}
