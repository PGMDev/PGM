package tc.oc.pgm.entity;

import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.SetMultimap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;

public class TaggedMobMatchModule implements MatchModule, Listener {

  private final Match match;
  private final SetMultimap<String, LivingEntity> instances = HashMultimap.create();
  private final Map<UUID, String> byEntity = new HashMap<>();

  public TaggedMobMatchModule(Match match) {
    this.match = match;
  }

  public void track(String id, LivingEntity le) {
    instances.put(id, le);
    byEntity.put(le.getUniqueId(), id);
  }

  public void untrack(LivingEntity le) {
    String id = byEntity.remove(le.getUniqueId());

    if (id != null) {
      instances.remove(id, le);
    }
  }

  public Set<LivingEntity> get(String id) {
    return instances.get(id);
  }

  public @Nullable String getId(LivingEntity le) {
    return byEntity.get(le.getUniqueId());
  }

  public void forEach(String id, Consumer<LivingEntity> consumer) {
    for (LivingEntity le : ImmutableSet.copyOf(instances.get(id))) {
      consumer.accept(le);
    }
  }

  @EventHandler
  public void onDeath(EntityDeathEvent event) {
    untrack(event.getEntity());
  }

  @EventHandler
  public void onRemoval(EntityRemoveFromWorldEvent event) {
    if (event.getEntity() instanceof LivingEntity le) {
      untrack(le);
    }
  }
}
