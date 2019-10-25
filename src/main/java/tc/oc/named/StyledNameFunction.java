package tc.oc.named;

import com.google.common.base.Function;
import tc.oc.component.Component;

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
