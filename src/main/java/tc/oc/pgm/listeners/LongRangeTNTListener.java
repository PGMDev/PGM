package tc.oc.pgm.listeners;

import java.util.*;
import org.bukkit.Effect;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDespawnInVoidEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scoreboard.NameTagVisibility;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.tracker.Trackers;
import tc.oc.util.ViaUtils;
import tc.oc.world.NMSHacks;

public class LongRangeTNTListener implements Listener {

  private static final double MIN_DISTANCE_SQ = 56 * 56; // Minimum square distance to show fake TNT
  private static final double HEAD_HEIGHT = 1.7; // Offset of real TNT relative to fake TNT
  private static final int MAX_FAKE_TNT = 16; // Max simultaneous fake TNT per player

  private class Slot {
    final UUID uuid;
    final String name;

    private Slot() {
      this.uuid = UUID.randomUUID();
      String name = this.uuid.toString();
      this.name = name.substring(name.length() - 16);
    }
  }

  private class View {
    final Player player;
    final Map<Slot, TNT> slotsOwned = new HashMap<>();
    final Map<Slot, TNT> slotsUnowned = new HashMap<>();
    final Set<Slot> slotsFree = new HashSet<>();

    private View(Player player) {
      this.player = player;
      String teamName = "FakeTNT" + player.getWorld().getUID().toString().substring(0, 9);
      this.send(
          NMSHacks.teamCreatePacket(
              teamName,
              teamName,
              "",
              "",
              false,
              false,
              NameTagVisibility.NEVER,
              Collections.<String>emptySet()));

      for (Slot slot : slots) {
        NMSHacks.sendPacket(
            this.player, NMSHacks.teamJoinPacket(teamName, Collections.singleton(slot.name)));
        NMSHacks.sendPacket(
            this.player,
            NMSHacks.playerListAddPacket(
                slot.uuid, slot.name, null, GameMode.SPECTATOR, 9999, null));
        this.slotsFree.add(slot);
      }
    }

    private void send(Object packet) {
      NMSHacks.sendPacket(this.player, packet);
    }

    public void forgetAll() {
      for (TNT tnt : this.slotsUnowned.values()) {
        tnt.views.remove(this);
      }
      this.slotsUnowned.clear();

      for (TNT tnt : this.slotsOwned.values()) {
        tnt.views.remove(this);
      }
      this.slotsOwned.clear();

      this.slotsFree.addAll(Arrays.asList(slots));
    }

    public void sendCreate(Slot slot, TNT tnt) {
      this.send(
          NMSHacks.spawnPlayerPacket(
              tnt.entityId,
              slot.uuid,
              tnt.entity.getLocation().subtract(0, HEAD_HEIGHT, 0),
              new ItemStack(Material.AIR),
              tnt.metadata));
      this.send(tnt.equipmentPacket);
    }

    public void sendDestroy(TNT tnt) {
      this.send(tnt.destroyPacket);
    }

    public void sendUpdate(TNT tnt) {
      this.send(tnt.movePacket);
    }

    public void sendExplosion(TNT tnt) {
      this.player.playEffect(tnt.location, Effect.EXPLOSION_LARGE, null);
    }

    public boolean show(TNT tnt) {
      ParticipantState owner = Trackers.getOwnerSafely(tnt.entity);
      boolean owned = owner != null && owner.getId().equals(this.player.getUniqueId());
      Slot slot = null;

      if (!this.slotsFree.isEmpty()) {
        slot = this.slotsFree.iterator().next();
        this.slotsFree.remove(slot);
      } else if (owned && !this.slotsUnowned.isEmpty()) {
        slot = this.slotsUnowned.keySet().iterator().next();
        this.sendDestroy(this.slotsUnowned.remove(slot));
      }

      if (slot != null) {
        (owned ? this.slotsOwned : this.slotsUnowned).put(slot, tnt);
        tnt.views.put(this, slot);
        this.sendCreate(slot, tnt);
        return true;
      } else {
        return false;
      }
    }

    public void hide(TNT tnt, boolean explode) {
      this.sendDestroy(tnt);
      if (explode) this.sendExplosion(tnt);
      Slot slot = tnt.views.remove(this);
      this.slotsUnowned.remove(slot);
      this.slotsOwned.remove(slot);
      this.slotsFree.add(slot);
    }
  }

  private class TNT {
    final Map<View, Slot> views = new HashMap<>();

    final TNTPrimed entity;
    final int entityId;
    final NMSHacks.EntityMetadata metadata;
    final Object equipmentPacket;
    final Object destroyPacket;

    Location location;
    Object movePacket;

    TNT(TNTPrimed entity) {
      this.entity = entity;
      this.location = entity.getLocation();

      this.entityId = NMSHacks.allocateEntityId();
      this.metadata = NMSHacks.createEntityMetadata();
      NMSHacks.setEntityMetadata(this.metadata, false, false, false, false, true, (short) 300);
      this.equipmentPacket =
          NMSHacks.entityEquipmentPacket(this.entityId, 4, new ItemStack(Material.TNT));
      this.destroyPacket = NMSHacks.destroyEntitiesPacket(this.entityId);
    }

    void hideAll(boolean explode) {
      while (!this.views.isEmpty()) {
        this.views.keySet().iterator().next().hide(this, explode);
      }
    }

    void updateMovePacket() {
      // Client interpolation causes the fake TNT to lag behind the real TNT by a bit.
      // Adding about 4 velocities to the position seems to compensate for this almost perfectly.
      this.location = this.entity.getLocation().add(this.entity.getVelocity().multiply(4));
      this.movePacket =
          NMSHacks.teleportEntityPacket(
              this.entityId, this.location.clone().subtract(0, HEAD_HEIGHT, 0));
    }

    boolean tick() {
      if (this.entity.isDead() || PGM.get().getMatchManager().getMatch(this.entity) == null) {
        this.hideAll(false);
        return true;
      }

      this.updateMovePacket();

      for (Player player : this.entity.getWorld().getPlayers()) {
        View view = viewsByPlayer.get(player);
        if (view != null) {
          double distance = player.getLocation().distanceSquared(this.location);
          if (this.views.containsKey(view)) {
            if (distance < MIN_DISTANCE_SQ) {
              view.hide(this, false);
            } else {
              view.sendUpdate(this);
            }
          } else if (distance >= MIN_DISTANCE_SQ) {
            view.show(this);
          }
        }
      }

      return false;
    }
  }

  private final Plugin plugin;

  private final Slot[] slots = new Slot[MAX_FAKE_TNT];

  private final Map<Player, View> viewsByPlayer = new HashMap<>();
  private final Map<TNTPrimed, TNT> tntByEntity = new HashMap<>();

  public LongRangeTNTListener(Plugin plugin) {
    this.plugin = plugin;

    for (int i = 0; i < slots.length; i++) {
      this.slots[i] = new Slot();
    }

    this.plugin
        .getServer()
        .getScheduler()
        .runTaskTimer(
            this.plugin,
            new Runnable() {
              @Override
              public void run() {
                tick();
              }
            },
            0,
            1);
  }

  public void tick() {
    Iterator<Map.Entry<TNTPrimed, TNT>> iter = this.tntByEntity.entrySet().iterator();
    while (iter.hasNext()) {
      Map.Entry<TNTPrimed, TNT> tntEntry = iter.next();
      if (tntEntry.getValue().tick()) {
        iter.remove();
      }
    }
  }

  private void addTNT(Entity entity) {
    if (entity instanceof TNTPrimed) {
      this.tntByEntity.put((TNTPrimed) entity, new TNT((TNTPrimed) entity));
    }
  }

  private void removeTNT(Entity entity, boolean explode) {
    if (entity instanceof TNTPrimed) {
      TNT tnt = this.tntByEntity.remove(entity);
      if (tnt != null) {
        tnt.hideAll(explode);
      }
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerJoin(final PlayerJoinEvent event) {
    if (ViaUtils.getProtocolVersion(event.getPlayer()) >= 47) {
      this.viewsByPlayer.put(event.getPlayer(), new View(event.getPlayer()));
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerQuit(PlayerQuitEvent event) {
    View view = this.viewsByPlayer.remove(event.getPlayer());
    if (view != null) view.forgetAll();
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerRespawn(PlayerRespawnEvent event) {
    View view = this.viewsByPlayer.get(event.getPlayer());
    if (view != null) view.forgetAll();
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onTNTPrime(ExplosionPrimeEvent event) {
    this.addTNT(event.getEntity());
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onTNTExplode(EntityExplodeEvent event) {
    this.removeTNT(event.getEntity(), true);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onTNTDespawn(EntityDespawnInVoidEvent event) {
    this.removeTNT(event.getEntity(), false);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onWorldUnload(WorldUnloadEvent event) {
    for (TNT tnt : this.tntByEntity.values()) {
      tnt.hideAll(false);
    }
    this.tntByEntity.clear();
  }
}
