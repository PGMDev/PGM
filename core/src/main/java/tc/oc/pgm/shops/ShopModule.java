package tc.oc.pgm.shops;

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
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.MapTag;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.map.factory.MapModuleFactory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.kits.ItemKit;
import tc.oc.pgm.kits.Kit;
import tc.oc.pgm.kits.KitParser;
import tc.oc.pgm.shops.menu.Category;
import tc.oc.pgm.shops.menu.Icon;
import tc.oc.pgm.util.material.Materials;
import tc.oc.pgm.util.xml.InvalidXMLException;
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
      Map<String, Shop> shops = Maps.newHashMap();
      Set<ShopKeeper> keepers = Sets.newHashSet();

      for (Element shop : XMLUtils.flattenElements(doc.getRootElement(), "shops")) {
        Attribute shopId = XMLUtils.getRequiredAttribute(shop, "id");
        String shopName = XMLUtils.getNullableAttribute(shop, "name");
        List<Category> categories = Lists.newArrayList();

        for (Element category : XMLUtils.getChildren(shop, "category")) {
          Attribute categoryId = XMLUtils.getRequiredAttribute(category, "id");
          Attribute categoryMaterial = XMLUtils.getRequiredAttribute(category, "material");
          Material material = parseMaterial(categoryMaterial, category);
          List<Icon> icons = parseIcons(category, kitParser, logger);

          categories.add(new Category(categoryId.getValue(), material, icons));
        }

        if (categories.isEmpty()) {
          throw new InvalidXMLException("At least one <category> is required per shop", shop);
        }

        Shop shopInstance = new Shop(shopId.getValue(), shopName, categories);
        factory.getFeatures().addFeature(shop, shopInstance);
        shops.put(shopInstance.getId(), shopInstance);
      }

      keepers = parseShopKeepers(shops, doc.getRootElement(), logger);

      return shops.isEmpty() ? null : new ShopModule(shops, keepers);
    }
  }

  private static Set<ShopKeeper> parseShopKeepers(
      Map<String, Shop> shops, Element root, Logger logger) throws InvalidXMLException {
    Set<ShopKeeper> keepers = Sets.newHashSet();

    for (Element shopkeeper : XMLUtils.flattenElements(root, "shopkeepers")) {
      Attribute shopAttr = XMLUtils.getRequiredAttribute(shopkeeper, "shop");
      Attribute locationAttr = XMLUtils.getRequiredAttribute(shopkeeper, "location");
      Attribute yawAttr = XMLUtils.getRequiredAttribute(shopkeeper, "yaw");
      Attribute pitchAttr = XMLUtils.getRequiredAttribute(shopkeeper, "pitch");

      String shopId = shopAttr.getValue();
      String name = XMLUtils.getNullableAttribute(shopkeeper, "name");
      Vector location = XMLUtils.parseVector(locationAttr);
      Float yaw = XMLUtils.parseNumber(yawAttr, Float.class, 0f);
      Float pitch = XMLUtils.parseNumber(pitchAttr, Float.class, 0f);
      Class<? extends Entity> mob =
          XMLUtils.parseEntityTypeAttribute(shopkeeper, "mob", Villager.class);

      if (!shops.containsKey(shopId)) {
        throw new InvalidXMLException(
            "No shop with id '" + shopId + "' could be found!", shopkeeper);
      }
      Shop shop = shops.get(shopId);

      keepers.add(new ShopKeeper(name, location, yaw, pitch, mob, shop));
    }

    return keepers;
  }

  private static List<Icon> parseIcons(Element category, KitParser kits, Logger logger)
      throws InvalidXMLException {
    List<Icon> icons = Lists.newArrayList();
    for (Element icon : XMLUtils.getChildren(category, "item")) {
      icons.add(parseIcon(icon, kits, logger));
    }

    if (icons.size() > Category.MAX_ICONS) {
      throw new InvalidXMLException(
          "Categories may only contain " + Category.MAX_ICONS + " icons", category);
    }

    if (icons.isEmpty()) {
      throw new InvalidXMLException("At least one <item> is required per category", category);
    }

    return icons;
  }

  private static Icon parseIcon(Element icon, KitParser kits, Logger logger)
      throws InvalidXMLException {
    Material currency = parseMaterial(XMLUtils.getRequiredAttribute(icon, "currency"), icon);
    Integer price =
        XMLUtils.parseNumber(XMLUtils.getRequiredAttribute(icon, "price"), Integer.class);
    ItemStack item = kits.parseItem(icon, false);

    Kit kit = null;
    Attribute kitAttr = icon.getAttribute("kit");
    if (kitAttr != null) {
      kit = kits.parseKitProperty(icon, "kit", null);
    } else {
      kit = new ItemKit(Maps.newHashMap(), Lists.newArrayList(item));
    }

    return new Icon(currency, price, item, kit);
  }

  private static Material parseMaterial(Attribute attribute, Element el)
      throws InvalidXMLException {
    if (attribute == null) return null;
    Material type = Materials.parseMaterial(attribute.getValue());
    if (type == null || (type == Material.AIR)) {
      throw new InvalidXMLException("Invalid material type '" + attribute.getValue() + "'", el);
    }
    return type;
  }
}
