package tc.oc.pgm.observers.tools;

import com.google.common.collect.Lists;
import java.util.List;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import tc.oc.component.Component;
import tc.oc.component.render.ComponentRenderers;
import tc.oc.component.types.PersonalizedTranslatable;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.menu.InventoryMenu;
import tc.oc.pgm.menu.InventoryMenuItem;

public class FlySpeedTool implements InventoryMenuItem {

  private static String TRANSLATION_KEY = "observer.tools.flyspeed.";

  @Override
  public Component getName() {
    return new PersonalizedTranslatable("observer.tools.flyspeed");
  }

  @Override
  public ChatColor getColor() {
    return ChatColor.DARK_RED;
  }

  @Override
  public List<String> getLore(MatchPlayer player) {
    Component flySpeed = FlySpeed.of(player.getBukkit().getFlySpeed()).getName();
    Component lore =
        new PersonalizedTranslatable("observer.tools.flyspeed.lore", flySpeed)
            .getPersonalizedText()
            .color(ChatColor.GRAY);
    return Lists.newArrayList(ComponentRenderers.toLegacyText(lore, player.getBukkit()));
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
      return speeds[(Math.max(0, ordinal() - 1)) % speeds.length];
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
