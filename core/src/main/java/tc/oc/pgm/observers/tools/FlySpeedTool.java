package tc.oc.pgm.observers.tools;

import com.google.common.collect.Lists;
import java.util.List;
import net.kyori.text.Component;
import net.kyori.text.TranslatableComponent;
import net.kyori.text.format.TextColor;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.menu.InventoryMenu;
import tc.oc.pgm.menu.InventoryMenuItem;
import tc.oc.pgm.util.text.TextTranslations;

public class FlySpeedTool implements InventoryMenuItem {

  private static String TRANSLATION_KEY = "setting.flyspeed.";

  @Override
  public Component getName() {
    return TranslatableComponent.of("setting.flyspeed");
  }

  @Override
  public ChatColor getColor() {
    return ChatColor.DARK_RED;
  }

  @Override
  public List<String> getLore(MatchPlayer player) {
    Component flySpeed = FlySpeed.of(player.getBukkit().getFlySpeed()).getName();
    Component lore = TranslatableComponent.of("setting.flyspeed.lore", TextColor.GRAY, flySpeed);
    return Lists.newArrayList(TextTranslations.translateLegacy(lore, player.getBukkit()));
  }

  @Override
  public Material getMaterial(MatchPlayer player) {
    return Material.FEATHER;
  }

  @Override
  public void onInventoryClick(InventoryMenu menu, MatchPlayer player, ClickType clickType) {
    FlySpeed speed = FlySpeed.of(player.getBukkit().getFlySpeed());
    if (clickType.isRightClick()) {
      player.getBukkit().setFlySpeed(speed.getPrev().getValue());
    } else {
      player.getBukkit().setFlySpeed(speed.getNext().getValue());
    }
    menu.refreshWindow(player);
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
      return TranslatableComponent.of(TRANSLATION_KEY + this.name().toLowerCase(), color);
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
