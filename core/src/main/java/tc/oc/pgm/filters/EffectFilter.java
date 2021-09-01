package tc.oc.pgm.filters;

import com.google.common.base.Preconditions;
import com.google.common.collect.Range;
import java.util.Collection;
import org.bukkit.potion.PotionEffect;
import tc.oc.pgm.api.filter.query.PlayerQuery;
import tc.oc.pgm.api.player.MatchPlayer;

public class EffectFilter extends ParticipantFilter {
  protected final PotionEffect base;
  // duration is stored in ticks
  protected final Range<Integer> duration;
  protected final boolean amplifier;

  public EffectFilter(PotionEffect base, Range<Integer> duration, boolean amplifier) {
    this.base = Preconditions.checkNotNull(base);
    this.duration = duration;
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
          && duration.contains(effect.getDuration())) {
        return QueryResponse.ALLOW;
      }
    }

    return QueryResponse.DENY;
  }
}
