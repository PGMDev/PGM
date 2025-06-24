package tc.oc.pgm.shops;

import static net.kyori.adventure.text.Component.translatable;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import tc.oc.pgm.action.Action;
import tc.oc.pgm.action.ActionModule;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.MapTag;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.map.factory.MapModuleFactory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.kits.ItemKit;
import tc.oc.pgm.kits.KitNode;
import tc.oc.pgm.kits.OverflowWarningKit;
import tc.oc.pgm.points.PointParser;
import tc.oc.pgm.points.PointProvider;
import tc.oc.pgm.points.PointProviderAttributes;
import tc.oc.pgm.shops.menu.Category;
import tc.oc.pgm.shops.menu.Icon;
import tc.oc.pgm.shops.menu.Payment;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.XMLFluentParser;
import tc.oc.pgm.util.xml.XMLUtils;

public class ShopModule implements MapModule<ShopMatchModule> {

  private static final Collection<MapTag> TAGS = ImmutableList.of(new MapTag("shops", "Shops"));

  private final ImmutableMap<String, Shop> shops;
  private final ImmutableSet<ShopKeeper> shopKeepers;

  public ShopModule(Map<String, Shop> shops, Set<ShopKeeper> keepers) {
    this.shops = ImmutableMap.copyOf(shops);
    this.shopKeepers = ImmutableSet.copyOf(keepers);
  }

  @Override
  public ShopMatchModule createMatchModule(Match match) {
    return new ShopMatchModule(match, shops, shopKeepers);
  }

  @Override
  public Collection<MapTag> getTags() {
    return TAGS;
  }

  public static class Factory implements MapModuleFactory<ShopModule> {

    @Override
    public Collection<Class<? extends MapModule<?>>> getWeakDependencies() {
      return ImmutableList.of(ActionModule.class);
    }

    @Override
    public ShopModule parse(MapFactory factory, Logger logger, Document doc)
        throws InvalidXMLException {
      var parser = factory.getParser();
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
          ItemStack categoryIcon = applyItemFlags(parser.item(category).required());
          List<Icon> icons = parseIcons(category, parser);
          Filter filter = parser.filter(category, "filter").orAllow();

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
        PointProvider location = pointParser.parseSingle(shopkeeper, new PointProviderAttributes());

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

  private static List<Icon> parseIcons(Element category, XMLFluentParser parser)
      throws InvalidXMLException {
    List<Icon> icons = Lists.newArrayList();
    for (Element icon : XMLUtils.getChildren(category, "item")) {
      icons.add(parseIcon(icon, parser));
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

  private static Icon parseIcon(Element icon, XMLFluentParser parser) throws InvalidXMLException {

    List<Payment> payments = parsePayments(icon, parser);

    ItemStack item = parser.item(icon).required();
    Filter filter = parser.filter(icon, "filter").orAllow();

    Action<? super MatchPlayer> action = parser
        .action(MatchPlayer.class, icon, "action", "kit")
        .optional(() -> KitNode.of(
            new ItemKit(null, Collections.singletonList(item), false, false, false, true),
            new OverflowWarningKit(translatable("shop.purchase.overflow"))));

    return new Icon(payments, item, filter, action);
  }

  public static List<Payment> parsePayments(Element parent, XMLFluentParser parser)
      throws InvalidXMLException {
    List<Payment> payments = Lists.newArrayList();
    for (Element payment : XMLUtils.getChildren(parent, "payment")) {
      payments.add(parsePayment(payment, parser));
    }
    if (payments.isEmpty()) {
      payments.add(parsePayment(parent, parser));
    }
    if (payments.size()
        != payments.stream().map(Payment::getCurrency).distinct().count()) {
      throw new InvalidXMLException(
          "Payment materials must be unique within a purchasable", parent);
    }
    return payments;
  }

  public static Payment parsePayment(Element el, XMLFluentParser parser)
      throws InvalidXMLException {
    Integer price = parser.parseInt(el, "price").optional(0);
    Material currency = price <= 0 ? null : parser.material(el, "currency").orNull();
    ChatColor color = parser.parseEnum(ChatColor.class, el, "color").optional(ChatColor.GOLD);

    ItemStack item = parser.item(el, "item").child().orNull();
    if (currency == null && item == null && price > 0) {
      throw new InvalidXMLException("A 'currency' attribute or child <item> is required", el);
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
