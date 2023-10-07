package tc.oc.pgm.killreward;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.api.player.event.MatchPlayerDeathEvent;
import tc.oc.pgm.api.tracker.info.DamageInfo;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.events.PlayerPartyChangeEvent;
import tc.oc.pgm.filters.query.DamageQuery;
import tc.oc.pgm.kits.tag.ItemModifier;
import tc.oc.pgm.util.collection.DefaultMapAdapter;
import tc.oc.pgm.util.event.ItemTransferEvent;
import tc.oc.pgm.util.event.PlayerItemTransferEvent;

@ListenerScope(MatchScope.RUNNING)
public class KillRewardMatchModule implements MatchModule, Listener {
  private final Match match;
  private final Map<UUID, Integer> killStreaks;
  private final ImmutableList<KillReward> killRewards;
  private final Multimap<UUID, KillReward> deadPlayerRewards;

  public KillRewardMatchModule(Match match, List<KillReward> killRewards) {
    this.match = match;
    this.killRewards = ImmutableList.copyOf(killRewards);
    this.killStreaks = new DefaultMapAdapter<>(key -> 0, true);
    this.deadPlayerRewards = ArrayListMultimap.create();
  }

  public int getKillStreak(UUID uuid) {
    return killStreaks.get(uuid);
  }

  private Collection<KillReward> getRewards(
      @Nullable Event event, ParticipantState victim, DamageInfo damageInfo) {
    final DamageQuery query = DamageQuery.attackerDefault(event, victim, damageInfo);
    return Collections2.filter(
        killRewards, killReward -> killReward.filter.query(query).isAllowed());
  }

  private Collection<KillReward> getRewards(MatchPlayerDeathEvent event) {
    return getRewards(event, event.getVictim().getParticipantState(), event.getDamageInfo());
  }

  private void giveRewards(MatchPlayer killer, Collection<KillReward> rewards) {
    for (KillReward reward : rewards) {
      // Apply action/kit first, so it can not override reward items
      reward.action.trigger(killer);

      for (ItemStack stack : reward.items) {
        ItemStack clone = stack.clone();
        ItemModifier.apply(clone, killer);
        PlayerItemTransferEvent event =
            new PlayerItemTransferEvent(
                null,
                ItemTransferEvent.Reason.PLUGIN,
                killer.getBukkit(),
                null,
                killer.getBukkit().getInventory(),
                clone,
                null,
                clone.getAmount(),
                null);
        match.callEvent(event);
        if (!event.isCancelled() && event.getQuantity() > 0) {
          // BEWARE: addItem modifies its argument.. send in the clone!
          clone.setAmount(event.getQuantity());
          killer.getBukkit().getInventory().addItem(clone);
        }
      }
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onDeath(MatchPlayerDeathEvent event) {
    final ParticipantState killer = event.getKiller();
    if (event.isChallengeKill() && killer != null) {
      killStreaks.put(killer.getId(), 1 + killStreaks.get(killer.getId()));
    }

    final MatchPlayer victim = event.getVictim();
    if (victim != null) {
      killStreaks.remove(victim.getId());
    }

    if (!event.isChallengeKill() || killer == null) return;
    Collection<KillReward> rewards = getRewards(event);

    // Always apply victim rewards
    rewards.forEach(r -> r.victimAction.trigger(victim));

    // Apply kill rewards only if killer didn't leave
    MatchPlayer onlineKiller = killer.getPlayer().orElse(null);
    if (onlineKiller == null) return;

    if (onlineKiller.isDead()) {
      // If a player earns a KW while dead, give it to them when they respawn. Rationale: If they
      // click respawn fast enough, they will get the reward anyway, and we can't prevent it in that
      // case, so we might as well just give it to them always. Also, if the KW is in itemkeep, they
      // should definitely get it while dead, and this is a relatively simple way to handle that
      // case.
      deadPlayerRewards.putAll(onlineKiller.getId(), rewards);
    } else {
      giveRewards(onlineKiller, rewards);
    }
  }

  /**
   * This is called from {@link tc.oc.pgm.spawns.SpawnMatchModule} so that rewards are given after
   * kits
   */
  public void giveDeadPlayerRewards(MatchPlayer attacker) {
    giveRewards(attacker, deadPlayerRewards.removeAll(attacker.getId()));
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPartyChange(PlayerPartyChangeEvent event) {
    killStreaks.remove(event.getPlayer().getId());
    deadPlayerRewards.removeAll(event.getPlayer().getId());
  }
}
