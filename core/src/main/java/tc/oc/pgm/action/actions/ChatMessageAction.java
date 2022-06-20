package tc.oc.pgm.action.actions;

import net.kyori.adventure.text.Component;
import tc.oc.pgm.util.Audience;

public class ChatMessageAction extends AbstractAction<Audience> {

  private final Component text;

  public ChatMessageAction(Component text) {
    super(Audience.class);
    this.text = text;
  }

  @Override
  public void trigger(Audience audience) {
    audience.sendMessage(text);
  }
}
