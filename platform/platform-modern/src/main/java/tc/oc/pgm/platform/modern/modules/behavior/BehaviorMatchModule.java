package tc.oc.pgm.platform.modern.modules.behavior;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.behavior.Behavior;
import org.bukkit.attribute.Attribute;
import org.bukkit.craftbukkit.CraftWorld;
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
    var entity = mannequin.getEntity();
    var attr = entity.getAttribute(Attribute.ATTACK_DAMAGE);
    if (attr == null) {
      entity.registerAttribute(Attribute.ATTACK_DAMAGE);
      attr = entity.getAttribute(Attribute.ATTACK_DAMAGE);
      attr.setBaseValue(2.0); // default only when we created the attribute
    }
    entity.setAI(true);

    net.minecraft.world.entity.monster.zombie.Zombie ghost =
        new net.minecraft.world.entity.monster.zombie.Zombie(
            EntityType.ZOMBIE, ((CraftWorld) match.getWorld()).getHandle());
    ghost.setPos(
        entity.getLocation().getX(),
        entity.getLocation().getY(),
        entity.getLocation().getZ());
    ghost.setOnGround(true);
    var loc = entity.getLocation();
    var path = ghost
        .getNavigation()
        .createPath(new BlockPos((int) loc.getX() + 3, (int) loc.getY(), (int) loc.getZ()), 0);
    org.bukkit.Bukkit.getLogger()
        .info("[spike] path = " + (path == null ? "null" : path.getNodeCount() + " nodes"));

    hostiles.put(mannequin, new CombatInstance(mannequin, combat));
  }

  public void unregister(Mannequin mannequin) {
    hostiles.remove(mannequin);
  }

  // NEUTRAL
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
