package tc.oc.pgm.platform.modern.modules.behavior;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.match.Tickable;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.time.Tick;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.platform.modern.modules.behavior.combat.CombatBehavior;
import tc.oc.pgm.platform.modern.modules.behavior.combat.CombatInstance;
import tc.oc.pgm.platform.modern.modules.behavior.looking.LookBehavior;
import tc.oc.pgm.platform.modern.modules.behavior.looking.LookInstance;
import tc.oc.pgm.platform.modern.modules.mannequin.Mannequin;

@ListenerScope(MatchScope.RUNNING)
public class BehaviorMatchModule implements MatchModule, Listener, Tickable {

  private final Match match;
  private final Map<String, BehaviorDefinition> behaviorDefinitions;
  private final Map<Mannequin, CombatInstance> hostiles = new HashMap<>();
  private final Map<Mannequin, LookInstance> looks = new HashMap<>();

  public BehaviorMatchModule(Match match, Map<String, BehaviorDefinition> behaviorDefinitions) {
    this.match = match;
    this.behaviorDefinitions = behaviorDefinitions;
  }

  @Override
  public void load() {
    match.addTickable(this, MatchScope.RUNNING);
  }

  public void register(Mannequin mannequin, BehaviorDefinition definition) {
    var entity = mannequin.getEntity();

    CombatBehavior combat = definition.getCombatBehavior();
    if (combat != null) {
      var attr = entity.getAttribute(Attribute.ATTACK_DAMAGE);
      if (attr == null) {
        entity.registerAttribute(Attribute.ATTACK_DAMAGE);
        attr = entity.getAttribute(Attribute.ATTACK_DAMAGE);
        attr.setBaseValue(2.0); // default only when we created the attribute
      }
      hostiles.put(mannequin, new CombatInstance(mannequin, combat, definition.isAvoidDanger()));
    }

    LookBehavior look = definition.getLookBehavior();
    if (look != null) {
      looks.put(mannequin, new LookInstance(mannequin, look));
    }
  }

  public void unregister(Mannequin mannequin) {
    hostiles.remove(mannequin);
    looks.remove(mannequin);
  }

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
    looks.forEach(((mannequin, li) -> {
      CombatInstance ci = hostiles.get(mannequin);
      if (ci == null || (!ci.hasTarget() && !ci.isPanicking())) {
        li.tick(match);
      }
    }));
  }
}
