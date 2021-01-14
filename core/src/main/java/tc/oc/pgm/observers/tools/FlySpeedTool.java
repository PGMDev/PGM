package tc.oc.pgm.observers.tools;

import static net.kyori.adventure.text.Component.translatable;

import com.google.common.collect.Lists;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import tc.oc.pgm.util.menu.InventoryMenu;
import tc.oc.pgm.util.menu.InventoryMenuItem;
import tc.oc.pgm.util.text.TextTranslations;

public class FlySpeedTool implements InventoryMenuItem {

  private static final String TRANSLATION_KEY = "setting.flyspeed.";

  @Override
  public Component getDisplayName() {
    return translatable("setting.flyspeed", NamedTextColor.DARK_RED);
  }

  @Override
  public List<String> getLore(Player player) {
    Component flySpeed = FlySpeed.of(player.getFlySpeed()).getName();
    Component lore = translatable("setting.flyspeed.lore", NamedTextColor.GRAY, flySpeed);
    return Lists.newArrayList(TextTranslations.translateLegacy(lore, player));
  }

  @Override
  public Material getMaterial(Player player) {
    return Material.FEATHER;
  }

  @Override
  public void onInventoryClick(InventoryMenu menu, Player player, ClickType clickType) {
    FlySpeed speed = FlySpeed.of(player.getFlySpeed());
    if (clickType.isRightClick()) {
      player.setFlySpeed(speed.getPrev().getValue());
    } else {
      player.setFlySpeed(speed.getNext().getValue());
    }
    menu.refreshWindow(player);
  }

  public enum FlySpeed {
    NORMAL(NamedTextColor.YELLOW, 0.1f),
    FAST(NamedTextColor.GOLD, 0.25f),
    FASTER(NamedTextColor.RED, 0.5f),
    HYPERSPEED(NamedTextColor.LIGHT_PURPLE, 0.9f);

    private final TextColor color;
    private final float value;

    private static FlySpeed[] speeds = values();

    FlySpeed(TextColor color, float value) {
      this.color = color;
      this.value = value;
    }

    public float getValue() {
      return value;
    }

    public Component getName() {
      return translatable(TRANSLATION_KEY + this.name().toLowerCase(), color);
    }

    public FlySpeed getNext() {
      return speeds[(ordinal() + 1) % speeds.length];
    }

    public FlySpeed getPrev() {
      int index = (ordinal() == 0 ? speeds.length : ordinal()) - 1;
      return speeds[index % speeds.length];
    }

    public static FlySpeed of(float value) {
      for (FlySpeed speed : FlySpeed.values()) {
        if (speed.getValue() == value) {
          return speed;
        }
      }
      return NORMAL;
    }
  }
}
