package tc.oc.pgm.filters.query;

import static com.google.common.base.Preconditions.checkNotNull;

import javax.annotation.Nullable;
import org.bukkit.event.Event;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.api.tracker.info.DamageInfo;

public class DamageQuery extends PlayerStateQuery
    implements tc.oc.pgm.api.filter.query.DamageQuery {

  private final ParticipantState victim;
  private final DamageInfo damageInfo;

  protected DamageQuery(
      @Nullable Event event,
      ParticipantState victim,
      DamageInfo damageInfo,
      ParticipantState defaultPlayer) {
    super(event, defaultPlayer);
    this.damageInfo = checkNotNull(damageInfo);
    this.victim = checkNotNull(victim);
  }

  public static DamageQuery victimDefault(
      @Nullable Event event, ParticipantState victim, DamageInfo damageInfo) {
    return new DamageQuery(event, victim, damageInfo, victim);
  }

  public static DamageQuery attackerDefault(
      @Nullable Event event, ParticipantState victim, DamageInfo damageInfo) {
    return new DamageQuery(event, victim, damageInfo, damageInfo.getAttacker());
  }

  @Override
  public ParticipantState getVictim() {
    return victim;
  }

  @Override
  public DamageInfo getDamageInfo() {
    return damageInfo;
  }
}
