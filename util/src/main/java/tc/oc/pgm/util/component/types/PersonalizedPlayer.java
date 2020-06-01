package tc.oc.pgm.util.component.types;

import static com.google.common.base.Preconditions.checkNotNull;

import java.lang.ref.WeakReference;
import javax.annotation.Nullable;
import lombok.Getter;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.TranslatableComponent;
import net.kyori.text.event.ClickEvent;
import net.kyori.text.event.HoverEvent;
import net.kyori.text.event.HoverEvent.Action;
import net.kyori.text.format.TextColor;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.text.TextParser;
import tc.oc.pgm.util.text.TextTranslations;

/**
 * A component that renders as a player's name.
 *
 * <p>The "fancy" flag determines whether the name will include flair and other decorations.
 *
 * <p>The "big" flag shows the player's nickname after their real name when they are nicked and the
 * viewer can see through it.
 *
 * <p>A non-fancy, non-big name has color and nothing else.
 */
@Getter
public class PersonalizedPlayer {

  private final String username;
  private final WeakReference<Player> player;
  private final NameStyle style;

  /**
   * Constructor
   *
   * @param style {@link NameStyle} to apply to the component
   */
  public PersonalizedPlayer(@Nullable Player player, String username, NameStyle style) {
    this.player = new WeakReference<>(player);
    this.username = username;
    this.style = checkNotNull(style);
  }

  /**
   * Constructor
   *
   * @param player to get identity from
   * @param style {@link NameStyle} to apply to the component
   */
  public PersonalizedPlayer(Player player, NameStyle style) {
    this(player, player.getName(), style);
  }

  public Component render() {
    Player player = this.player.get();

    TextComponent.Builder component = TextComponent.builder();
    if (player == null || !player.isOnline()) {
      component.append(username, TextColor.DARK_AQUA);
      return component.build();
    }

    Component usernameComponent;
    String realName = player.getName();
    String displayName = player.getDisplayName();

    if (!style.showPrefix) {
      displayName = displayName.substring(displayName.indexOf(realName) - 2);
    }

    if (style.showDeath && isDead(player)) {
      displayName =
          displayName.replaceFirst(realName, ChatColor.DARK_GRAY + realName + ChatColor.RESET);
    }

    if (style.showVanish && isVanished(player)) {
      displayName =
          displayName.replaceFirst(realName, ChatColor.STRIKETHROUGH + realName + ChatColor.RESET);
    }

    usernameComponent = TextParser.parseComponent(displayName);
    component.append(usernameComponent);

    if (style.teleport) {
      component.hoverEvent(
          HoverEvent.of(
              Action.SHOW_TEXT,
              TranslatableComponent.of("misc.teleportTo", TextColor.GRAY).args(usernameComponent)));
      component.clickEvent(ClickEvent.runCommand("/tp " + player.getName()));
    }

    return component.build();
  }

  public BaseComponent render(CommandSender viewer) {
    return new net.md_5.bungee.api.chat.TextComponent(
        TextTranslations.translateLegacy(render(), viewer));
  }

  private boolean isVanished(Player player) {
    return player.hasMetadata("isVanished");
  }

  private boolean isDead(Player player) {
    return player.hasMetadata("isDead") || player.isDead();
  }
}
