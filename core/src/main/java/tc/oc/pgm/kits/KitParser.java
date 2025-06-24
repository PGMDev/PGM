package tc.oc.pgm.kits;

import static tc.oc.pgm.util.attribute.AttributeUtils.ATTRIBUTE_UTILS;
import static tc.oc.pgm.util.inventory.InventoryUtils.INVENTORY_UTILS;
import static tc.oc.pgm.util.nms.NMSHacks.NMS_HACKS;

import com.google.common.base.Splitter;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Range;
import com.google.common.collect.SetMultimap;
import java.time.Duration;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffect;
import org.jdom2.Element;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.action.Action;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.consumable.ConsumableDefinition;
import tc.oc.pgm.doublejump.DoubleJumpKit;
import tc.oc.pgm.filters.matcher.StaticFilter;
import tc.oc.pgm.kits.tag.Grenade;
import tc.oc.pgm.kits.tag.ItemModifier;
import tc.oc.pgm.kits.tag.ItemTags;
import tc.oc.pgm.projectile.ProjectileDefinition;
import tc.oc.pgm.shield.ShieldKit;
import tc.oc.pgm.shield.ShieldParameters;
import tc.oc.pgm.teams.TeamFactory;
import tc.oc.pgm.teams.Teams;
import tc.oc.pgm.util.bukkit.BukkitUtils;
import tc.oc.pgm.util.inventory.ArmorType;
import tc.oc.pgm.util.inventory.InventoryUtils;
import tc.oc.pgm.util.inventory.ItemMatcher;
import tc.oc.pgm.util.inventory.Slot;
import tc.oc.pgm.util.material.ItemMaterialData;
import tc.oc.pgm.util.material.MaterialData;
import tc.oc.pgm.util.material.Materials;
import tc.oc.pgm.util.xml.InheritingElement;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;
import tc.oc.pgm.util.xml.XMLUtils;

public abstract class KitParser {
  private static final Set<String> ITEM_TYPES = Set.of("item", "book", "head", "firework");

  protected final MapFactory factory;
  protected final Set<Kit> kits = new HashSet<>();

  public KitParser(MapFactory factory) {
    this.factory = factory;
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
    Filter filter = factory.getFilters().parseFilterProperty(el, "filter", StaticFilter.ALLOW);

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
    kits.add(this.parseTeamSwitchKit(el));
    kits.add(this.parseMaxHealthKit(el));
    kits.add(this.parseActionKit(el));
    kits.add(this.parseOverflowWarning(el));
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
    Element applyClear = el.getChild("clear");
    if (applyClear != null) {
      boolean items = XMLUtils.parseBoolean(applyClear.getAttribute("items"), true);
      boolean armor = XMLUtils.parseBoolean(applyClear.getAttribute("armor"), true);
      boolean effects = XMLUtils.parseBoolean(applyClear.getAttribute("effects"), false);
      return new ClearItemsKit(items, armor, effects);
    } else {
      // legacy
      if ("".equals(el.getChildText("clear-items"))) return new ClearItemsKit(true, false, false);
    }
    return null;
  }

  /*
   ~ <fly/>                      {FlyKit: allowFlight = true,  flying = null  }
   ~ <fly flying="false"/>       {FlyKit: allowFlight = true,  flying = false }
   ~ <fly allowFlight="false"/>  {FlyKit: allowFlight = false, flying = null  }
   ~ <fly flying="true"/>        {FlyKit: allowFlight = true,  flying = true  }
  */
  public FlyKit parseFlyKit(Element parent) throws InvalidXMLException {
    Element el = parent.getChild("fly");
    if (el == null) {
      return null;
    }

    boolean canFly = XMLUtils.parseBoolean(el.getAttribute("can-fly"), true);
    Boolean flying = XMLUtils.parseBoolean(el.getAttribute("flying"), null);
    org.jdom2.Attribute flySpeedAtt = el.getAttribute("fly-speed");
    float flySpeedMultiplier = 1;
    if (flySpeedAtt != null) {
      flySpeedMultiplier = XMLUtils.parseNumber(
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

    for (Element itemEl : ((InheritingElement) el).getChildren(ITEM_TYPES)) {
      ItemStack item = this.parseItemStack(itemEl);

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

    if (slotItems.isEmpty() && freeItems.isEmpty()) return null;

    boolean repairTools = XMLUtils.parseBoolean(Node.fromAttr(el, "repair-tools"), true);
    boolean deductTools = XMLUtils.parseBoolean(Node.fromAttr(el, "deduct-tools"), true);
    boolean deductItems = XMLUtils.parseBoolean(Node.fromAttr(el, "deduct-items"), true);
    boolean dropOverflow = XMLUtils.parseBoolean(Node.fromAttr(el, "drop-overflow"), false);

    return new ItemKit(slotItems, freeItems, repairTools, deductTools, deductItems, dropOverflow);
  }

  public @Nullable ItemStack parseItemStack(Element el) throws InvalidXMLException {
    return switch (el.getName()) {
      case "item" -> parseItem(el, true);
      case "book" -> parseBook(el);
      case "head" -> parseHead(el);
      case "firework" -> parseFirework(el);
      default -> null;
    };
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
    SetMultimap<Attribute, AttributeModifier> modifiers = parseAttributeModifiers(el);
    return modifiers.isEmpty() ? null : new AttributeKit(modifiers);
  }

  public SetMultimap<Attribute, AttributeModifier> parseAttributeModifiers(Element el)
      throws InvalidXMLException {
    SetMultimap<Attribute, AttributeModifier> modifiers = HashMultimap.create();

    Node attr = Node.fromAttr(el, "attribute", "attributes");
    if (attr != null) {
      for (String modifierText : Splitter.on(";").split(attr.getValue())) {
        var mod = XMLUtils.parseCompactAttributeModifier(attr, modifierText);
        modifiers.put(mod.getLeft(), mod.getRight());
      }
    }

    for (Element elAttribute : el.getChildren("attribute")) {
      var mod = XMLUtils.parseAttributeModifier(elAttribute);
      modifiers.put(mod.getLeft(), mod.getRight());
    }

    return modifiers;
  }

  public ItemStack parseBook(Element el) throws InvalidXMLException {
    ItemStack itemStack = parseItem(el, Material.WRITTEN_BOOK);
    BookMeta meta = (BookMeta) itemStack.getItemMeta();
    meta.setTitle(
        BukkitUtils.colorize(XMLUtils.getRequiredUniqueChild(el, "title").getText()));
    meta.setAuthor(
        BukkitUtils.colorize(XMLUtils.getRequiredUniqueChild(el, "author").getText()));

    Element elPages = el.getChild("pages");
    if (elPages != null) {
      for (Element elPage : elPages.getChildren("page")) {
        String text = elPage.getText();
        text = text.trim(); // Remove leading and trailing whitespace
        text = Pattern.compile("^[ \\t]+", Pattern.MULTILINE)
            .matcher(text)
            .replaceAll(""); // Remove indentation on each line
        text = Pattern.compile("^\\n", Pattern.MULTILINE)
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
    ItemStack itemStack = parseItem(el, MaterialData.item(Materials.PLAYER_HEAD, (short) 3));
    SkullMeta meta = (SkullMeta) itemStack.getItemMeta();
    NMS_HACKS.setSkullMetaOwner(
        meta,
        XMLUtils.parseUsername(Node.fromChildOrAttr(el, "name")),
        XMLUtils.parseUuid(Node.fromRequiredChildOrAttr(el, "uuid")),
        XMLUtils.parseUnsignedSkin(Node.fromRequiredChildOrAttr(el, "skin")));
    itemStack.setItemMeta(meta);
    return itemStack;
  }

  public ItemStack parseFirework(Element el) throws InvalidXMLException {
    ItemStack itemStack = parseItem(el, Materials.FIREWORK);
    FireworkMeta meta = (FireworkMeta) itemStack.getItemMeta();
    int power = XMLUtils.parseNumber(Node.fromAttr(el, "power"), Integer.class, false, 1);
    meta.setPower(power);

    for (Element explosionEl : el.getChildren("explosion")) {
      Type type = XMLUtils.parseEnum(Node.fromAttr(explosionEl, "type"), Type.class, Type.BURST);
      boolean flicker = XMLUtils.parseBoolean(Node.fromAttr(explosionEl, "flicker"), false);
      boolean trail = XMLUtils.parseBoolean(Node.fromAttr(explosionEl, "trail"), false);

      List<Color> primary = parseColors(Node.fromChildren(explosionEl, "color"));
      List<Color> fade = parseColors(Node.fromChildren(explosionEl, "fade"));

      if (primary.isEmpty()) {
        throw new InvalidXMLException("At least one <color> must be defined", explosionEl);
      }

      meta.addEffect(FireworkEffect.builder()
          .with(type)
          .withColor(primary)
          .withFade(fade)
          .flicker(flicker)
          .trail(trail)
          .build());
    }

    itemStack.setItemMeta(meta);
    return itemStack;
  }

  private List<Color> parseColors(List<Node> nodes) throws InvalidXMLException {
    List<Color> colors = new ArrayList<>(nodes.size());
    for (Node node : nodes) {
      colors.add(XMLUtils.parseHexColor(node));
    }
    return colors;
  }

  public ItemMatcher parseItemMatcher(Element parent) throws InvalidXMLException {
    return parseItemMatcher(parent, "item");
  }

  public ItemMatcher parseItemMatcher(Element parent, String childName) throws InvalidXMLException {
    ItemStack stack = parseItem(parent.getChild(childName), false);
    if (stack == null)
      throw new InvalidXMLException("Child " + childName + " element expected", parent);

    Range<Integer> amount =
        XMLUtils.parseNumericRange(Node.fromAttr(parent, "amount"), Integer.class, null);
    if (amount == null) amount = Range.atLeast(stack.getAmount());
    else if (stack.getAmount() != 1)
      throw new InvalidXMLException("Cannot combine amount range with an item amount", parent);

    boolean ignoreDurability =
        XMLUtils.parseBoolean(Node.fromAttr(parent, "ignore-durability"), true);
    boolean ignoreMetadata = XMLUtils.parseBoolean(Node.fromAttr(parent, "ignore-metadata"), false);
    boolean ignoreName =
        XMLUtils.parseBoolean(Node.fromAttr(parent, "ignore-name"), ignoreMetadata);
    boolean ignoreEnchantments =
        XMLUtils.parseBoolean(Node.fromAttr(parent, "ignore-enchantments"), ignoreMetadata);

    return new ItemMatcher(
        stack, amount, ignoreDurability, ignoreMetadata, ignoreName, ignoreEnchantments);
  }

  public ItemStack parseItem(Element el, boolean allowAir) throws InvalidXMLException {
    if (el == null) return null;

    org.jdom2.Attribute attrMaterial = el.getAttribute("material");
    String name = attrMaterial != null ? attrMaterial.getValue() : el.getValue();
    short dmg = XMLUtils.parseNumber(el.getAttribute("damage"), Short.class, (short) 0);
    var md = XMLUtils.parseItemMaterialData(new Node(el), name, dmg);

    if (md == null || (md.getItemType() == Material.AIR && !allowAir)) {
      throw new InvalidXMLException("Invalid material type '" + name + "'", el);
    }

    return parseItem(el, md);
  }

  public ItemStack parseItem(Element el, Material type) throws InvalidXMLException {
    return parseItem(
        el,
        MaterialData.item(
            type, XMLUtils.parseNumber(el.getAttribute("damage"), Short.class, (short) 0)));
  }

  public ItemStack parseItem(Element el, ItemMaterialData material) throws InvalidXMLException {
    int amount = XMLUtils.parseNumber(Node.fromAttr(el, "amount"), Integer.class, true, 1);

    // amount returns max value of integer if "oo" is given as amount
    if (amount == Integer.MAX_VALUE) amount = ItemKit.INFINITE_STACK_SIZE;

    // must be CraftItemStack to keep track of NBT data
    ItemStack itemStack = INVENTORY_UTILS.craftItemCopy(material.toItemStack(amount));

    // amount returns max value of integer if "oo" is given as amount
    if (amount == ItemKit.INFINITE_STACK_SIZE && !itemStack.getType().isBlock()) {
      throw new InvalidXMLException("infinity can only be applied to a block material", el);
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

    if (meta instanceof EnchantmentStorageMeta) {
      for (Entry<Enchantment, Integer> enchant :
          parseEnchantments(el, "stored-").entrySet()) {
        ((EnchantmentStorageMeta) meta)
            .addStoredEnchant(enchant.getKey(), enchant.getValue(), true);
      }
    }

    List<PotionEffect> potions = parsePotions(el);
    if (!potions.isEmpty() && meta instanceof PotionMeta potionMeta) {

      for (PotionEffect effect : potionMeta.getCustomEffects()) {
        potionMeta.removeCustomEffect(effect.getType());
      }

      for (PotionEffect effect : potions) {
        potionMeta.addCustomEffect(effect, false);
      }
    }

    ATTRIBUTE_UTILS.applyAttributeModifiers(parseAttributeModifiers(el), meta);

    String customName = el.getAttributeValue("name");
    if (customName != null) {
      meta.setDisplayName(BukkitUtils.colorize(customName));
    } else if (XMLUtils.parseBoolean(el.getAttribute("grenade"), false)) {
      meta.setDisplayName("Grenade");
    }

    if (meta instanceof LeatherArmorMeta armorMeta) {
      Node attrColor = Node.fromAttr(el, "color");
      if (attrColor != null) {
        armorMeta.setColor(XMLUtils.parseHexColor(attrColor));
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
      INVENTORY_UTILS.setUnbreakable(meta, true);
    }

    Element elCanDestroy = el.getChild("can-destroy");
    if (elCanDestroy != null) {
      INVENTORY_UTILS.setCanDestroy(
          meta, XMLUtils.parseMaterialMatcher(elCanDestroy).getMaterials());
    }

    Element elCanPlaceOn = el.getChild("can-place-on");
    if (elCanPlaceOn != null) {
      INVENTORY_UTILS.setCanPlaceOn(
          meta, XMLUtils.parseMaterialMatcher(elCanPlaceOn).getMaterials());
    }
  }

  String itemFlagName(ItemFlag flag) {
    return switch (flag) {
      case HIDE_ATTRIBUTES -> "attributes";
      case HIDE_ENCHANTS -> "enchantments";
      case HIDE_UNBREAKABLE -> "unbreakable";
      case HIDE_DESTROYS -> "can-destroy";
      case HIDE_PLACED_ON -> "can-place-on";
        //noinspection UnnecessaryDefault: newer versions do have extra branches
      default -> {
        if (flag == InventoryUtils.HIDE_ADDITIONAL_FLAG) yield "other";
        yield flag.name().replace("HIDE_", "").toLowerCase().replace("_", "-");
      }
    };
  }

  public void parseCustomNBT(Element el, ItemStack itemStack) throws InvalidXMLException {
    if (XMLUtils.parseBoolean(el.getAttribute("team-color"), false))
      ItemModifier.TEAM_COLOR.set(itemStack, true);

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

    if (XMLUtils.parseBoolean(el.getAttribute("locked"), false)) {
      ItemTags.LOCKED.set(itemStack, true);
    }

    if (itemStack.getAmount() == ItemKit.INFINITE_STACK_SIZE) {
      ItemTags.INFINITE.set(itemStack, true);
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

    Node consumableNode = Node.fromAttr(el, "consumable");
    if (consumableNode != null) {
      ItemTags.CONSUMABLE.set(
          itemStack,
          factory
              .getFeatures()
              .createReference(consumableNode, ConsumableDefinition.class)
              .getId());
    }
  }

  public Map.Entry<Enchantment, Integer> parseEnchantment(Element el) throws InvalidXMLException {
    return new AbstractMap.SimpleImmutableEntry<>(
        XMLUtils.parseEnchantment(new Node(el)),
        XMLUtils.parseNumber(Node.fromAttr(el, "level"), Integer.class, 1));
  }

  public Map<Enchantment, Integer> parseEnchantments(Element el) throws InvalidXMLException {
    return parseEnchantments(el, "");
  }

  public Map<Enchantment, Integer> parseEnchantments(Element el, String prefix)
      throws InvalidXMLException {
    Map<Enchantment, Integer> enchantments = Maps.newHashMap();

    Node attr = Node.fromAttr(el, prefix + "enchantment", prefix + "enchantments");
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

    for (Element elEnchantment : el.getChildren(prefix + "enchantment")) {
      Map.Entry<Enchantment, Integer> entry = parseEnchantment(elEnchantment);
      enchantments.put(entry.getKey(), entry.getValue());
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
      float power = XMLUtils.parseNumber(
          child.getAttribute("power"), Float.class, DoubleJumpKit.DEFAULT_POWER);
      Duration rechargeTime = XMLUtils.parseDuration(
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

    double health = XMLUtils.parseNumber(
        el.getAttribute("health"), Double.class, ShieldParameters.DEFAULT_HEALTH);
    Duration rechargeDelay =
        XMLUtils.parseDuration(el.getAttribute("delay"), ShieldParameters.DEFAULT_DELAY);
    return new ShieldKit(new ShieldParameters(health, rechargeDelay));
  }

  public TeamSwitchKit parseTeamSwitchKit(Element parent) throws InvalidXMLException {
    Element el = XMLUtils.getUniqueChild(parent, "team-switch");
    if (el == null) return null;

    boolean showTitle = XMLUtils.parseBoolean(el.getAttribute("show-title"), true);
    TeamFactory team = Teams.getTeam(el.getAttributeValue("team"), factory);
    if (team == null) {
      throw new InvalidXMLException(
          el.getAttributeValue("team") + " is not a valid team name!", el);
    }
    return new TeamSwitchKit(team, showTitle);
  }

  public MaxHealthKit parseMaxHealthKit(Element parent) throws InvalidXMLException {
    Element el = XMLUtils.getUniqueChild(parent, "max-health");
    if (el == null) return null;

    double maxHealth = XMLUtils.parseNumber(el, Double.class);

    if (maxHealth < 1) {
      throw new InvalidXMLException(
          maxHealth + " is not a valid max-health value, must be greater than 0", el);
    }

    return new MaxHealthKit(maxHealth);
  }

  public ActionKit parseActionKit(Element parent) throws InvalidXMLException {
    if (parent.getChildren("action").isEmpty()) return null;

    var parser = factory.getParser();
    ImmutableList.Builder<Action<? super MatchPlayer>> builder = ImmutableList.builder();
    for (Element action : parent.getChildren("action")) {
      builder.add(parser.action(MatchPlayer.class, action).required());
    }

    return new ActionKit(builder.build());
  }

  public OverflowWarningKit parseOverflowWarning(Element parent) throws InvalidXMLException {
    Node node = Node.fromChildOrAttr(parent, "overflow-warning");
    if (node == null) return null;

    return new OverflowWarningKit(XMLUtils.parseFormattedText(node));
  }
}
