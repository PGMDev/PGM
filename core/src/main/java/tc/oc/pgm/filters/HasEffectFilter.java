package tc.oc.pgm.filters;

import com.google.common.base.Preconditions;
import java.util.Collection;
import org.bukkit.potion.PotionEffect;
import tc.oc.pgm.api.filter.query.PlayerQuery;
import tc.oc.pgm.api.player.MatchPlayer;

public class HasEffectFilter extends ParticipantFilter {
  protected final PotionEffect base;
  // min/max duration are stored in ticks
  protected final long minDuration;
  protected final long maxDuration;
  protected final boolean amplifier;

  public HasEffectFilter(PotionEffect base, long minDuration, long maxDuration, boolean amplifier) {
    this.base = Preconditions.checkNotNull(base);
    this.minDuration = minDuration;
    this.maxDuration = maxDuration;
    this.amplifier = amplifier;
  }

  protected Collection<PotionEffect> getEffects(MatchPlayer player) {
    return player.getBukkit().getActivePotionEffects();
  }

  @Override
  protected QueryResponse queryPlayer(PlayerQuery query, MatchPlayer player) {
    for (PotionEffect effect : getEffects(player)) {
      if (effect == null) continue;

      if (effect.getType().equals(base.getType())
          && (!amplifier || (effect.getAmplifier() == base.getAmplifier()))
          && ((minDuration == -1) || (effect.getDuration() >= minDuration))
          && ((maxDuration == -1) || (effect.getDuration() <= maxDuration))) {
        return QueryResponse.ALLOW;
      }
    }

    return QueryResponse.DENY;
  }
}
