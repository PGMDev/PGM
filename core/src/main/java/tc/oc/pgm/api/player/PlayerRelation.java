package tc.oc.pgm.api.player;

import static tc.oc.pgm.util.Assert.assertNotNull;

import org.jetbrains.annotations.Nullable;

/**
 * Represents the competitive relationship between two {@link ParticipantState}s. The attacker can
 * be null to indicate a neutral relationship i.e. "world" damage.
 *
 * <p>{@link PlayerRelation#SELF} and {@link PlayerRelation#ALLY} are mutually exclusive, a player
 * is not their own ally, and a player can be their own {@link PlayerRelation#ENEMY} if the two
 * states have different parties
 */
public enum PlayerRelation {
  NEUTRAL, // attacker is null (e.g. world damage) or not participating
  SELF, // same player, same team
  ALLY, // different players, same team
  ENEMY; // different teams (same/different player doesn't matter)

  public static PlayerRelation get(ParticipantState victim, @Nullable MatchPlayerState attacker) {
    assertNotNull(victim);

    if (attacker == null || !attacker.getParty().isParticipating()) {
      return NEUTRAL;
    } else if (!victim.getParty().equals(attacker.getParty())) {
      return ENEMY;
    } else if (victim.equals(attacker)) {
      return SELF;
    } else {
      return ALLY;
    }
  }

  public static PlayerRelation get(ParticipantState victim, @Nullable MatchPlayer attacker) {
    return get(victim, attacker == null ? null : attacker.getState());
  }

  public boolean are(ParticipantState victim, @Nullable MatchPlayerState attacker) {
    return this == get(victim, attacker);
  }

  public boolean are(ParticipantState victim, @Nullable MatchPlayer attacker) {
    return are(victim, attacker == null ? null : attacker.getState());
  }
}
