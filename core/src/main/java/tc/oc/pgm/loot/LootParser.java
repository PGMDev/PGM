package tc.oc.pgm.loot;

import org.bukkit.inventory.ItemStack;
import org.jdom2.Element;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.util.compose.CompositionParser;
import tc.oc.pgm.util.xml.InvalidXMLException;

public class LootParser extends CompositionParser<ItemStack> {

  public LootParser(MapFactory factory) {
    super(factory);
  }

  @Override
  public ItemStack parseUnit(Element element) throws InvalidXMLException {
    return this.factory.getKits().parseItemStack(element);
  }
}
