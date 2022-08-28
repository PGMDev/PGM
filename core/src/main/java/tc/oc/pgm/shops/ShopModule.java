package tc.oc.pgm.shops;

import static net.kyori.adventure.text.Component.text;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import tc.oc.pgm.action.Action;
import tc.oc.pgm.action.ActionParser;
import tc.oc.pgm.action.actions.AbstractAction;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.MapTag;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.map.factory.MapModuleFactory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.filters.matcher.StaticFilter;
import tc.oc.pgm.filters.parse.FilterParser;
import tc.oc.pgm.kits.KitParser;
import tc.oc.pgm.points.PointParser;
import tc.oc.pgm.points.PointProvider;
import tc.oc.pgm.points.PointProviderAttributes;
import tc.oc.pgm.shops.menu.Category;
import tc.oc.pgm.shops.menu.Icon;
import tc.oc.pgm.shops.menu.Payment;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;
import tc.oc.pgm.util.xml.XMLUtils;

public class ShopModule implements MapModule {

  private static final Collection<MapTag> TAGS =
      ImmutableList.of(new MapTag("shops", "Shops", false, true));

  private final ImmutableMap<String, Shop> shops;
  private final ImmutableSet<ShopKeeper> shopKeepers;

  public ShopModule(Map<String, Shop> shops, Set<ShopKeeper> keepers) {
    this.shops = ImmutableMap.copyOf(shops);
    this.shopKeepers = ImmutableSet.copyOf(keepers);
  }

  @Override
  public MatchModule createMatchModule(Match match) {
    return new ShopMatchModule(match, shops, shopKeepers);
  }

  @Override
  public Collection<MapTag> getTags() {
    return TAGS;
  }

  public static class Factory implements MapModuleFactory<ShopModule> {

    @Override
    public ShopModule parse(MapFactory factory, Logger logger, Document doc)
        throws InvalidXMLException {
      KitParser kitParser = factory.getKits();
      FilterParser filterParser = factory.getFilters();
      ActionParser actionParser = new ActionParser(factory);
      PointParser pointParser = new PointParser(factory);
      Map<String, Shop> shops = Maps.newHashMap();
      Set<ShopKeeper> keepers = Sets.newHashSet();

      // Parse Shops
      for (Element shop : XMLUtils.flattenElements(doc.getRootElement(), "shops")) {
        String shopId = XMLUtils.getRequiredAttribute(shop, "id").getValue();
        String shopName = XMLUtils.getNullableAttribute(shop, "name");
        List<Category> categories = Lists.newArrayList();

        for (Element category : XMLUtils.getChildren(shop, "category")) {
          Attribute categoryId = XMLUtils.getRequiredAttribute(category, "id");
          ItemStack categoryIcon = applyItemFlags(kitParser.parseItem(category, false));
          List<Icon> icons = parseIcons(category, kitParser, actionParser, filterParser, logger);
          Filter filter = filterParser.parseFilterProperty(category, "filter", StaticFilter.ALLOW);

          categories.add(new Category(categoryId.getValue(), categoryIcon, filter, icons));
        }

        if (categories.isEmpty()) {
          throw new InvalidXMLException("At least one <category> is required per shop", shop);
        }

        Shop shopInstance = new Shop(shopId, shopName, categories);
        factory.getFeatures().addFeature(shop, shopInstance);
        shops.put(shopId, shopInstance);
      }

      // Parse Shopkeepers
      for (Element shopkeeper : XMLUtils.flattenElements(doc.getRootElement(), "shopkeepers")) {
        Attribute shopAttr = XMLUtils.getRequiredAttribute(shopkeeper, "shop");

        String shopId = shopAttr.getValue();
        String name = XMLUtils.getNullableAttribute(shopkeeper, "name");
        Class<? extends Entity> mob =
            XMLUtils.parseEntityTypeAttribute(shopkeeper, "mob", Villager.class);
        ImmutableList<PointProvider> location =
            ImmutableList.copyOf(pointParser.parse(shopkeeper, new PointProviderAttributes()));

        if (location.isEmpty()) {
          throw new InvalidXMLException("shopkeeper must have a location defined", shopkeeper);
        }

        Shop shop = shops.get(shopId);

        if (shop == null) {
          throw new InvalidXMLException(
              "No shop with id '" + shopId + "' could be found", shopkeeper);
        }

        keepers.add(new ShopKeeper(name, location, mob, shop));
      }

      return shops.isEmpty() ? null : new ShopModule(shops, keepers);
    }
  }

  private static List<Icon> parseIcons(
      Element category, KitParser kits, ActionParser actions, FilterParser filters, Logger logger)
      throws InvalidXMLException {
    List<Icon> icons = Lists.newArrayList();
    for (Element icon : XMLUtils.getChildren(category, "item")) {
      icons.add(parseIcon(icon, kits, actions, filters, logger));
    }

    if (icons.size() > Category.MAX_ICONS) {
      throw new InvalidXMLException(
          "Categories may only contain up " + Category.MAX_ICONS + " icons", category);
    }

    if (icons.isEmpty()) {
      throw new InvalidXMLException("At least one icon is required per category", category);
    }

    return icons;
  }

  private static Icon parseIcon(
      Element icon, KitParser kits, ActionParser actions, FilterParser filters, Logger logger)
      throws InvalidXMLException {

    List<Payment> payments = Lists.newArrayList();

    for (Element payment : XMLUtils.getChildren(icon, "payment")) {
      payments.add(parsePayment(payment, kits));
    }

    if (payments.isEmpty()) {
      payments.add(parsePayment(icon, kits));
    }

    ItemStack item = kits.parseItem(icon, false);
    Filter filter = filters.parseFilterProperty(icon, "filter", StaticFilter.ALLOW);

    Action<? super MatchPlayer> action;
    Node actionNode = Node.fromAttr(icon, "action", "kit");
    if (actionNode != null) {
      action = actions.parseReference(actionNode, null, MatchPlayer.class);
    } else {
      // TODO: extract to a proper give-item action, handle full inventory situation.
      action =
          new AbstractAction<MatchPlayer>(MatchPlayer.class) {
            @Override
            public void trigger(MatchPlayer player) {
              Map<Integer, ItemStack> overflow = player.getInventory().addItem(item);

              // Drop extra items at feet
              if (!overflow.isEmpty()) {
                // will be translatable in production
                player.sendWarning(
                    text(
                        "Purchase could not fit in your inventory, so it's been dropped at your feet"));
                World world = player.getWorld();
                overflow
                    .values()
                    .forEach(stack -> world.dropItemNaturally(player.getLocation(), stack));
              }
            }
          };
    }

    return new Icon(payments, item, filter, action);
  }

  private static Payment parsePayment(Element element, KitParser kits) throws InvalidXMLException {
    Node priceAttr = Node.fromAttr(element, "price");
    Node currencyAttr = Node.fromAttr(element, "currency");
    Node colorAttr = Node.fromAttr(element, "color");

    Integer price = XMLUtils.parseNumber(priceAttr, Integer.class, 0);
    Material currency =
        price <= 0 || currencyAttr == null ? null : XMLUtils.parseMaterial(currencyAttr);
    ChatColor color = XMLUtils.parseChatColor(colorAttr, ChatColor.GOLD);

    ItemStack item = null;
    Element itemEl = XMLUtils.getUniqueChild(element, "item");
    if (itemEl != null) {
      item = kits.parseItem(itemEl, false);
    }

    if (currency == null && item == null && price > 0) {
      throw new InvalidXMLException("A 'currency' attribute or child <item> is required", element);
    }

    return new Payment(currency, price, color, item);
  }

  private static ItemStack applyItemFlags(ItemStack stack) {
    ItemMeta meta = stack.getItemMeta();
    meta.addItemFlags(ItemFlag.values());
    stack.setItemMeta(meta);
    return stack;
  }
}
