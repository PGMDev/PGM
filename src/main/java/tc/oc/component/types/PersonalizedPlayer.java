package tc.oc.component.types;

import static com.google.common.base.Preconditions.checkNotNull;

import lombok.Getter;
import lombok.NonNull;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import tc.oc.component.Component;
import tc.oc.identity.Identities;
import tc.oc.identity.Identity;
import tc.oc.named.NameStyle;
import tc.oc.named.NameType;
import tc.oc.pgm.PGMUtil;

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

  private final Identity identity;
  private final NameStyle style;

  /**
   * Constructor
   *
   * @param identity to use
   * @param style {@link NameStyle} to apply to the component
   */
  public PersonalizedPlayer(@NonNull Identity identity, @NonNull NameStyle style) {
    super(new TextComponent(identity.getRealName()));
    this.identity = checkNotNull(identity);
    this.style = checkNotNull(style);
  }

  /**
   * Constructor
   *
   * @param player to get identity from
   * @param style {@link NameStyle} to apply to the component
   */
  public PersonalizedPlayer(Player player, NameStyle style) {
    this(Identities.current(player), style);
  }

  @Override
  public BaseComponent render(CommandSender viewer) {
    return PGMUtil.get()
        .getNameRenderer()
        .getComponentName(getIdentity(), new NameType(getStyle(), getIdentity(), viewer))
        .render(viewer);
  }
}
