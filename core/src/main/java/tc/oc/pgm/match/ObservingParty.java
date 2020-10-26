package tc.oc.pgm.match;

import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.util.bukkit.BukkitUtils;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.text.TextFormatter;

public abstract class ObservingParty extends SimpleParty {

  private Component componentName;
  private Component chatPrefix;

  public ObservingParty(Match match) {
    super(match);
  }

  @Override
  public boolean isNamePlural() {
    return true;
  }

  @Override
  public Color getFullColor() {
    return BukkitUtils.colorOf(this.getColor());
  }

  @Override
  public Component getName(NameStyle style) {
    if (componentName == null) {
      componentName = Component.text(getDefaultName(), TextFormatter.convert(getColor()));
    }
    return componentName;
  }

  @Override
  public String getNameLegacy() {
    return getDefaultName();
  }

  @Override
  public Component getChatPrefix() {
    if (chatPrefix == null) {
      chatPrefix = Component.text("(Obs) ", TextFormatter.convert(getColor()));
    }
    return chatPrefix;
  }

  @Override
  public boolean isAutomatic() {
    return false;
  }

  @Override
  public boolean isParticipating() {
    return false;
  }

  @Override
  public boolean isObserving() {
    return true;
  }
}
