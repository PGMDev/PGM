package tc.oc.util.bukkit.component.types;

import static com.google.common.base.Preconditions.checkNotNull;

import java.lang.ref.WeakReference;
import javax.annotation.Nullable;
import lombok.Getter;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import tc.oc.util.bukkit.component.Component;
import tc.oc.util.bukkit.named.NameStyle;
import tc.oc.util.bukkit.nms.DeathOverride;

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
public class PersonalizedPlayer extends Component {

  private final String username;
  private final WeakReference<Player> player;
  private final NameStyle style;

  /**
   * Constructor
   *
   * @param style {@link NameStyle} to apply to the component
   */
  public PersonalizedPlayer(@Nullable Player player, String username, NameStyle style) {
    super(new TextComponent(username));
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

  @Override
  public BaseComponent render(CommandSender viewer) {
    Player player = this.player.get();

    BaseComponent component;
    if (player == null || !player.isOnline()) {
      component = new TextComponent(username);
      component.setColor(ChatColor.DARK_AQUA);
      return component;
    }

    String realName = player.getName();
    String displayName = player.getDisplayName();

    if (!style.showPrefix) {
      displayName = displayName.substring(displayName.indexOf(realName) - 2);
    }

    if (style.showDeath && DeathOverride.isDead(player)) {
      displayName =
          displayName.replaceFirst(realName, ChatColor.DARK_GRAY + realName + ChatColor.RESET);
    }

    component = TextComponent.fromLegacyToComponent(displayName, false);

    if (style.teleport) {
      component.setHoverEvent(
          new HoverEvent(
              HoverEvent.Action.SHOW_TEXT,
              new BaseComponent[] {
                new PersonalizedTranslatable("tip.teleportTo", component.duplicate()).render(viewer)
              }));
      component.setClickEvent(
          new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tp " + player.getName()));
    }

    return component;
  }
}
