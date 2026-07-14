package tc.oc.pgm.platform.modern.modules.behavior;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import net.minecraft.world.entity.ai.behavior.Behavior;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.match.Tickable;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.time.Tick;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.platform.modern.modules.behavior.combat.CombatBehavior;
import tc.oc.pgm.platform.modern.modules.behavior.combat.CombatInstance;
import tc.oc.pgm.platform.modern.modules.mannequin.Mannequin;

import java.util.HashMap;
import java.util.Map;

@ListenerScope(MatchScope.RUNNING)
public class BehaviorMatchModule implements MatchModule, Listener, Tickable {

  private final Match match;
  private final Map<String, BehaviorDefinition> behaviorDefinitions;
  private final SetMultimap<String, Behavior> instances = HashMultimap.create();
  private final Map<Mannequin, CombatInstance> hostiles = new HashMap<>();

  public BehaviorMatchModule(Match match, Map<String, BehaviorDefinition> behaviorDefinitions) {
    this.match = match;
    this.behaviorDefinitions = behaviorDefinitions;
  }

  @Override
  public void load() {
    match.addTickable(this, MatchScope.RUNNING);
  }

  public void register(Mannequin mannequin, CombatBehavior combat) {
    hostiles.put(mannequin, new CombatInstance(mannequin, combat));
  }

  public void unregister(Mannequin mannequin) {
    hostiles.remove(mannequin);
  }

  //NEUTRAL
  @EventHandler
  public void onDamage(EntityDamageByEntityEvent event) {
    if (!(event.getDamager() instanceof Player player)) return;
    MatchPlayer attacker = match.getPlayer(player);
    if (attacker == null) return;
    Tick now = match.getTick();

    for (CombatInstance ci : hostiles.values()) {
      if (ci.matches(event.getEntity())) {
        ci.onAttacked(attacker, now);
        return;
      }
    }
  }

  @Override
  public void tick(Match match, Tick tick) {
    hostiles.values().forEach(ci -> ci.tick(match, tick));
  }
}
