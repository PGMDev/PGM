package tc.oc.util.bukkit.component.types;

import java.util.Collections;
import java.util.List;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import tc.oc.util.bukkit.component.ImmutableComponent;

/**
 * A {@link tc.oc.util.bukkit.component.Component} used to render a blank {@link String} using a
 * {@link TextComponent}
 */
public class BlankComponent extends ImmutableComponent {

  public static final BlankComponent INSTANCE = new BlankComponent();

  private BlankComponent() {
    super(new TextComponent(""));
  }

  @Override
  public String toPlainText() {
    return "";
  }

  @Override
  public String toLegacyText() {
    return "";
  }

  @Override
  public List<BaseComponent> getExtra() {
    return Collections.emptyList();
  }
}
