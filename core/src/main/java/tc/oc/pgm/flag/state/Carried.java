package tc.oc.pgm.flag.state;

import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;
import static net.kyori.adventure.title.Title.title;
import static tc.oc.pgm.util.TimeUtils.fromTicks;
import static tc.oc.pgm.util.bukkit.InventoryViewUtil.INVENTORY_VIEW;
import static tc.oc.pgm.util.nms.Packets.PLAYERS;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.filter.query.Query;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.filters.query.PlayerQuery;
import tc.oc.pgm.filters.query.PlayerStateQuery;
import tc.oc.pgm.flag.Flag;
import tc.oc.pgm.flag.FlagDefinition;
import tc.oc.pgm.flag.NetDefinition;
import tc.oc.pgm.flag.Post;
import tc.oc.pgm.flag.event.FlagCaptureEvent;
import tc.oc.pgm.flag.event.FlagStateChangeEvent;
import tc.oc.pgm.goals.events.GoalEvent;
import tc.oc.pgm.kits.ArmorType;
import tc.oc.pgm.kits.Kit;
import tc.oc.pgm.score.ScoreMatchModule;
import tc.oc.pgm.scoreboard.SidebarMatchModule;
import tc.oc.pgm.spawns.events.ParticipantDespawnEvent;
import tc.oc.pgm.teams.TeamFactory;
import tc.oc.pgm.teams.TeamMatchModule;
import tc.oc.pgm.util.named.NameStyle;

/** State of a flag when a player has picked it up and is wearing the banner on their head. */
public class Carried extends Spawned implements Missing {

  protected final MatchPlayer carrier;
  protected ItemStack helmetItem;
  protected @Nullable NetDefinition deniedByNet;
  protected @Nullable Flag deniedByFlag;
  protected @Nullable Component lastMessage;

  private static final int DROP_QUEUE_SIZE = 100;
  private final Deque<Location> dropLocations = new ArrayDeque<>(DROP_QUEUE_SIZE);

  public Carried(Flag flag, Post post, MatchPlayer carrier, Location dropLocation) {
    super(flag, post);
    this.carrier = carrier;
    this.dropLocations.add(
        dropLocation); // Need an initial dropLocation in case the carrier never generates ones
  }

  @Override
  public boolean isRecoverable() {
    return true;
  }

  @Override
  public Location getLocation() {
    return this.carrier.getBukkit().getLocation();
  }

  @Override
  public Iterable<Location> getProximityLocations(ParticipantState player) {
    if (isCarrying(player)) {
      final Query query = new PlayerStateQuery(player);
      return flag.getNets().stream()
          .flatMap(net -> {
            if (net.getCaptureFilter().query(query).isAllowed()) {
              return Stream.of(
                  net.getProximityLocation().toLocation(flag.getMatch().getWorld()));
            } else {
              return Stream.empty();
            }
          })
          .collect(Collectors.toList());
    } else {
      return super.getProximityLocations(player);
    }
  }

  @Override
  public void enterState() {
    super.enterState();

    Kit kit = this.flag.getDefinition().getPickupKit();
    if (kit != null) carrier.applyKit(kit, false);
    kit = this.flag.getDefinition().getCarryKit();
    if (kit != null) carrier.applyKit(kit, false);

    this.helmetItem = this.carrier.getBukkit().getInventory().getHelmet();
    this.carrier.getBukkit().getInventory().setHelmet(this.flag.getBannerItem().clone());

    PGM.get()
        .getExecutor()
        .schedule(
            () -> PLAYERS.sendLegacyHelmet(carrier.getBukkit(), flag.getLegacyBannerItem()),
            50L,
            TimeUnit.MILLISECONDS);

    SidebarMatchModule smm = this.flag.getMatch().getModule(SidebarMatchModule.class);
    if (smm != null) smm.blinkGoal(this.flag, 2, null);

    if (this.flag.getDefinition().willShowRespawnOnPickup()) {
      String postName = post.peekNext(flag).getPostName();
      if (postName != null) { // The post needs a name in order to display the message.
        this.flag
            .getMatch()
            .sendMessage(translatable(
                "flag.willRespawn.next",
                this.flag.getComponentName(),
                text(postName, NamedTextColor.AQUA)));
      }
    }
  }

  @Override
  public void leaveState() {
    SidebarMatchModule smm = this.flag.getMatch().getModule(SidebarMatchModule.class);
    if (smm != null) smm.stopBlinkingGoal(this.flag);

    this.carrier.sendActionBar(empty());

    this.carrier.getInventory().remove(this.flag.getBannerItem());
    this.carrier.getInventory().setHelmet(this.helmetItem);

    Kit kit = this.flag.getDefinition().getDropKit();
    if (kit != null) this.carrier.applyKit(kit, false);
    kit = this.flag.getDefinition().getCarryKit();
    if (kit != null) kit.remove(this.carrier);

    super.leaveState();
  }

  protected Competitor getBeneficiary(TeamFactory owner) {
    if (owner != null) {
      return this.flag.getMatch().needModule(TeamMatchModule.class).getTeam(owner);
    } else {
      return this.carrier.getCompetitor();
    }
  }

  protected Component getMessage() {
    Component message;
    if (this.deniedByNet == null) {
      if (this.flag.getDefinition().getCarryMessage() != null) {
        message = this.flag.getDefinition().getCarryMessage();
      } else {
        message = translatable("flag.carrying", this.flag.getComponentName());
      }

      return message.color(NamedTextColor.AQUA).decoration(TextDecoration.BOLD, true);
    } else {
      if (this.deniedByNet.getDenyMessage() != null) {
        message = this.deniedByNet.getDenyMessage();
      } else if (this.deniedByFlag != null) {
        message = translatable(
            "flag.captureDenied.byFlag",
            this.flag.getComponentName(),
            this.deniedByFlag.getComponentName());
      } else {
        message = translatable("flag.captureDenied", this.flag.getComponentName());
      }

      return message.color(NamedTextColor.RED).decoration(TextDecoration.BOLD, true);
    }
  }

  @Override
  public void tickRunning() {
    super.tickRunning();

    Component message = this.getMessage();
    this.carrier.sendActionBar(message);

    if (!message.equals(this.lastMessage)) {
      this.lastMessage = message;
      this.carrier.showTitle(
          title(empty(), message, Title.Times.times(Duration.ZERO, fromTicks(5), fromTicks(35))));
    }

    ScoreMatchModule smm = this.flag.getMatch().getModule(ScoreMatchModule.class);
    if (smm != null && this.flag.getDefinition().getPointsPerSecond() != 0) {
      smm.incrementScore(
          this.getBeneficiary(this.flag.getDefinition().getOwner()),
          this.flag.getDefinition().getPointsPerSecond() / 20D);
    }
  }

  @Override
  public boolean isCarrying(MatchPlayer player) {
    return this.carrier == player;
  }

  @Override
  public boolean isCarrying(Party party) {
    return this.carrier.getParty() == party;
  }

  @Override
  protected boolean canSeeParticles(Player player) {
    return player != this.carrier.getBukkit();
  }

  protected void dropFlag() {
    for (Location dropLocation : this.dropLocations) {
      if (this.flag.canDrop(new PlayerQuery(null, carrier, dropLocation))) {
        this.flag.transition(new Dropped(this.flag, this.post, dropLocation, this.carrier));
        return;
      }
    }

    // Could not find a usable drop location, just recover the flag
    this.recover();
  }

  protected void captureFlag(NetDefinition net) {
    this.carrier.sendMessage(translatable("flag.capture.you", this.flag.getComponentName()));

    this.flag
        .getMatch()
        .sendMessage(translatable(
            "flag.capture.player",
            this.flag.getComponentName(),
            this.carrier.getName(NameStyle.COLOR)));

    this.flag.resetTouches(this.carrier.getCompetitor());
    this.flag.resetProximity(this.carrier.getCompetitor());

    ScoreMatchModule smm = this.flag.getMatch().getModule(ScoreMatchModule.class);
    if (smm != null) {
      if (net.getPointsPerCapture() != 0) {
        smm.incrementScore(this.getBeneficiary(net.getOwner()), net.getPointsPerCapture());
      }

      if (this.flag.getDefinition().getPointsPerCapture() != 0) {
        smm.incrementScore(
            this.getBeneficiary(this.flag.getDefinition().getOwner()),
            this.flag.getDefinition().getPointsPerCapture());
      }
    }

    Post post = this.post;
    if (net.getReturnPost() != null) post = flag.getPost(net.getReturnPost());

    if (post.isPermanent()) {
      this.flag.transition(new Completed(this.flag, post));
    } else {
      this.flag.transition(new Captured(this.flag, post, net, this.getLocation()));
    }

    FlagCaptureEvent event = new FlagCaptureEvent(this.flag, this.carrier, net);
    this.flag.getMatch().callEvent(event);
  }

  public MatchPlayer getCarrier() {
    return this.carrier;
  }

  protected boolean isCarrier(MatchPlayer player) {
    return this.carrier == player;
  }

  protected boolean isCarrier(Entity player) {
    return this.carrier.getBukkit() == player;
  }

  protected boolean isFlag(ItemStack stack) {
    return stack.isSimilar(this.flag.getBannerItem());
  }

  @Override
  public void onEvent(PlayerDropItemEvent event) {
    super.onEvent(event);
    if (this.isCarrier(event.getPlayer()) && this.isFlag(event.getItemDrop().getItemStack())) {
      event.getItemDrop().remove();
      this.dropFlag();
    }
  }

  @Override
  public void onEvent(ParticipantDespawnEvent event) {
    super.onEvent(event);
    if (this.isCarrier(event.getPlayer())) {
      this.dropFlag();
    }
  }

  @Override
  public void onEvent(InventoryClickEvent event) {
    super.onEvent(event);
    if (isCarrier(event.getWhoClicked()) && event.getSlot() == ArmorType.HELMET.inventorySlot()) {
      event.setCancelled(true);
      INVENTORY_VIEW.setCursor(event.getView(), null);
      event.setCurrentItem(null);
      this.flag.getMatch().getExecutor(MatchScope.RUNNING).execute(() -> {
        if (isCurrent()) dropFlag();
      });
    }
  }

  @Override
  public void onEvent(PlayerMoveEvent event) {
    super.onEvent(event);

    if (this.isCarrier(event.getPlayer())) {
      Location playerLoc = event.getTo();

      // Only check the filter if there are no other possible fallback locations and the last drop
      // location hasn't already been chacked
      if ((dropLocations.isEmpty() || !dropLocations.peekLast().equals(playerLoc))
          && flag.canDrop(new PlayerQuery(null, carrier, playerLoc))) {
        if (this.dropLocations.size() >= DROP_QUEUE_SIZE) this.dropLocations.removeLast();
        this.dropLocations.addFirst(playerLoc);
      }

      this.checkCapture(event.getTo());
    }
  }

  @Override
  public void onEvent(GoalEvent event) {
    super.onEvent(event);
    this.checkCapture(null);
  }

  @Override
  public void onEvent(FlagStateChangeEvent event) {
    super.onEvent(event);
    this.checkCapture(null);
  }

  protected void checkCapture(Location to) {
    if (to == null) to = this.carrier.getBukkit().getLocation();

    this.deniedByFlag = null;
    if (this.deniedByNet != null && !this.deniedByNet.isSticky()) {
      this.deniedByNet = null;
    }

    for (NetDefinition net : this.flag.getNets()) {
      if (net.getRegion().contains(to)) {
        if (tryCapture(net)) {
          return;
        } else {
          this.deniedByNet = net;
        }
      }
    }

    if (this.deniedByNet != null) {
      tryCapture(this.deniedByNet);
    }
  }

  protected boolean tryCapture(NetDefinition net) {
    for (FlagDefinition returnableDef : net.getRecoverableFlags()) {
      Flag returnable = returnableDef.getGoal(this.flag.getMatch());
      if (returnable.isCurrent(Carried.class)) {
        this.deniedByFlag = returnable;
        return false;
      }
    }

    if (this.flag.canCapture(this.carrier, net)) {
      this.captureFlag(net);
      return true;
    } else {
      return false;
    }
  }

  @Override
  public TextColor getStatusColor(Party viewer) {
    if (this.flag.getDefinition().hasMultipleCarriers()) {
      return this.carrier.getParty().getTextColor();
    } else {
      return super.getStatusColor(viewer);
    }
  }

  @Override
  public Component getStatusSymbol(Party viewer) {
    return Flag.CARRIED_SYMBOL;
  }
}
