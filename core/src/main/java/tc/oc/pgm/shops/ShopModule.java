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
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Logger;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jdom2.Document;
import org.jdom2.Element;
import tc.oc.pgm.action.Action;
import tc.oc.pgm.action.ActionModule;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.MapProtos;
import tc.oc.pgm.api.map.MapTag;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.map.factory.MapModuleFactory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.entity.MobProperties;
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
      boolean shopFeatures = factory.getProto().isNoOlderThan(MapProtos.SHOP_CATEGORY_FEATURES);

      // Parse Shops
      for (Element shop : XMLUtils.flattenElements(doc.getRootElement(), "shops")) {
        if ("category".equals(shop.getName())) {
          parseCategory(shop, factory, true);
          continue;
        }

        String shopId = parser.string(shop, "id").attr().required();
        String shopName = parser.string(shop, "name").attr().orNull();
        List<Supplier<Category>> categories = Lists.newArrayList();

        for (Element category : XMLUtils.getChildren(shop, "category")) {
          if (isCategoryReference(category)) {
            var reference =
                parser.reference(Category.class, category, "id").attr().required();
            categories.add(reference::get);
          } else {
            Category parsed = parseCategory(category, factory, shopFeatures);
            categories.add(() -> parsed);
          }
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
        var shop = parser.reference(Shop.class, shopkeeper, "shop").required();
        String name = parser.string(shopkeeper, "name").attr().orNull();
        Class<? extends Entity> mob = parser
            .node(XMLUtils::parseEntityType, shopkeeper, "mob")
            .attr()
            .optional(Villager.class);
        List<Consumer<Entity>> properties = shopFeatures
            ? MobProperties.MOB_PROPERTIES.parseAttributes(
                mob, shopkeeper, "shop", "name", "mob", "yaw", "pitch", "angle", "safe", "outdoors")
            : List.of();
        PointProvider location = pointParser.parseSingle(shopkeeper, new PointProviderAttributes());

        keepers.add(new ShopKeeper(name, location, mob, properties, shop));
      }

      return shops.isEmpty() ? null : new ShopModule(shops, keepers);
    }
  }

  private static boolean isCategoryReference(Element el) {
    return el.getChildren().isEmpty()
        && el.getAttributes().size() == 1
        && el.getAttribute("id") != null;
  }

  private static Category parseCategory(Element el, MapFactory factory, boolean register)
      throws InvalidXMLException {
    var parser = factory.getParser();
    String id = parser.string(el, "id").attr().required();
    ItemStack icon = applyItemFlags(parser.item(el).required());
    Filter filter = parser.filter(el, "filter").orAllow();
    PaymentDefaults defaults = PaymentDefaults.parse(el, parser);
    List<Icon> icons = parseIcons(el, parser, defaults);

    Category category = new Category(id, icon, filter, icons);
    if (register) factory.getFeatures().addFeature(el, category);
    return category;
  }

  private static List<Icon> parseIcons(
      Element category, XMLFluentParser parser, PaymentDefaults defaults)
      throws InvalidXMLException {
    List<Icon> icons = Lists.newArrayList();
    for (Element icon : XMLUtils.getChildren(category, "item")) {
      icons.add(parseIcon(icon, parser, defaults));
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

  private static Icon parseIcon(Element icon, XMLFluentParser parser, PaymentDefaults defaults)
      throws InvalidXMLException {
    boolean stackable = false;

    List<Payment> payments = parsePayments(icon, parser, defaults);

    ItemStack item = parser.item(icon).required();
    Filter filter = parser.filter(icon, "filter").orAllow();

    Action<? super MatchPlayer> action =
        parser.action(MatchPlayer.class, icon, "action", "kit").orNull();

    if (action == null) {
      stackable = true; // simple item, safe to buy in bulk
      action = KitNode.of(
          new ItemKit(null, Collections.singletonList(item), false, false, false, true),
          new OverflowWarningKit(translatable("shop.purchase.overflow")));
    }

    return new Icon(payments, item, filter, action, stackable);
  }

  public static List<Payment> parsePayments(Element parent, XMLFluentParser parser)
      throws InvalidXMLException {
    return parsePayments(parent, parser, null);
  }

  private static List<Payment> parsePayments(
      Element parent, XMLFluentParser parser, PaymentDefaults defaults) throws InvalidXMLException {
    List<Payment> payments = Lists.newArrayList();
    for (Element payment : XMLUtils.getChildren(parent, "payment")) {
      payments.add(parsePayment(payment, parser, defaults));
    }
    if (payments.isEmpty()) {
      payments.add(parsePayment(parent, parser, defaults));
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
    return parsePayment(el, parser, null);
  }

  private static Payment parsePayment(Element el, XMLFluentParser parser, PaymentDefaults defaults)
      throws InvalidXMLException {
    Integer price = parser.parseInt(el, "price").optional(0);
    Material currency = price <= 0 ? null : parser.material(el, "currency").orNull();
    if (currency == null && price > 0 && defaults != null) currency = defaults.currency();

    ChatColor defaultColor =
        defaults != null && defaults.color() != null ? defaults.color() : ChatColor.GOLD;
    ChatColor color = parser.parseEnum(ChatColor.class, el, "color").optional(defaultColor);

    ItemStack item = parser.item(el, "item").child().orNull();
    if (currency == null && item == null && price > 0) {
      throw new InvalidXMLException("A 'currency' attribute or child <item> is required", el);
    }

    return new Payment(currency, price, color, item);
  }

  private record PaymentDefaults(Material currency, ChatColor color) {
    static PaymentDefaults parse(Element el, XMLFluentParser parser) throws InvalidXMLException {
      return new PaymentDefaults(
          parser.material(el, "currency").attr().orNull(),
          parser.parseEnum(ChatColor.class, el, "payment-color").attr().orNull());
    }
  }

  private static ItemStack applyItemFlags(ItemStack stack) {
    ItemMeta meta = stack.getItemMeta();
    meta.addItemFlags(ItemFlag.values());
    stack.setItemMeta(meta);
    return stack;
  }
}
