package tc.oc.pgm.match;

import javax.annotation.Nullable;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import org.bukkit.Color;
import org.bukkit.command.CommandSender;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.util.bukkit.BukkitUtils;
import tc.oc.pgm.util.text.TextParser;

public abstract class ObservingParty extends SimpleParty {

  private String coloredName;
  private Component componentName;
  private Component chatPrefix;

  public ObservingParty(Match match) {
    super(match);
  }

  @Override
  public String getName() {
    return getDefaultName();
  }

  @Override
  public String getName(@Nullable CommandSender viewer) {
    return getName();
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
  public String getColoredName() {
    if (coloredName == null) {
      coloredName = getColor() + getName();
    }
    return coloredName;
  }

  @Override
  public String getColoredName(@Nullable CommandSender viewer) {
    return getColoredName();
  }

  @Override
  public Component getComponentName() {
    if (componentName == null) {
      componentName = TextComponent.of(getName(), TextParser.parseTextColor(getColor()));
    }
    return componentName;
  }

  @Override
  public Component getChatPrefix() {
    if (chatPrefix == null) {
      chatPrefix = TextComponent.of("(Obs) ", TextParser.parseTextColor(getColor()));
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
