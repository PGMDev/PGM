package tc.oc.pgm.action.actions;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.util.Audience;

public class MessageAction extends AbstractAction<Audience> {

  private final Component text;
  private final Component actionbar;
  private final Title title;

  public MessageAction(
      @Nullable Component text, @Nullable Component actionbar, @Nullable Title title) {
    super(Audience.class);
    this.text = text;
    this.actionbar = actionbar;
    this.title = title;
  }

  @Override
  public void trigger(Audience audience) {
    if (text != null) audience.sendMessage(text);
    if (title != null) audience.showTitle(title);
    if (actionbar != null) audience.sendActionBar(actionbar);
  }
}
