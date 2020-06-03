package tc.oc.pgm.match;

import org.bukkit.ChatColor;
import tc.oc.pgm.api.match.Match;

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
  public String toString() {
    return getClass().getSimpleName() + "{match=" + getMatch() + "}";
  }
}
