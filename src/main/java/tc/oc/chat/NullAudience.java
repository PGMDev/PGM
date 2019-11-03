package tc.oc.chat;

import java.util.Collections;
import tc.oc.pgm.api.chat.Audience;
import tc.oc.pgm.api.chat.MultiAudience;

public class NullAudience implements MultiAudience {

  public static final NullAudience INSTANCE = new NullAudience();

  @Override
  public Iterable<? extends Audience> getAudiences() {
    return Collections.emptyList();
  }
}
