package tc.oc.pgm.match;

import net.kyori.text.Component;
import org.bukkit.ChatColor;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.util.named.NameStyle;

public class Observers extends ObservingParty {

  public Observers(Match match) {
    super(match);
  }

  @Override
  public String getDefaultName() {
    return "Observers";
  }

  @Override
  public ChatColor getColor() {
    return ChatColor.AQUA;
  }

  @Override
  public Component getName(NameStyle style) {
    return getName();
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{match=" + getMatch() + "}";
  }
}
