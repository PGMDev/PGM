package tc.oc.pgm.settings.tools;

import com.google.common.collect.Lists;
import java.util.List;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import tc.oc.pgm.settings.ObserverTool;
import tc.oc.pgm.util.component.Component;
import tc.oc.pgm.util.component.ComponentRenderers;
import tc.oc.pgm.util.component.types.PersonalizedTranslatable;
import tc.oc.pgm.util.menu.InventoryMenu;

public class FlySpeedTool implements ObserverTool {

  private static final String TRANSLATION_KEY = "setting.flyspeed.";

  @Override
  public Component getName() {
    return new PersonalizedTranslatable("setting.flyspeed");
  }

  @Override
  public ChatColor getColor() {
    return ChatColor.DARK_RED;
  }

  @Override
  public List<String> getLore(Player player) {
    Component flySpeed = FlySpeed.of(player.getFlySpeed()).getName();
    Component lore =
        new PersonalizedTranslatable("setting.flyspeed.lore", flySpeed)
            .getPersonalizedText()
            .color(ChatColor.GRAY);
    return Lists.newArrayList(ComponentRenderers.toLegacyText(lore, player));
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
    NORMAL(ChatColor.YELLOW, 0.1f),
    FAST(ChatColor.GOLD, 0.25f),
    FASTER(ChatColor.RED, 0.5f),
    HYPERSPEED(ChatColor.LIGHT_PURPLE, 0.9f);

    private ChatColor color;
    private float value;

    private static FlySpeed[] speeds = values();

    FlySpeed(ChatColor color, float value) {
      this.color = color;
      this.value = value;
    }

    public float getValue() {
      return value;
    }

    public Component getName() {
      return new PersonalizedTranslatable(TRANSLATION_KEY + this.name().toLowerCase())
          .getPersonalizedText()
          .color(color);
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
