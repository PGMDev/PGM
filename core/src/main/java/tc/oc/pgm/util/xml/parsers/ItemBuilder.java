package tc.oc.pgm.util.xml.parsers;

import org.bukkit.inventory.ItemStack;
import org.jdom2.Element;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.kits.KitParser;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;

public class ItemBuilder extends Builder<ItemStack, ItemBuilder> {
  private final KitParser kits;
  boolean allowAir = false;

  public ItemBuilder(KitParser kits, @Nullable Element el, String... prop) {
    super(el, prop);
    this.kits = kits;
  }

  public ItemBuilder allowAir() {
    allowAir = true;
    return this;
  }

  @Override
  protected Node getNode(boolean required) throws InvalidXMLException {
    return Node.from(false, child, self, required, el, prop);
  }

  @Override
  protected ItemStack parse(Node node) throws InvalidXMLException {
    return kits.parseItem(node.getElement(), allowAir);
  }

  @Override
  protected ItemBuilder getThis() {
    return this;
  }
}
