package tc.oc.util.bukkit.named;

import com.google.common.base.Function;
import tc.oc.util.bukkit.component.Component;

public class StyledNameFunction implements Function<Named, Component> {
  private final NameStyle style;

  public StyledNameFunction(NameStyle style) {
    this.style = style;
  }

  @Override
  public Component apply(Named named) {
    return named.getStyledName(style);
  }
}
