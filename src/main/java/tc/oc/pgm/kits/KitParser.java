package tc.oc.pgm.kits;

import com.google.common.base.Splitter;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Range;
import com.google.common.collect.SetMultimap;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffect;
import org.jdom2.Element;
import org.joda.time.Duration;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.doublejump.DoubleJumpKit;
import tc.oc.pgm.filters.Filter;
import tc.oc.pgm.filters.StaticFilter;
import tc.oc.pgm.kits.tag.Grenade;
import tc.oc.pgm.kits.tag.ItemTags;
import tc.oc.pgm.projectile.ProjectileDefinition;
import tc.oc.pgm.shield.ShieldKit;
import tc.oc.pgm.shield.ShieldParameters;
import tc.oc.pgm.util.XMLUtils;
import tc.oc.server.BukkitUtils;
import tc.oc.util.Pair;
import tc.oc.xml.InvalidXMLException;
import tc.oc.xml.Node;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public abstract class KitParser {
  protected final MapFactory factory;
  protected final Set<AttributeModifier> attributeModifiers = new HashSet<>();
  protected final Set<Kit> kits = new HashSet<>();

  public KitParser(MapFactory factory) {
    this.factory = factory;
  }

  /**
   * Return all {@link AttributeModifier}s used by parsed {@link AttributeKit}s. We need to keep
   * track of these so we can remove them from players.
   */
  public Set<AttributeModifier> getAttributeModifiers() {
    return attributeModifiers;
  }

  public Set<Kit> getKits() {
    return kits;
  }

  public abstract Kit parse(Element el) throws InvalidXMLException;

  public abstract Kit parseReference(Node node, String name) throws InvalidXMLException;

  protected boolean maybeReference(Element el) {
    return "kit".equals(el.getName())
        && el.getAttribute("parent") == null
        && el.getAttribute("parents") == null
        && el.getChildren().isEmpty();
  }

  public @Nullable Kit parseKitProperty(Element el, String name) throws InvalidXMLException {
    return parseKitProperty(el, name, null);
  }

  public Kit parseKitProperty(Element el, String name, @Nullable Kit def)
      throws InvalidXMLException {
    org.jdom2.Attribute attr = el.getAttribute(name);
    Element child = XMLUtils.getUniqueChild(el, name);
    if (attr != null) {
      if (child != null) {
        throw new InvalidXMLException("Kit reference conflicts with inline kit '" + name + "'", el);
      }
      return this.parseReference(new Node(attr), attr.getValue());
    } else if (child != null) {
      return this.parse(child);
    }
    return def;
  }

  protected KitDefinition parseDefinition(Element el) throws InvalidXMLException {
    List<Kit> kits = Lists.newArrayList();

    Node attrParents = Node.fromAttr(el, "parent", "parents");
    if (attrParents != null) {
      Iterable<String> parentNames = Splitter.on(',').split(attrParents.getValue());
      for (String parentName : parentNames) {
        kits.add(parseReference(attrParents, parentName.trim()));
      }
    }

    Boolean force = XMLUtils.parseBoolean(Node.fromAttr(el, "force"));
    Boolean potionParticles = XMLUtils.parseBoolean(Node.fromAttr(el, "potion-particles"));
    Filter filter =
            factory.getFilters().parseFilterProperty(el, "filter", StaticFilter.ALLOW);

    kits.add(this.parseClearItemsKit(el)); // must be added before anything else

    for (Element child : el.getChildren("kit")) {
      kits.add(this.parse(child));
    }

    kits.add(this.parseArmorKit(el));
    kits.add(this.parseItemKit(el));
    kits.add(this.parsePotionKit(el));
    kits.add(this.parseAttributeKit(el));
    kits.add(this.parseHealthKit(el));
    kits.add(this.parseHungerKit(el));
    kits.add(this.parseKnockbackReductionKit(el));
    kits.add(this.parseWalkSpeedKit(el));
    kits.add(this.parseDoubleJumpKit(el));
    kits.add(this.parseEnderPearlKit(el));
    kits.add(this.parseFlyKit(el));
    kits.add(this.parseGameModeKit(el));
    kits.add(this.parseShieldKit(el));
    kits.addAll(this.parseRemoveKits(el));

    kits.removeAll(Collections.singleton((Kit) null)); // Remove any nulls returned above
    this.kits.addAll(kits);

    return new KitNode(kits, filter, force, potionParticles);
  }

  public KnockbackReductionKit parseKnockbackReductionKit(Element el) throws InvalidXMLException {
    Element child = el.getChild("knockback-reduction");
    if (child == null) {
      return null;
    }
    return new KnockbackReductionKit(XMLUtils.parseNumber(child, Float.class));
  }

  public WalkSpeedKit parseWalkSpeedKit(Element el) throws InvalidXMLException {
    Element child = el.getChild("walk-speed");
    if (child == null) {
      return null;
    }
    return new WalkSpeedKit(
        XMLUtils.parseNumber(child, Float.class, Range.closed(WalkSpeedKit.MIN, WalkSpeedKit.MAX)));
  }

  public ClearItemsKit parseClearItemsKit(Element el) throws InvalidXMLException {
    if ("".equals(el.getChildText("clear"))) return new ClearItemsKit(true);
    if ("".equals(el.getChildText("clear-items"))) return new ClearItemsKit(false);
    return null;
  }

  /*
   ~ <fly/>                      {FlyKit: allowFlight = true,  flying = null  }
   ~ <fly flying="false"/>       {FlyKit: allowFlight = true,  flying = false }
   ~ <fly allowFlight="false"/>  {FlyKit: allowFlight = false, flying = null  }
   ~ <fly flying="true"/>        {FlyKit: allowFlight = true,  flying = true  }
  */
  public FlyKit parseFlyKit(Element el) throws InvalidXMLException {
    Element child = el.getChild("fly");
    if (child == null) {
      return null;
    }

    boolean canFly = XMLUtils.parseBoolean(el.getAttribute("can-fly"), true);
    Boolean flying = XMLUtils.parseBoolean(el.getAttribute("flying"), null);
    org.jdom2.Attribute flySpeedAtt = el.getAttribute("fly-speed");
    float flySpeedMultiplier = 1;
    if (flySpeedAtt != null) {
      flySpeedMultiplier =
          XMLUtils.parseNumber(
              el.getAttribute("fly-speed"), Float.class, Range.closed(FlyKit.MIN, FlyKit.MAX));
    }

    return new FlyKit(canFly, flying, flySpeedMultiplier);
  }

  private ArmorKit.ArmorItem parseArmorItem(Element el) throws InvalidXMLException {
    if (el == null) {
      return null;
    }
    ItemStack stack = parseItem(el, true);
    boolean locked = XMLUtils.parseBoolean(el.getAttribute("locked"), false);
    return new ArmorKit.ArmorItem(stack, locked);
  }

  public ArmorKit parseArmorKit(Element el) throws InvalidXMLException {
    Map<ArmorType, ArmorKit.ArmorItem> armor = new HashMap<>();

    for (ArmorType armorType : ArmorType.values()) {
      ArmorKit.ArmorItem armorItem =
          this.parseArmorItem(el.getChild(armorType.name().toLowerCase()));
      if (armorItem != null) {
        armor.put(armorType, armorItem);
      }
    }

    if (!armor.isEmpty()) {
      return new ArmorKit(armor);
    } else {
      return null;
    }
  }

  public ItemKit parseItemKit(Element el) throws InvalidXMLException {
    Map<Slot, ItemStack> slotItems = Maps.newHashMap();
    List<ItemStack> freeItems = new ArrayList<>();

    for (Element itemEl : el.getChildren()) {
      ItemStack item = null;
      switch (itemEl.getName()) {
        case "item":
          item = parseItem(itemEl, true);
          break;

        case "book":
          item = parseBook(itemEl);
          break;

        case "head":
          item = parseHead(itemEl);
          break;
      }

      if (item != null) {
        Node nodeSlot = Node.fromAttr(itemEl, "slot");
        if (nodeSlot == null) {
          freeItems.add(item);
        } else {
          Slot slot = parseInventorySlot(nodeSlot);
          if (null != slotItems.put(slot, item)) {
            throw new InvalidXMLException("Kit already has an item in " + slot.getKey(), nodeSlot);
          }
        }
      }
    }

    return slotItems.isEmpty() ? null : new ItemKit(slotItems, freeItems);
  }

  public Slot parseInventorySlot(Node node) throws InvalidXMLException {
    String value = node.getValue();
    Slot slot;
    try {
      slot = Slot.Player.forIndex(Integer.parseInt(value));
      if (slot == null) {
        throw new InvalidXMLException(
            "Invalid inventory slot index (must be between 0 and 39)", node);
      }
    } catch (NumberFormatException e) {
      slot = Slot.forKey(value);
      if (slot == null) {
        throw new InvalidXMLException("Invalid inventory slot name", node);
      }
    }

    if (slot instanceof Slot.EnderChest) {
      throw new InvalidXMLException("Ender chest kits are not yet supported", node);
    }

    return slot;
  }

  public PotionKit parsePotionKit(Element el) throws InvalidXMLException {
    List<PotionEffect> potions = parsePotions(el);
    return potions.isEmpty() ? null : new PotionKit(ImmutableSet.copyOf(potions));
  }

  public List<PotionEffect> parsePotions(Element el) throws InvalidXMLException {
    List<PotionEffect> effects = new ArrayList<>();

    Node attr = Node.fromAttr(el, "potion", "potions", "effect", "effects");
    if (attr != null) {
      for (String piece : attr.getValue().split(";")) {
        effects.add(XMLUtils.parseCompactPotionEffect(attr, piece));
      }
    }

    for (Node elPotion : Node.fromChildren(el, "potion", "effect")) {
      effects.add(XMLUtils.parsePotionEffect(elPotion.getElement()));
    }

    return effects;
  }

  public AttributeKit parseAttributeKit(Element el) throws InvalidXMLException {
    SetMultimap<String, AttributeModifier> modifiers = parseAttributeModifiers(el);
    attributeModifiers.addAll(modifiers.values());
    return modifiers.isEmpty() ? null : new AttributeKit(modifiers);
  }

  public SetMultimap<String, AttributeModifier> parseAttributeModifiers(Element el)
      throws InvalidXMLException {
    SetMultimap<String, AttributeModifier> modifiers = HashMultimap.create();

    Node attr = Node.fromAttr(el, "attribute", "attributes");
    if (attr != null) {
      for (String modifierText : Splitter.on(";").split(attr.getValue())) {
        Pair<String, AttributeModifier> mod =
            XMLUtils.parseCompactAttributeModifier(attr, modifierText);
        modifiers.put(mod.first, mod.second);
      }
    }

    for (Element elAttribute : el.getChildren("attribute")) {
      Pair<String, AttributeModifier> mod = XMLUtils.parseAttributeModifier(elAttribute);
      modifiers.put(mod.first, mod.second);
    }

    return modifiers;
  }

  public ItemStack parseBook(Element el) throws InvalidXMLException {
    ItemStack itemStack = parseItem(el, Material.WRITTEN_BOOK);
    BookMeta meta = (BookMeta) itemStack.getItemMeta();
    meta.setTitle(BukkitUtils.colorize(XMLUtils.getRequiredUniqueChild(el, "title").getText()));
    meta.setAuthor(BukkitUtils.colorize(XMLUtils.getRequiredUniqueChild(el, "author").getText()));

    Element elPages = el.getChild("pages");
    if (elPages != null) {
      for (Element elPage : elPages.getChildren("page")) {
        String text = elPage.getText();
        text = text.trim(); // Remove leading and trailing whitespace
        text =
            Pattern.compile("^[ \\t]+", Pattern.MULTILINE)
                .matcher(text)
                .replaceAll(""); // Remove indentation on each line
        text =
            Pattern.compile("^\\n", Pattern.MULTILINE)
                .matcher(text)
                .replaceAll(
                    " \n"); // Add a space to blank lines, otherwise they vanish for unknown reasons
        text = BukkitUtils.colorize(text); // Color codes
        meta.addPage(text);
      }
    }

    itemStack.setItemMeta(meta);
    return itemStack;
  }

  public ItemStack parseHead(Element el) throws InvalidXMLException {
    ItemStack itemStack = parseItem(el, Material.SKULL_ITEM, (short) 3);
    SkullMeta meta = (SkullMeta) itemStack.getItemMeta();
    meta.setOwner(
        XMLUtils.parseUsername(Node.fromChildOrAttr(el, "name")),
        XMLUtils.parseUuid(Node.fromRequiredChildOrAttr(el, "uuid")),
        XMLUtils.parseUnsignedSkin(Node.fromRequiredChildOrAttr(el, "skin")));
    itemStack.setItemMeta(meta);
    return itemStack;
  }

  public ItemStack parseRequiredItem(Element parent) throws InvalidXMLException {
    ItemStack stack = parseItem(parent.getChild("item"), false);
    if (stack == null) {
      throw new InvalidXMLException("Item expected", parent);
    }
    return stack;
  }

  public ItemStack parseItem(Element el, boolean allowAir) throws InvalidXMLException {
    if (el == null) return null;

    org.jdom2.Attribute attrMaterial = el.getAttribute("material");
    Material type =
        Material.matchMaterial(attrMaterial != null ? attrMaterial.getValue() : el.getValue());
    if (type == null || (type == Material.AIR && !allowAir)) {
      throw new InvalidXMLException("Invalid material type '" + el.getValue() + "'.", el);
    }

    return parseItem(el, type);
  }

  public ItemStack parseItem(Element el, Material type) throws InvalidXMLException {
    return parseItem(
        el, type, XMLUtils.parseNumber(el.getAttribute("damage"), Short.class, (short) 0));
  }

  public ItemStack parseItem(Element el, Material type, short damage) throws InvalidXMLException {
    int amount = XMLUtils.parseNumber(el.getAttribute("amount"), Integer.class, 1);

    // must be CraftItemStack to keep track of NBT data
    ItemStack itemStack = CraftItemStack.asCraftCopy(new ItemStack(type, amount, damage));

    if (itemStack.getType() != type) {
      throw new InvalidXMLException("Invalid item/block", el);
    }

    ItemMeta meta = itemStack.getItemMeta();
    if (meta != null) { // This happens if the item is "air"
      parseItemMeta(el, meta);
      itemStack.setItemMeta(meta);
    }

    parseCustomNBT(el, itemStack);

    return itemStack;
  }

  public void parseItemMeta(Element el, ItemMeta meta) throws InvalidXMLException {
    for (Map.Entry<Enchantment, Integer> enchant : parseEnchantments(el).entrySet()) {
      meta.addEnchant(enchant.getKey(), enchant.getValue(), true);
    }

    List<PotionEffect> potions = parsePotions(el);
    if (!potions.isEmpty() && meta instanceof PotionMeta) {
      PotionMeta potionMeta = (PotionMeta) meta;

      for (PotionEffect effect : potionMeta.getCustomEffects()) {
        potionMeta.removeCustomEffect(effect.getType());
      }

      for (PotionEffect effect : potions) {
        potionMeta.addCustomEffect(effect, false);
      }
    }

    for (Map.Entry<String, AttributeModifier> entry : parseAttributeModifiers(el).entries()) {
      meta.addAttributeModifier(entry.getKey(), entry.getValue());
    }

    String customName = el.getAttributeValue("name");
    if (customName != null) {
      meta.setDisplayName(BukkitUtils.colorize(customName));
    } else if (XMLUtils.parseBoolean(el.getAttribute("grenade"), false)) {
      meta.setDisplayName("Grenade");
    }

    if (meta instanceof LeatherArmorMeta) {
      LeatherArmorMeta armorMeta = (LeatherArmorMeta) meta;
      org.jdom2.Attribute attrColor = el.getAttribute("color");
      if (attrColor != null) {
        String raw = attrColor.getValue();
        if (!raw.matches("[a-fA-F0-9]{6}")) {
          throw new InvalidXMLException("Invalid color format", attrColor);
        }
        armorMeta.setColor(Color.fromRGB(Integer.parseInt(attrColor.getValue(), 16)));
      }
    }

    String loreText = el.getAttributeValue("lore");
    if (loreText != null) {
      List<String> lore =
          ImmutableList.copyOf(Splitter.on('|').split(BukkitUtils.colorize(loreText)));
      meta.setLore(lore);
    }

    for (ItemFlag flag : ItemFlag.values()) {
      if (!XMLUtils.parseBoolean(Node.fromAttr(el, "show-" + itemFlagName(flag)), true)) {
        meta.addItemFlags(flag);
      }
    }

    if (XMLUtils.parseBoolean(el.getAttribute("unbreakable"), false)) {
      meta.spigot().setUnbreakable(true);
    }

    Element elCanDestroy = el.getChild("can-destroy");
    if (elCanDestroy != null) {
      meta.setCanDestroy(XMLUtils.parseMaterialMatcher(elCanDestroy).getMaterials());
    }

    Element elCanPlaceOn = el.getChild("can-place-on");
    if (elCanPlaceOn != null) {
      meta.setCanPlaceOn(XMLUtils.parseMaterialMatcher(elCanPlaceOn).getMaterials());
    }
  }

  String itemFlagName(ItemFlag flag) {
    switch (flag) {
      case HIDE_ATTRIBUTES:
        return "attributes";
      case HIDE_ENCHANTS:
        return "enchantments";
      case HIDE_UNBREAKABLE:
        return "unbreakable";
      case HIDE_DESTROYS:
        return "can-destroy";
      case HIDE_PLACED_ON:
        return "can-place-on";
      case HIDE_POTION_EFFECTS:
        return "other";
    }
    throw new IllegalStateException("Unknown item flag " + flag);
  }

  public void parseCustomNBT(Element el, ItemStack itemStack) throws InvalidXMLException {
    if (XMLUtils.parseBoolean(el.getAttribute("grenade"), false)) {
      Grenade.ITEM_TAG.set(
          itemStack,
          new Grenade(
              XMLUtils.parseNumber(el.getAttribute("grenade-power"), Float.class, 1f),
              XMLUtils.parseBoolean(el.getAttribute("grenade-fire"), false),
              XMLUtils.parseBoolean(el.getAttribute("grenade-destroy"), true)));
    }

    if (XMLUtils.parseBoolean(el.getAttribute("prevent-sharing"), false)) {
      ItemTags.PREVENT_SHARING.set(itemStack, true);
    }

    Node projectileNode = Node.fromAttr(el, "projectile");
    if (projectileNode != null) {
      ItemTags.PROJECTILE.set(
          itemStack,
              factory

              .getFeatures()
              .createReference(projectileNode, ProjectileDefinition.class)
              .getId());
      String name = itemStack.getItemMeta().getDisplayName();
      ItemTags.ORIGINAL_NAME.set(itemStack, name != null ? name : "");
    }
  }

  public Pair<Enchantment, Integer> parseEnchantment(Element el) throws InvalidXMLException {
    return Pair.create(
        XMLUtils.parseEnchantment(new Node(el)),
        XMLUtils.parseNumber(Node.fromAttr(el, "level"), Integer.class, 1));
  }

  public Map<Enchantment, Integer> parseEnchantments(Element el) throws InvalidXMLException {
    Map<Enchantment, Integer> enchantments = Maps.newHashMap();

    Node attr = Node.fromAttr(el, "enchantment", "enchantments");
    if (attr != null) {
      Iterable<String> enchantmentTexts = Splitter.on(";").split(attr.getValue());
      for (String enchantmentText : enchantmentTexts) {
        int level = 1;
        List<String> parts = Lists.newArrayList(Splitter.on(":").limit(2).split(enchantmentText));
        Enchantment enchant = XMLUtils.parseEnchantment(attr, parts.get(0));
        if (parts.size() > 1) {
          level = XMLUtils.parseNumber(attr, parts.get(1), Integer.class);
        }
        enchantments.put(enchant, level);
      }
    }

    for (Element elEnchantment : el.getChildren("enchantment")) {
      Pair<Enchantment, Integer> entry = parseEnchantment(elEnchantment);
      enchantments.put(entry.first, entry.second);
    }

    return enchantments;
  }

  public HealthKit parseHealthKit(Element parent) throws InvalidXMLException {
    Element el = XMLUtils.getUniqueChild(parent, "health");
    if (el == null) {
      return null;
    }

    int health = XMLUtils.parseNumber(el, Integer.class);
    if (health < 1 || health > 20) {
      throw new InvalidXMLException(
          health + " is not a valid health value, must be between 1 and 20", el);
    }

    return new HealthKit(health);
  }

  public HungerKit parseHungerKit(Element parent) throws InvalidXMLException {
    Float saturation = null;
    Element el = XMLUtils.getUniqueChild(parent, "saturation");
    if (el != null) {
      saturation = XMLUtils.parseNumber(el, Float.class);
    }

    Integer foodLevel = null;
    el = XMLUtils.getUniqueChild(parent, "foodlevel");
    if (el != null) {
      foodLevel = XMLUtils.parseNumber(el, Integer.class);
    }

    if (saturation != null || foodLevel != null) {
      return new HungerKit(saturation, foodLevel);
    } else {
      return null;
    }
  }

  public DoubleJumpKit parseDoubleJumpKit(Element parent) throws InvalidXMLException {
    Element child = XMLUtils.getUniqueChild(parent, "double-jump");

    if (child != null) {
      boolean enabled = XMLUtils.parseBoolean(child.getAttribute("enabled"), true);
      float power =
          XMLUtils.parseNumber(
              child.getAttribute("power"), Float.class, DoubleJumpKit.DEFAULT_POWER);
      Duration rechargeTime =
          XMLUtils.parseDuration(
              child.getAttribute("recharge-time"), DoubleJumpKit.DEFAULT_RECHARGE);
      boolean rechargeInAir =
          XMLUtils.parseBoolean(child.getAttribute("recharge-before-landing"), false);

      return new DoubleJumpKit(enabled, power, rechargeTime, rechargeInAir);
    } else {
      return null;
    }
  }

  public ResetEnderPearlsKit parseEnderPearlKit(Element parent) throws InvalidXMLException {
    return XMLUtils.parseBoolean(parent.getAttribute("reset-ender-pearls"), false)
        ? new ResetEnderPearlsKit()
        : null;
  }

  public Collection<RemoveKit> parseRemoveKits(Element parent) throws InvalidXMLException {
    Set<RemoveKit> kits = Collections.emptySet();
    for (Element el : parent.getChildren("remove")) {
      if (kits.isEmpty()) kits = new HashSet<>();

      Node idAttr = Node.fromAttr(el, "id");
      RemoveKit kit;
      if (idAttr != null) {
        kit = new RemoveKit(parseReference(idAttr, idAttr.getValue()));
      } else {
        kit = new RemoveKit(parse(el));
      }
      kits.add(kit);
      factory

          .getFeatures()
          .addFeature(el, kit); // So we can retrieve the node from KitModule#postParse
    }
    return kits;
  }

  public GameModeKit parseGameModeKit(Element parent) throws InvalidXMLException {
    GameMode gameMode =
        XMLUtils.parseGameMode(Node.fromNullable(parent.getChild("game-mode")), (GameMode) null);
    return gameMode == null ? null : new GameModeKit(gameMode);
  }

  public ShieldKit parseShieldKit(Element parent) throws InvalidXMLException {
    Element el = XMLUtils.getUniqueChild(parent, "shield");
    if (el == null) return null;

    double health =
        XMLUtils.parseNumber(
            el.getAttribute("health"), Double.class, ShieldParameters.DEFAULT_HEALTH);
    Duration rechargeDelay =
        XMLUtils.parseDuration(el.getAttribute("delay"), ShieldParameters.DEFAULT_DELAY);
    return new ShieldKit(new ShieldParameters(health, rechargeDelay));
  }
}
