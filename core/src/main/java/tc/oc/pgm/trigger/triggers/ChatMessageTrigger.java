package tc.oc.pgm.trigger.triggers;

import net.kyori.adventure.text.Component;
import tc.oc.pgm.trigger.TriggerDefinition;
import tc.oc.pgm.util.Audience;

public class ChatMessageTrigger implements TriggerDefinition<Audience> {

  private final Component text;

  public ChatMessageTrigger(Component text) {
    this.text = text;
  }

  @Override
  public Class<Audience> getTriggerType() {
    return Audience.class;
  }

  @Override
  public void trigger(Audience audience) {
    audience.sendMessage(text);
  }
}
