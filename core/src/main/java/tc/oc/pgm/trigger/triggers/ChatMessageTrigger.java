package tc.oc.pgm.trigger.triggers;

import net.kyori.adventure.text.Component;
import tc.oc.pgm.util.Audience;

public class ChatMessageTrigger extends AbstractTrigger<Audience> {

  private final Component text;

  public ChatMessageTrigger(Component text) {
    super(Audience.class);
    this.text = text;
  }

  @Override
  public void trigger(Audience audience) {
    audience.sendMessage(text);
  }
}
