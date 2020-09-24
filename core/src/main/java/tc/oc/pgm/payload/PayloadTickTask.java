package tc.oc.pgm.payload;

import java.util.List;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.Tickable;
import tc.oc.pgm.api.time.Tick;

public class PayloadTickTask implements Tickable {
  private final List<Payload> payloads;

  PayloadTickTask(List<Payload> payloads) {
    this.payloads = payloads;
  }

  @Override
  public void tick(Match match, Tick tick) {
    for (Payload payload : payloads) {
      payload.tick();
    }
  }
}
