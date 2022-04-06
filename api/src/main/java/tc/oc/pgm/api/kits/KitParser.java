package tc.oc.pgm.api.kits;

import com.google.common.collect.SetMultimap;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.jdom2.Element;
import tc.oc.pgm.api.attribute.AttributeModifier;
import tc.oc.pgm.api.xml.InvalidXMLException;
import tc.oc.pgm.api.xml.Node;

public interface KitParser {
  Set<AttributeModifier> getAttributeModifiers();

  Set<Kit> getKits();

  Kit parse(Element el) throws InvalidXMLException;

  Kit parseReference(Node node, String name) throws InvalidXMLException;

  @Nullable
  Kit parseKitProperty(Element el, String name) throws InvalidXMLException;

  Kit parseKitProperty(Element el, String name, @Nullable Kit def) throws InvalidXMLException;

  Kit parseKnockbackReductionKit(Element el) throws InvalidXMLException;

  Kit parseWalkSpeedKit(Element el) throws InvalidXMLException;

  Kit parseClearItemsKit(Element el) throws InvalidXMLException;

  /*
   ~ <fly/>                      {FlyKit: allowFlight = true,  flying = null  }
   ~ <fly flying="false"/>       {FlyKit: allowFlight = true,  flying = false }
   ~ <fly allowFlight="false"/>  {FlyKit: allowFlight = false, flying = null  }
   ~ <fly flying="true"/>        {FlyKit: allowFlight = true,  flying = true  }
  */
  Kit parseFlyKit(Element el) throws InvalidXMLException;

  Kit parseArmorKit(Element el) throws InvalidXMLException;

  Kit parseItemKit(Element el) throws InvalidXMLException;

  Slot parseInventorySlot(Node node) throws InvalidXMLException;

  Kit parsePotionKit(Element el) throws InvalidXMLException;

  List<PotionEffect> parsePotions(Element el) throws InvalidXMLException;

  Kit parseAttributeKit(Element el) throws InvalidXMLException;

  SetMultimap<String, AttributeModifier> parseAttributeModifiers(Element el)
      throws InvalidXMLException;

  ItemStack parseBook(Element el) throws InvalidXMLException;

  ItemStack parseHead(Element el) throws InvalidXMLException;

  ItemStack parseRequiredItem(Element parent) throws InvalidXMLException;

  ItemStack parseItem(Element el, boolean allowAir) throws InvalidXMLException;

  ItemStack parseItem(Element el, Material type) throws InvalidXMLException;

  ItemStack parseItem(Element el, Material type, short damage) throws InvalidXMLException;

  void parseItemMeta(Element el, ItemMeta meta) throws InvalidXMLException;

  void parseCustomNBT(Element el, ItemStack itemStack) throws InvalidXMLException;

  Map.Entry<Enchantment, Integer> parseEnchantment(Element el) throws InvalidXMLException;

  Map<Enchantment, Integer> parseEnchantments(Element el) throws InvalidXMLException;

  Map<Enchantment, Integer> parseEnchantments(Element el, String prefix) throws InvalidXMLException;

  Kit parseHealthKit(Element parent) throws InvalidXMLException;

  Kit parseHungerKit(Element parent) throws InvalidXMLException;

  Kit parseDoubleJumpKit(Element parent) throws InvalidXMLException;

  Kit parseEnderPearlKit(Element parent) throws InvalidXMLException;

  Collection<Kit> parseRemoveKits(Element parent) throws InvalidXMLException;

  Kit parseGameModeKit(Element parent) throws InvalidXMLException;

  Kit parseShieldKit(Element parent) throws InvalidXMLException;

  Kit parseTeamSwitchKit(Element parent) throws InvalidXMLException;

  Kit parseMaxHealthKit(Element parent) throws InvalidXMLException;
}
