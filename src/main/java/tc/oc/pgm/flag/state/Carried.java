package tc.oc.pgm.flag.state;

import com.google.common.base.Function;
import java.util.ArrayDeque;
import java.util.Deque;
import javax.annotation.Nullable;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import tc.oc.component.Component;
import tc.oc.component.types.PersonalizedText;
import tc.oc.component.types.PersonalizedTranslatable;
import tc.oc.named.NameStyle;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.filters.query.IQuery;
import tc.oc.pgm.filters.query.PlayerStateQuery;
import tc.oc.pgm.flag.Flag;
import tc.oc.pgm.flag.FlagDefinition;
import tc.oc.pgm.flag.Net;
import tc.oc.pgm.flag.Post;
import tc.oc.pgm.flag.event.FlagCaptureEvent;
import tc.oc.pgm.flag.event.FlagStateChangeEvent;
import tc.oc.pgm.goals.events.GoalEvent;
import tc.oc.pgm.kits.ArmorType;
import tc.oc.pgm.kits.Kit;
import tc.oc.pgm.kits.KitMatchModule;
import tc.oc.pgm.score.ScoreMatchModule;
import tc.oc.pgm.scoreboard.SidebarMatchModule;
import tc.oc.pgm.spawns.events.ParticipantDespawnEvent;
import tc.oc.pgm.teams.TeamFactory;
import tc.oc.pgm.teams.TeamMatchModule;
import tc.oc.util.collection.IterableUtils;
import tc.oc.util.components.Components;
import tc.oc.world.NMSHacks;

/** State of a flag when a player has picked it up and is wearing the banner on their head. */
public class Carried extends Spawned implements Missing {

  protected final MatchPlayer carrier;
  protected ItemStack helmetItem;
  protected boolean helmetLocked;
  protected @Nullable Net deniedByNet;
  protected @Nullable Flag deniedByFlag;
  protected @Nullable Component lastMessage;

  private static final int DROP_QUEUE_SIZE = 100;
  private Deque<Location> dropLocations = new ArrayDeque<>(DROP_QUEUE_SIZE);

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
      final IQuery query = new PlayerStateQuery(player);
      return IterableUtils.transfilter(
          flag.getNets(),
          new Function<Net, Location>() {
            @Override
            public Location apply(Net net) {
              if (net.getCaptureFilter().query(query).isAllowed()) {
                return net.getProximityLocation().toLocation(flag.getMatch().getWorld());
              } else {
                return null;
              }
            }
          });
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
    this.helmetLocked =
        this.flag
            .getMatch()
            .getModule(KitMatchModule.class)
            .lockArmorSlot(this.carrier, ArmorType.HELMET, false);

    this.carrier.getBukkit().getInventory().setHelmet(this.flag.getBannerItem().clone());

    SidebarMatchModule smm = this.flag.getMatch().getModule(SidebarMatchModule.class);
    if (smm != null) smm.blinkGoal(this.flag, 2, null);
  }

  @Override
  public void leaveState() {
    SidebarMatchModule smm = this.flag.getMatch().getModule(SidebarMatchModule.class);
    if (smm != null) smm.stopBlinkingGoal(this.flag);

    NMSHacks.sendHotbarMessage(this.carrier.getBukkit(), "");

    this.carrier.getInventory().remove(this.flag.getBannerItem());
    this.carrier.getInventory().setHelmet(this.helmetItem);

    this.flag
        .getMatch()
        .getModule(KitMatchModule.class)
        .lockArmorSlot(this.carrier, ArmorType.HELMET, this.helmetLocked);

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
        message =
            new PersonalizedTranslatable("match.flag.carrying.you", this.flag.getComponentName());
      }

      message.setColor(net.md_5.bungee.api.ChatColor.AQUA);
      message.setBold(true);
      return message;
    } else {
      if (this.deniedByNet.getDenyMessage() != null) {
        message = this.deniedByNet.getDenyMessage();
      } else if (this.deniedByFlag != null) {
        message =
            new PersonalizedTranslatable(
                "match.flag.captureDenied.byFlag",
                this.flag.getComponentName(),
                this.deniedByFlag.getComponentName());
      } else {
        message =
            new PersonalizedTranslatable("match.flag.captureDenied", this.flag.getComponentName());
      }

      message.setColor(net.md_5.bungee.api.ChatColor.RED);
      message.setBold(true);
      return message;
    }
  }

  @Override
  public void tickRunning() {
    super.tickRunning();

    Component message = this.getMessage();
    this.carrier.sendHotbarMessage(
        message instanceof PersonalizedTranslatable
            ? ((PersonalizedTranslatable) message).getPersonalizedText()
            : message);

    if (!Components.equals(message, this.lastMessage)) {
      this.lastMessage = message;
      this.carrier.showTitle(new PersonalizedText(), message, 0, 5, 35);
    }

    ScoreMatchModule smm = this.flag.getMatch().getModule(ScoreMatchModule.class);
    if (smm != null && this.flag.getDefinition().getPointsPerSecond() > 0) {
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
      if (this.flag.canDropAt(dropLocation)) {
        this.flag.transition(new Dropped(this.flag, this.post, dropLocation, this.carrier));
        return;
      }
    }

    // Could not find a usable drop location, just recover the flag
    this.recover();
  }

  protected void captureFlag(Net net) {
    this.carrier.sendMessage(
        new PersonalizedTranslatable("match.flag.capture.you", this.flag.getComponentName()));

    this.flag
        .getMatch()
        .sendMessage(
            new PersonalizedTranslatable(
                "match.flag.capture",
                this.flag.getComponentName(),
                this.carrier.getStyledName(NameStyle.COLOR)));

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

    Post post = net.getReturnPost() != null ? net.getReturnPost() : this.post;
    if (post.isPermanent()) {
      this.flag.transition(new Completed(this.flag, post));
    } else {
      this.flag.transition(new Captured(this.flag, post, net, this.getLocation()));
    }

    FlagCaptureEvent event = new FlagCaptureEvent(this.flag, this.carrier, net);
    this.flag.getMatch().callEvent(event);
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
    if (this.isCarrier(event.getWhoClicked())
        && event.getSlot() == ArmorType.HELMET.inventorySlot()) {
      event.setCancelled(true);
      event.getView().setCursor(null);
      event.setCurrentItem(null);
      this.flag
          .getMatch()
          .getScheduler(MatchScope.RUNNING)
          .runTask(
              new Runnable() {
                @Override
                public void run() {
                  if (isCurrent()) dropFlag();
                }
              });
    }
  }

  @Override
  public void onEvent(PlayerMoveEvent event) {
    super.onEvent(event);

    if (this.isCarrier(event.getPlayer())) {
      Location playerLoc = event.getTo();

      if (this.flag.canDropAt(playerLoc)) {
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

    for (Net net : this.flag.getNets()) {
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

  protected boolean tryCapture(Net net) {
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
  public org.bukkit.ChatColor getStatusColor(Party viewer) {
    if (this.flag.getDefinition().hasMultipleCarriers()) {
      return this.carrier.getParty().getColor();
    } else {
      return super.getStatusColor(viewer);
    }
  }

  @Override
  public String getStatusSymbol(Party viewer) {
    return Flag.CARRIED_SYMBOL;
  }
}
