package tc.oc.pgm.observers.tools;

import com.google.common.collect.Lists;
import java.util.List;
import net.md_5.bungee.api.ChatColor;
import org.apache.commons.lang.WordUtils;
import org.bukkit.Material;
import tc.oc.component.Component;
import tc.oc.component.render.ComponentRenderers;
import tc.oc.component.types.PersonalizedText;
import tc.oc.component.types.PersonalizedTranslatable;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.gui.InventoryGUI;
import tc.oc.pgm.observers.ObserverTool;

public class FlySpeedTool implements ObserverTool {

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
  public void onInventoryClick(InventoryGUI menu, MatchPlayer player) {
    incrementSpeed(player);
    menu.refreshWindow(player);
  }

  private void incrementSpeed(MatchPlayer player) {
    FlySpeed speed = FlySpeed.of(player.getBukkit().getFlySpeed());
    player.getBukkit().setFlySpeed(speed.getNext().getValue());
  }

  public static enum FlySpeed {
    NORMAL(ChatColor.YELLOW, 0.1f),
    FAST(ChatColor.GOLD, 0.25f),
    FASTER(ChatColor.RED, 0.5f),
    HYPERSPEED(ChatColor.LIGHT_PURPLE, 0.9f);

    private ChatColor color;
    private float value;

    FlySpeed(ChatColor color, float value) {
      this.color = color;
      this.value = value;
    }

    public float getValue() {
      return value;
    }

    public Component getName() {
      return new PersonalizedText(WordUtils.capitalize(this.name().toLowerCase())).color(color);
    }

    public FlySpeed getNext() {
      return ordinal() < (values().length - 1) ? values()[ordinal() + 1] : NORMAL;
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
