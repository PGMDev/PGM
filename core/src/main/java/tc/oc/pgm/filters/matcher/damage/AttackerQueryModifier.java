package tc.oc.pgm.filters.matcher.damage;

import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.filter.query.DamageQuery;
import tc.oc.pgm.api.filter.query.PlayerQuery;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.filters.modifier.QueryModifier;
import tc.oc.pgm.filters.query.PlayerStateQuery;

public class AttackerQueryModifier extends QueryModifier<DamageQuery, PlayerQuery> {

  public AttackerQueryModifier(Filter child) {
    super(child, PlayerQuery.class);
  }

  @Nullable
  @Override
  protected PlayerQuery transformQuery(DamageQuery query) {
    ParticipantState attacker = query.getDamageInfo().getAttacker();
    return attacker != null ? new PlayerStateQuery(query.getEvent(), attacker) : null;
  }

  @Override
  public Class<? extends DamageQuery> queryType() {
    return DamageQuery.class;
  }
}
