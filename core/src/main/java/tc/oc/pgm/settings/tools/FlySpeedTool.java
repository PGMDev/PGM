package tc.oc.pgm.settings.tools;

import com.google.common.collect.Lists;
import java.util.List;
import net.kyori.text.Component;
import net.kyori.text.TranslatableComponent;
import net.kyori.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import tc.oc.pgm.settings.ObserverTool;
import tc.oc.pgm.util.menu.InventoryMenu;
import tc.oc.pgm.util.text.TextTranslations;

public class FlySpeedTool implements ObserverTool {

  private static final String TRANSLATION_KEY = "setting.flyspeed.";

  @Override
  public Component getName() {
    return TranslatableComponent.of("setting.flyspeed");
  }

  @Override
  public TextColor getColor() {
    return TextColor.DARK_RED;
  }

  @Override
  public List<String> getLore(Player player) {
    Component flySpeed = FlySpeed.of(player.getFlySpeed()).getName();
    Component lore =
        TranslatableComponent.of("setting.flyspeed.lore").args(flySpeed).color(TextColor.GRAY);
    return Lists.newArrayList(TextTranslations.translateLegacy(lore, player));
  }

  @Override
  public Material getMaterial(Player player) {
    return Material.FEATHER;
  }

  @Override
  public void onClick(InventoryMenu menu, Player player, ClickType clickType) {
    FlySpeed speed = FlySpeed.of(player.getFlySpeed());
    if (clickType.isRightClick()) {
      player.setFlySpeed(speed.getPrev().getValue());
    } else {
      player.setFlySpeed(speed.getNext().getValue());
    }

    menu.refresh(player);
  }

  public static enum FlySpeed {
    NORMAL(TextColor.YELLOW, 0.1f),
    FAST(TextColor.GOLD, 0.25f),
    FASTER(TextColor.RED, 0.5f),
    HYPERSPEED(TextColor.LIGHT_PURPLE, 0.9f);

    private TextColor color;
    private float value;

    private static FlySpeed[] speeds = values();

    FlySpeed(TextColor color, float value) {
      this.color = color;
      this.value = value;
    }

    public float getValue() {
      return value;
    }

    public Component getName() {
      return TranslatableComponent.of(TRANSLATION_KEY + this.name().toLowerCase()).color(color);
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
