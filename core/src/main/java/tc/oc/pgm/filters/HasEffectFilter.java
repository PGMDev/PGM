package tc.oc.pgm.filters;

import com.google.common.base.Preconditions;
import java.util.Collection;
import org.bukkit.potion.PotionEffect;
import tc.oc.pgm.api.filter.query.PlayerQuery;
import tc.oc.pgm.api.player.MatchPlayer;

public class HasEffectFilter extends ParticipantFilter {
  protected final PotionEffect base;

  public HasEffectFilter(PotionEffect base) {
    this.base = Preconditions.checkNotNull(base);
  }

  protected Collection<PotionEffect> getEffects(MatchPlayer player) {
    return player.getBukkit().getActivePotionEffects();
  }

  @Override
  protected QueryResponse queryPlayer(PlayerQuery query, MatchPlayer player) {
    for (PotionEffect effect : getEffects(player)) {
      if (effect == null) continue;

      // Match if the effects are the same type and amplifier, and if the player has at least the
      // desired duration left.
      if (effect.getType().equals(base.getType())
          && (effect.getAmplifier() == base.getAmplifier())
          && (effect.getDuration() >= base.getDuration())) {
        return QueryResponse.ALLOW;
      }
    }

    return QueryResponse.DENY;
  }
}
