package tc.oc.pgm.shops;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import javax.annotation.Nullable;
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

  private final Set<Shop> shops = Sets.newHashSet();
  private final Set<ShopKeeper> shopKeepers = Sets.newHashSet();

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
      ShopModule module = new ShopModule();
      KitParser kitParser = factory.getKits();
      SetMultimap<String, KeeperData> locations = parseShopKeepers(doc.getRootElement(), logger);

      for (Element shop : XMLUtils.flattenElements(doc.getRootElement(), "shops")) {
        Attribute shopName = XMLUtils.getRequiredAttribute(shop, "name");
        List<Category> categories = Lists.newArrayList();

        for (Element category : XMLUtils.getChildren(shop, "category")) {
          Attribute categoryName = XMLUtils.getRequiredAttribute(category, "name");
          Attribute categoryMaterial = XMLUtils.getRequiredAttribute(category, "material");
          Material material = parseMaterial(categoryMaterial, category);
          List<Icon> icons = parseIcons(category, kitParser, logger);

          categories.add(new Category(categoryName.getValue(), material, icons));
        }

        if (categories.isEmpty()) {
          throw new InvalidXMLException("At least one <category> is required per shop", shop);
        }

        Shop shopInstance = new Shop(shopName.getValue(), categories);
        factory.getFeatures().addFeature(shop, shopInstance);
        module.shops.add(shopInstance);

        Set<KeeperData> shopKeeperLocations = locations.get(shopName.getValue());
        if (shopKeeperLocations.isEmpty()) {
          throw new InvalidXMLException("At least one <shopkeeper> is required per shop", shop);
        }
        shopKeeperLocations.stream()
            .map(
                data ->
                    new ShopKeeper(
                        data.getName(),
                        data.getLocation(),
                        data.getYaw(),
                        data.getPitch(),
                        data.getType(),
                        shopInstance))
            .forEach(keeper -> module.shopKeepers.add(keeper));
      }

      return !module.shops.isEmpty() ? module : null;
    }
  }

  private static SetMultimap<String, KeeperData> parseShopKeepers(Element root, Logger logger)
      throws InvalidXMLException {
    SetMultimap<String, KeeperData> shopkeepers = HashMultimap.create();

    for (Element shopkeeper : XMLUtils.flattenElements(root, "shopkeepers")) {
      Attribute locationAttr = XMLUtils.getRequiredAttribute(shopkeeper, "location");
      Attribute yawAttr = XMLUtils.getRequiredAttribute(shopkeeper, "yaw");
      Attribute pitchAttr = XMLUtils.getRequiredAttribute(shopkeeper, "pitch");
      Attribute typeAttr = XMLUtils.getRequiredAttribute(shopkeeper, "type");

      String name = XMLUtils.getNullableAttribute(shopkeeper, "name");
      Vector location = XMLUtils.parseVector(locationAttr);
      Float yaw = XMLUtils.parseNumber(yawAttr, Float.class, 0f);
      Float pitch = XMLUtils.parseNumber(pitchAttr, Float.class, 0f);
      Class<? extends Entity> mob =
          XMLUtils.parseEntityTypeAttribute(shopkeeper, "mob", Villager.class);

      shopkeepers.put(typeAttr.getValue(), new KeeperData(name, location, yaw, pitch, mob));
    }

    return shopkeepers;
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

  private static class KeeperData {
    private final String name;
    private final Vector location;
    private final Class<? extends Entity> type;
    private final Float yaw;
    private final Float pitch;

    public KeeperData(
        @Nullable String name,
        Vector location,
        Float yaw,
        Float pitch,
        Class<? extends Entity> type) {
      this.name = name;
      this.location = location;
      this.yaw = yaw;
      this.pitch = pitch;
      this.type = type;
    }

    @Nullable
    public String getName() {
      return name;
    }

    public Vector getLocation() {
      return location;
    }

    public Float getYaw() {
      return yaw;
    }

    public Float getPitch() {
      return pitch;
    }

    public Class<? extends Entity> getType() {
      return type;
    }
  }
}
