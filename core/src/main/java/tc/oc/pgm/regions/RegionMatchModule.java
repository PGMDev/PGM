package tc.oc.pgm.regions;

import static tc.oc.pgm.api.map.MapProtos.REGION_PRIORITY_VERSION;
import static tc.oc.pgm.util.material.MaterialUtils.MATERIAL_UTILS;
import static tc.oc.pgm.util.nms.Packets.PLAYERS;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Hanging;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.event.BlockTransformEvent;
import tc.oc.pgm.api.filter.Filter.QueryResponse;
import tc.oc.pgm.api.filter.query.BlockQuery;
import tc.oc.pgm.api.filter.query.PlayerQuery;
import tc.oc.pgm.api.filter.query.Query;
import tc.oc.pgm.api.map.MapProtos;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.events.ParticipantBlockTransformEvent;
import tc.oc.pgm.filters.query.PlayerBlockQuery;
import tc.oc.pgm.filters.query.Queries;
import tc.oc.pgm.flag.event.FlagPickupEvent;
import tc.oc.pgm.util.MatchPlayers;
import tc.oc.pgm.util.block.BlockVectors;
import tc.oc.pgm.util.event.GeneralizedEvent;
import tc.oc.pgm.util.event.PlayerCoarseMoveEvent;
import tc.oc.pgm.util.material.MaterialData;

@ListenerScope(MatchScope.LOADED)
public class RegionMatchModule implements MatchModule, Listener {

  private final Match match;
  private final RFAContext rfaContext;
  private final boolean useRegionPriority;

  private Integer maxBuildHeight;

  public RegionMatchModule(Match match, RFAContext rfaContext, Integer maxBuildHeight) {
    this.match = match;
    this.rfaContext = rfaContext;
    this.useRegionPriority = match.getMap().getProto().isNoOlderThan(REGION_PRIORITY_VERSION);
    this.maxBuildHeight = maxBuildHeight;
  }

  public Integer getMaxBuildHeight() {
    return maxBuildHeight;
  }

  public void setMaxBuildHeight(Integer maxBuildHeight) {
    this.maxBuildHeight = maxBuildHeight;
  }

  protected void checkEnterLeave(
      Event event, MatchPlayer player, @Nullable Location from, Location to) {
    if (player == null || !player.canInteract()) return;

    tc.oc.pgm.filters.query.PlayerQuery query =
        new tc.oc.pgm.filters.query.PlayerQuery(event, player);

    if (useRegionPriority) {
      // We need to handle both scopes in the same loop, because the priority order can interleave
      // them
      for (RegionFilterApplication rfa : this.rfaContext.getAll()) {
        if ((rfa.scope == RFAScope.PLAYER_ENTER
                && (from == null || !rfa.region.contains(from))
                && rfa.region.contains(to))
            || rfa.scope == RFAScope.PLAYER_LEAVE
                && (from == null || rfa.region.contains(from))
                && !rfa.region.contains(to)) {

          if (processQuery(rfa, query)) {
            break; // Stop after the first non-abstaining filter
          }
        }
      }
    } else {
      // To preserve legacy behavior exactly, these need to be in seperate loops
      for (RegionFilterApplication rfa : this.rfaContext.get(RFAScope.PLAYER_ENTER)) {
        if ((from == null || !rfa.region.contains(from)) && rfa.region.contains(to)) {
          if (processQuery(rfa, query) && rfa.useRegionPriority) {
            break;
          }
        }
      }

      for (RegionFilterApplication rfa : this.rfaContext.get(RFAScope.PLAYER_LEAVE)) {
        if ((from == null || rfa.region.contains(from)) && !rfa.region.contains(to)) {
          if (processQuery(rfa, query) && rfa.useRegionPriority) {
            break;
          }
        }
      }
    }
  }

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  public void checkEnterLeave(final PlayerCoarseMoveEvent event) {
    this.checkEnterLeave(
        event, this.match.getPlayer(event.getPlayer()), event.getBlockFrom(), event.getBlockTo());
  }

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  public void checkFlagPickup(final FlagPickupEvent event) {
    this.checkEnterLeave(
        event, event.getCarrier(), null, event.getCarrier().getBukkit().getLocation());
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void applyEffects(final PlayerCoarseMoveEvent event) {
    MatchPlayer player = this.match.getPlayer(event.getPlayer());
    if (player == null) return;

    var from = event.getBlockFrom();
    var to = event.getBlockTo();
    Query query = new tc.oc.pgm.filters.query.PlayerQuery(event, player);

    for (RegionFilterApplication rfa : this.rfaContext.get(RFAScope.EFFECT)) {
      if (rfa.velocity == null && rfa.kit == null) continue;

      boolean enters = rfa.region.enters(from, to);
      boolean exits = rfa.region.exits(from, to);
      if (!enters && !exits) continue;

      if (!player.canInteract()
          || rfa.filter == null
          || rfa.filter.query(query) != QueryResponse.DENY) {
        // Note: works on observers
        if (enters && rfa.velocity != null) {
          event.getPlayer().setVelocity(rfa.velocity);
          PLAYERS.updateVelocity(event.getPlayer());
        }

        if (rfa.kit != null && player.canInteract()) {
          if (enters) {
            player.applyKit(rfa.kit, false);
          }

          if (exits && rfa.lendKit) {
            rfa.kit.remove(player);
          }
        }
      }
    }
  }

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  public void checkBlockTransform(final BlockTransformEvent event) {
    var pos = BlockVectors.center(event.getNewState());
    ParticipantState actor = this.getActor(event);

    BlockState againstBlock = null;
    if (event.getCause() instanceof BlockPlaceEvent) {
      againstBlock = ((BlockPlaceEvent) event.getCause()).getBlockAgainst().getState();
    } else if (event.getCause() instanceof PlayerBucketEmptyEvent) {
      againstBlock =
          ((PlayerBucketEmptyEvent) event.getCause()).getBlockClicked().getState();
    }

    BlockQuery breakQuery = Queries.block(event, actor, event.getOldState());
    BlockQuery placeQuery = Queries.block(event, actor, event.getNewState());
    BlockQuery againstQuery =
        againstBlock == null ? null : Queries.block(event, actor, againstBlock);

    if (this.useRegionPriority) {
      // Note that the event may be in multiple scopes, which is why they must all be handled in the
      // same pass
      rfaLoop:
      for (RegionFilterApplication rfa : this.rfaContext.getAll()) {
        switch (rfa.scope) {
          case BLOCK_BREAK:
            if (event.isBreak() && rfa.region.contains(event.getOldState())) {
              if (processQuery(rfa, breakQuery)) {
                break rfaLoop;
              }
            }
            break;

          case BLOCK_PLACE:
            if (event.isPlace() && rfa.region.contains(event.getNewState())) {
              if (processQuery(rfa, placeQuery)) {
                break rfaLoop;
              }
            }
            break;

          case BLOCK_PLACE_AGAINST:
            if (againstQuery != null) {
              if (rfa.region.contains(againstQuery.getBlock())) {
                if (processQuery(rfa, againstQuery)) {
                  break rfaLoop;
                }
              }
            }
            break;
        }
      }
    } else {
      // Legacy behavior
      if (event.isPlace()) {
        for (RegionFilterApplication rfa : this.rfaContext.get(RFAScope.BLOCK_PLACE)) {
          if (rfa.region.contains(pos) && processQuery(rfa, placeQuery) && rfa.useRegionPriority) {
            break;
          }
        }
      } else {
        for (RegionFilterApplication rfa : this.rfaContext.get(RFAScope.BLOCK_BREAK)) {
          if (rfa.region.contains(pos) && processQuery(rfa, breakQuery) && rfa.useRegionPriority) {
            break;
          }
        }
      }
    }
  }

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  public void checkBlockPhysics(final BlockPhysicsEvent event) {
    tc.oc.pgm.filters.query.BlockQuery query =
        new tc.oc.pgm.filters.query.BlockQuery(event, event.getBlock().getState());
    for (RegionFilterApplication rfa : this.rfaContext.get(RFAScope.BLOCK_PHYSICS)) {
      if (rfa.region.contains(event.getBlock()) && processQuery(rfa, query)) break;
    }
  }

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  public void checkBlockDamage(final BlockDamageEvent event) {
    MatchPlayer player = this.match.getParticipant(event.getPlayer());
    if (player == null) return;

    PlayerBlockQuery query =
        new PlayerBlockQuery(event, player, event.getBlock().getState());

    for (RegionFilterApplication rfa : this.rfaContext.get(RFAScope.BLOCK_BREAK)) {
      if (rfa.earlyWarning && rfa.region.contains(event.getBlock())) {
        if (processQuery(rfa, query)) {
          if (event.isCancelled() && rfa.message != null) {
            player.sendWarning(rfa.message);
          }
          if (this.useRegionPriority || rfa.useRegionPriority) {
            break;
          }
        }
      }
    }
  }

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  public void checkUse(final PlayerInteractEvent event) {
    if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
      Block block = event.getClickedBlock();
      if (block == null) return;

      this.handleUse(event, block.getState(), null, this.match.getParticipant(event.getPlayer()));
    }
  }

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  public void checkHangingPlace(final HangingPlaceEvent event) {
    Hanging entity = event.getEntity();
    Block block = entity.getLocation().getBlock();
    this.handleHangingPlace(event, block, getHangingType(entity), event.getPlayer());
  }

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  public void checkHangingBreak(final HangingBreakByEntityEvent event) {
    this.handleHangingBreak(event, event.getEntity(), event.getRemover());
  }

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  public void checkItemFrameItemRemove(EntityDamageByEntityEvent event) {
    // This event is fired when popping an item out of an item frame, without breaking the frame
    // itself
    if (event.getEntity() instanceof ItemFrame
        && ((ItemFrame) event.getEntity()).getItem() != null) {
      this.handleHangingBreak(event, (Hanging) event.getEntity(), event.getDamager());
    }
  }

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  public void checkItemFrameRotate(PlayerInteractEntityEvent event) {
    if (event.getRightClicked() instanceof ItemFrame itemFrame) {
      if (itemFrame.getItem() != null) {
        // If frame contains an item, right-click will rotate it, which is handled as a "use" event
        this.handleUse(
            event,
            itemFrame.getLocation().getBlock().getState(),
            getHangingType(itemFrame),
            this.match.getParticipant(event.getPlayer()));
      } else if (event.getPlayer().getItemInHand() != null) {
        // If the frame is empty and it's right clicked with an item, this will place the item in
        // the frame,
        // which is handled as a "place" event, with the placed item as the block world
        this.handleHangingPlace(
            event,
            itemFrame.getLocation().getBlock(),
            MaterialData.item(event.getPlayer().getItemInHand()),
            event.getPlayer());
      }
    }
  }

  private void handleUse(
      Event event, BlockState blockState, @Nullable MaterialData md, @Nullable MatchPlayer player) {
    if (!MatchPlayers.canInteract(player)) return;

    PlayerBlockQuery query = new PlayerBlockQuery(event, player, blockState).withMaterial(md);

    for (RegionFilterApplication rfa : this.rfaContext.get(RFAScope.USE)) {
      if (rfa.region.contains(blockState) && processQuery(rfa, query)) {
        if (query.getEvent() instanceof PlayerInteractEvent pie && pie.isCancelled()) {
          pie.setCancelled(false);
          pie.setUseItemInHand(Event.Result.ALLOW);
          pie.setUseInteractedBlock(Event.Result.DENY);

          if (rfa.message != null) {
            player.sendWarning(rfa.message);
          }
        }
        if (this.useRegionPriority || rfa.useRegionPriority) {
          break;
        }
      }
    }
  }

  private void handleHangingPlace(Event event, Block block, MaterialData material, Entity placer) {
    Query query = makeBlockQuery(event, placer, block, material);

    for (RegionFilterApplication rfa : this.rfaContext.get(RFAScope.BLOCK_PLACE)) {
      if (rfa.region.contains(block) && processQuery(rfa, query)) {
        sendCancelMessage(rfa, query);
        if (this.useRegionPriority || rfa.useRegionPriority) break;
      }
    }
  }

  private void handleHangingBreak(Event event, Hanging hanging, Entity breaker) {
    MaterialData material = getHangingType(hanging);
    if (material == null) return;
    Block block = breaker.getLocation().getBlock();

    Query query = makeBlockQuery(event, breaker, block, material);

    for (RegionFilterApplication rfa : this.rfaContext.get(RFAScope.BLOCK_BREAK)) {
      if (rfa.region.contains(block) && processQuery(rfa, query)) {
        sendCancelMessage(rfa, query);
        if (this.useRegionPriority || rfa.useRegionPriority) break;
      }
    }
  }

  private void sendCancelMessage(RegionFilterApplication rfa, Query query) {
    if (rfa.message != null
        && query.getEvent() instanceof Cancellable
        && ((Cancellable) query.getEvent()).isCancelled()
        && query instanceof PlayerQuery) {

      MatchPlayer player = ((PlayerQuery) query).getPlayer();
      if (player != null) player.sendWarning(rfa.message);
    }
  }

  private Query makeBlockQuery(Event event, Entity entity, Block block, MaterialData md) {
    if (entity instanceof Player) {
      MatchPlayer player = this.match.getPlayer((Player) entity);
      if (MatchPlayers.canInteract(player)) {
        return new PlayerBlockQuery(event, player, block.getState()).withMaterial(md);
      }
    }
    return new tc.oc.pgm.filters.query.BlockQuery(event, block).withMaterial(md);
  }

  private ParticipantState getActor(BlockTransformEvent event) {
    // Legacy maps assume that all TNT damage is done by "world"
    if (match.getMap().getProto().isOlderThan(MapProtos.FILTER_OWNED_TNT)
        && event.getCause() instanceof EntityExplodeEvent) return null;

    return ParticipantBlockTransformEvent.getPlayerState(event);
  }

  private static MaterialData getHangingType(Hanging hanging) {
    return MATERIAL_UTILS.createItemData(hanging);
  }

  /**
   * Query the RFA's filter with the given objects. If the query is denied, cancel the event and set
   * the deny message. If the query is allowed, un-cancel the event. If the query abstains, do
   * nothing.
   *
   * @return false if the query abstained, otherwise true
   */
  protected static boolean processQuery(RegionFilterApplication rfa, Query query) {
    if (rfa.filter == null) {
      return false;
    }

    switch (rfa.filter.query(query)) {
      case ALLOW:
        if (query.getEvent() instanceof Cancellable) {
          ((Cancellable) query.getEvent()).setCancelled(false);
        }
        return true;

      case DENY:
        if (query.getEvent() instanceof GeneralizedEvent) {
          ((GeneralizedEvent) query.getEvent()).setCancelled(rfa.message);
        } else if (query.getEvent() instanceof Cancellable) {
          ((Cancellable) query.getEvent()).setCancelled(true);
        }
        return true;

      default:
        return false;
    }
  }
}
