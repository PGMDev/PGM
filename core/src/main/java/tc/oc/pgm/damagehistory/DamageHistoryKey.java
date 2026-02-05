package tc.oc.pgm.damagehistory;

import static tc.oc.pgm.util.Assert.assertNotNull;

import java.util.Objects;
import java.util.UUID;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.player.ParticipantState;

public record DamageHistoryKey(ParticipantState state) {

  public static DamageHistoryKey from(DamageEntry damageEntry) {
    ParticipantState damager = damageEntry.getDamager();
    assertNotNull(damager);
    return new DamageHistoryKey(damager);
  }

  @Deprecated
  public ParticipantState getState() {
    return state();
  }

  public UUID getPlayer() {
    return state.getId();
  }

  public Competitor getParty() {
    return state.getParty();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    DamageHistoryKey that = (DamageHistoryKey) o;
    return Objects.equals(getPlayer(), that.getPlayer())
        && Objects.equals(getParty(), that.getParty());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getPlayer(), getParty());
  }
}
