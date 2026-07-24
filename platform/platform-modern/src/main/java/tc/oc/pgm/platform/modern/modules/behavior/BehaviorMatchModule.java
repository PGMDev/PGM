package tc.oc.pgm.platform.modern.modules.behavior;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.pathfinder.PathType;
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
import tc.oc.pgm.platform.modern.modules.behavior.evade.EvadeBehavior;
import tc.oc.pgm.platform.modern.modules.behavior.evade.EvadeInstance;
import tc.oc.pgm.platform.modern.modules.behavior.home.HomeBehavior;
import tc.oc.pgm.platform.modern.modules.behavior.home.HomeInstance;
import tc.oc.pgm.platform.modern.modules.behavior.looking.LookBehavior;
import tc.oc.pgm.platform.modern.modules.behavior.looking.LookInstance;
import tc.oc.pgm.platform.modern.modules.behavior.pathing.PathingBehavior;
import tc.oc.pgm.platform.modern.modules.behavior.pathing.PathingInstance;
import tc.oc.pgm.platform.modern.modules.mannequin.Mannequin;

@ListenerScope(MatchScope.RUNNING)
public class BehaviorMatchModule implements MatchModule, Listener, Tickable {

  private final Match match;
  private final Map<Mannequin, CombatInstance> hostiles = new HashMap<>();
  private final Map<Mannequin, HomeInstance> homes = new HashMap<>();
  private final Map<Mannequin, EvadeInstance> evades = new HashMap<>();
  private final Map<Mannequin, LookInstance> looks = new HashMap<>();
  private final Map<Mannequin, PathingInstance> paths = new HashMap<>();

  public BehaviorMatchModule(Match match, Map<String, BehaviorDefinition> behaviorDefinitions) {
    this.match = match;
  }

  @Override
  public void load() {
    match.addTickable(this, MatchScope.RUNNING);
  }

  public void register(Mannequin mannequin, BehaviorDefinition definition) {
    var entity = mannequin.getEntity();
    CombatBehavior combat = definition.getCombatBehavior();
    HomeBehavior home = definition.getHomeBehavior();
    EvadeBehavior evade = definition.getEvadeBehavior();
    LookBehavior look = definition.getLookBehavior();
    PathingBehavior path = definition.getPathingBehavior();

    Zombie ghost = null;
    if (combat != null || home != null || evade != null || path != null) {
      ghost = new Zombie(
          EntityType.ZOMBIE, ((CraftWorld) mannequin.getEntity().getWorld()).getHandle());
      ghost.setOnGround(true);
      if (definition.isAvoidDanger()) {
        CombatInstance.DANGER_PENALTIES.forEach(ghost::setPathfindingMalus);
      }
      if (definition.isOpenDoors()) {
        ghost.setPathfindingMalus(PathType.DOOR_WOOD_CLOSED, 0.0F);
      }
    }

    if (combat != null) {
      var attr = entity.getAttribute(Attribute.ATTACK_DAMAGE);
      if (attr == null) {
        entity.registerAttribute(Attribute.ATTACK_DAMAGE);
        attr = entity.getAttribute(Attribute.ATTACK_DAMAGE);
        attr.setBaseValue(2.0); // Fix later once mob-kits is done
      }

      boolean hasPathing = path != null;
      Float leash = hasPathing ? path.leash() : null;
      hostiles.put(
          mannequin,
          new CombatInstance(
              mannequin, combat, home, definition.isOpenDoors(), ghost, hasPathing, leash));
    }

    if (home != null) {
      homes.put(mannequin, new HomeInstance(mannequin, home, ghost));
    }
    if (evade != null) {
      evades.put(mannequin, new EvadeInstance(mannequin, evade, ghost));
    }
    if (look != null) {
      looks.put(mannequin, new LookInstance(mannequin, look));
    }
    if (path != null) {
      paths.put(mannequin, new PathingInstance(mannequin, path, ghost, definition.isOpenDoors()));
    }
  }

  public void unregister(Mannequin mannequin) {
    hostiles.remove(mannequin);
    homes.remove(mannequin);
    evades.remove(mannequin);
    looks.remove(mannequin);
    paths.remove(mannequin);
  }

  @EventHandler
  public void onDamage(EntityDamageByEntityEvent event) {
    if (!(event.getDamager() instanceof Player player)) return;
    MatchPlayer attacker = match.getPlayer(player);
    if (attacker == null) return;
    Tick now = match.getTick();

    for (Map.Entry<Mannequin, CombatInstance> ci : hostiles.entrySet()) {
      if (ci.getValue().matches(event.getEntity())) {
        ci.getValue().onAttacked(attacker, now);
        HomeInstance hi = homes.get(ci.getKey());
        if (hi != null) hi.clearPaths();
        break;
      }
    }

    for (Map.Entry<Mannequin, EvadeInstance> ei : evades.entrySet()) {
      if (ei.getValue().matches(event.getEntity())) {
        ei.getValue().startPanic(attacker, now);
        HomeInstance hi = homes.get(ei.getKey());
        if (hi != null) {
          hi.clearPaths();
        }
        break;
      }
    }
  }

  @EventHandler
  public void onEnvironmentalDamage(EntityDamageEvent event) {
    if (event instanceof EntityDamageByEntityEvent) return;
    if (!EvadeInstance.isPanicCause(event.getCause())) return;

    Tick now = match.getTick();
    for (Map.Entry<Mannequin, EvadeInstance> ei : evades.entrySet()) {
      if (ei.getValue().matches(event.getEntity())) {
        ei.getValue().startPanic(null, now);
        return;
      }
    }
  }

  private boolean inCombat(Mannequin mannequin) {
    CombatInstance ci = hostiles.get(mannequin);
    return ci != null && ci.hasTarget();
  }

  private boolean isEvading(Mannequin mannequin, Tick tick) {
    EvadeInstance ei = evades.get(mannequin);
    return ei != null && ei.isEvading(tick);
  }

  private boolean canMove(Mannequin mannequin) {
    return mannequin.getDefinition().hasGravity()
        && mannequin.getDefinition().hasPhysics()
        && !mannequin.getDefinition().isImmovable();
  }

  @Override
  public void tick(Match match, Tick tick) {
    hostiles.forEach((mannequin, ci) -> ci.tick(match, tick, canMove(mannequin)));

    homes.forEach((mannequin, hi) -> {
      if (canMove(mannequin) && !inCombat(mannequin) && !isEvading(mannequin, tick)) {
        hi.tick(match, tick);
      } else {
        hi.clearPaths();
      }
    });

    evades.forEach((mannequin, ei) -> {
      if (canMove(mannequin)) {
        ei.tick(match, tick);
      }
    });

    looks.forEach(((mannequin, li) -> {
      boolean blocked = inCombat(mannequin) || isEvading(mannequin, tick);
      PathingInstance pi = paths.get(mannequin);
      boolean pathing = pi != null && pi.isWalking();
      if (!blocked && !pathing) {
        li.tick(match);
      }
    }));

    paths.forEach((mannequin, pi) -> {
      boolean blocked = inCombat(mannequin) || isEvading(mannequin, tick);
      if (canMove(mannequin) && !blocked) {
        pi.tick(match, tick);
      } else {
        pi.pathingInterrupted();
      }
    });
  }

  public void modifyBehavior(Mannequin mannequin, BehaviorDefinition newDef) {
    unregister(mannequin);
    register(mannequin, newDef);
  }
}
