package tc.oc.pgm.damagehistory;

import static tc.oc.pgm.util.nms.PlayerUtils.PLAYER_UTILS;

import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.api.tracker.info.DamageInfo;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.kits.ApplyKitEvent;
import tc.oc.pgm.kits.HealthKit;
import tc.oc.pgm.spawns.events.ParticipantDespawnEvent;
import tc.oc.pgm.tracker.TrackerMatchModule;
import tc.oc.pgm.util.event.entity.PotionEffectAddEvent;

@ListenerScope(MatchScope.RUNNING)
public class DamageHistoryMatchModule implements MatchModule, Listener {

  private final Match match;
  private final DamageHistory damageHistory;

  public DamageHistoryMatchModule(Match match) {
    this.match = match;
    this.damageHistory = new DamageHistory();
  }

  TrackerMatchModule tracker() {
    return match.needModule(TrackerMatchModule.class);
  }

  public Deque<DamageEntry> getDamageHistory(MatchPlayer player) {
    return this.damageHistory.getPlayerHistory(player.getId());
  }

  public @Nullable ParticipantState getAssister(MatchPlayer player) {
    Deque<DamageEntry> history = getDamageHistory(player);
    if (history == null || history.size() <= 1) return null;

    ParticipantState killer = history.getLast().getDamager();
    if (killer == null) return null;

    double total = history.stream().mapToDouble(DamageEntry::getDamage).sum();

    var highest = history.reversed().stream()
        .filter(dmg -> dmg.canAssist(player, killer))
        .collect(Collectors.groupingBy(
            DamageHistoryKey::from,
            LinkedHashMap::new,
            Collectors.mapping(DamageEntry::getDamage, Collectors.reducing(0d, Double::sum))))
        .entrySet()
        .stream()
        .max(Map.Entry.comparingByValue())
        .orElse(null);

    if (highest == null
        || highest.getValue() < (total * PGM.get().getConfiguration().getAssistPercent())
        || highest.getKey().getParty().equals(player.getParty())) return null;

    return highest.getKey().getState();
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onDamage(EntityDamageEvent event) {
    MatchPlayer victim = getVictim(event.getEntity());
    if (victim == null) return;

    DamageInfo damageInfo = tracker().resolveDamage(event);
    ParticipantState attacker = damageInfo.getAttacker() != null ? damageInfo.getAttacker() : null;

    damageHistory.addDamage(victim, getDamageAmount(event), attacker);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerDespawn(final ParticipantDespawnEvent event) {
    getDamageHistory(event.getPlayer()).clear();
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onDamageMonitor(final EntityRegainHealthEvent event) {
    MatchPlayer victim = getVictim(event.getEntity());
    if (victim == null) return;

    double maxHealing = victim.getBukkit().getMaxHealth() - victim.getBukkit().getHealth();
    damageHistory.removeDamage(victim, Math.min(maxHealing, event.getAmount()));
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPotionEffectAdd(final PotionEffectAddEvent event) {
    if (!event.getEffect().getType().equals(PotionEffectType.ABSORPTION)) return;

    MatchPlayer victim = getVictim(event.getEntity());
    if (victim == null) return;

    double currentHearts = PLAYER_UTILS.getAbsorption(event.getEntity());
    double newHearts = (event.getEffect().getAmplifier() + 1) * 4;

    damageHistory.removeDamage(victim, Math.max(0, newHearts - currentHearts));
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onHealthChangeKit(ApplyKitEvent event) {
    if (!(event.getKit() instanceof HealthKit)) return;

    HealthKit healthKit = (HealthKit) event.getKit();
    Player bukkitPlayer = event.getPlayer().getBukkit();

    double newHealth = Math.min(healthKit.getHalfHearts(), bukkitPlayer.getMaxHealth());
    double currentHealth = bukkitPlayer.getHealth();
    double healthChange = newHealth - currentHealth;

    // Record damage or heal based on affect of kit
    if (event.isForce() || currentHealth < newHealth) {
      if (healthChange == 0) return;
      if (healthChange >= 0) {
        damageHistory.removeDamage(event.getPlayer(), healthChange);
      } else {
        damageHistory.addDamage(event.getPlayer(), -healthChange, null);
      }
    }
  }

  @Nullable
  MatchPlayer getVictim(Entity entity) {
    if (entity == null) return null;
    return match.getParticipant(entity);
  }

  private double getDamageAmount(EntityDamageEvent event) {
    if (!(event.getEntity() instanceof Player)) return 0;

    double absorptionHearts = -event.getDamage(EntityDamageEvent.DamageModifier.ABSORPTION);
    double realFinalDamage =
        Math.min(event.getFinalDamage(), ((Player) event.getEntity()).getHealth())
            + absorptionHearts;

    return Math.min(((Player) event.getEntity()).getMaxHealth(), realFinalDamage);
  }
}
