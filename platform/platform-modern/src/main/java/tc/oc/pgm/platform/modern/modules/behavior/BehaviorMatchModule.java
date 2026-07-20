package tc.oc.pgm.platform.modern.modules.behavior;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.zombie.Zombie;
import org.bukkit.attribute.Attribute;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
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
import tc.oc.pgm.platform.modern.modules.behavior.combat.PanicInstance;
import tc.oc.pgm.platform.modern.modules.behavior.looking.LookBehavior;
import tc.oc.pgm.platform.modern.modules.behavior.looking.LookInstance;
import tc.oc.pgm.platform.modern.modules.behavior.pathing.PathingBehavior;
import tc.oc.pgm.platform.modern.modules.behavior.pathing.PathingInstance;
import tc.oc.pgm.platform.modern.modules.mannequin.Mannequin;

@ListenerScope(MatchScope.RUNNING)
public class BehaviorMatchModule implements MatchModule, Listener, Tickable {

  private final Match match;
  private final Map<String, BehaviorDefinition> behaviorDefinitions;
  private final Map<Mannequin, CombatInstance> hostiles = new HashMap<>();
  private final Map<Mannequin, LookInstance> looks = new HashMap<>();
  private final Map<Mannequin, PathingInstance> paths = new HashMap<>();

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
    PathingBehavior path = definition.getPathingBehavior();

    if (combat != null) {
      var attr = entity.getAttribute(Attribute.ATTACK_DAMAGE);
      if (attr == null) {
        entity.registerAttribute(Attribute.ATTACK_DAMAGE);
        attr = entity.getAttribute(Attribute.ATTACK_DAMAGE);
        attr.setBaseValue(2.0); // Fix later once mob-kits is done
      }

      boolean hasPathing = path != null;
      Float leash = hasPathing ? path.getLeash() : null;
      hostiles.put(
          mannequin,
          new CombatInstance(mannequin, combat, definition.isAvoidDanger(), hasPathing, leash));
    }

    LookBehavior look = definition.getLookBehavior();
    if (look != null) {
      looks.put(mannequin, new LookInstance(mannequin, look));
    }

    if (path != null) {
      Zombie pathGhost = new Zombie(
          EntityType.ZOMBIE, ((CraftWorld) mannequin.getEntity().getWorld()).getHandle());
      pathGhost.setOnGround(true);
      paths.put(mannequin, new PathingInstance(mannequin, path, pathGhost));
    }
  }

  public void unregister(Mannequin mannequin) {
    hostiles.remove(mannequin);
    looks.remove(mannequin);
    paths.remove(mannequin);
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

  @EventHandler
  public void onEnvironmentalDamage(EntityDamageEvent event) {
    if (event instanceof EntityDamageByEntityEvent) return;
    if (!PanicInstance.isPanicCause(event.getCause())) return;

    Tick now = match.getTick();
    for (CombatInstance ci : hostiles.values()) {
      if (ci.matches(event.getEntity())) {
        ci.onEnvironmentalDamage(now);
        return;
      }
    }
  }

  @Override
  public void tick(Match match, Tick tick) {
    hostiles.values().forEach(ci -> ci.tick(match, tick));
    looks.forEach(((mannequin, li) -> {
      CombatInstance ci = hostiles.get(mannequin);
      if (ci == null || (!ci.hasTarget() && !ci.isPanicking(tick))) {
        li.tick(match);
      }
    }));

    paths.forEach((mannequin, pi) -> {
      CombatInstance ci = hostiles.get(mannequin);
      boolean combatActive = ci != null && (ci.hasTarget() || ci.isPanicking(tick));
      if (!combatActive) {
        pi.tick(match, tick);
      } else {
        pi.pathingInterrupted();
      }
    });
  }
}
