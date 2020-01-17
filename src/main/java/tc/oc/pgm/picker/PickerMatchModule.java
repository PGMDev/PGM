package tc.oc.pgm.picker;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerLocaleChangeEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.material.MaterialData;
import tc.oc.item.Items;
import tc.oc.pgm.AllTranslations;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.chat.Sound;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.match.event.MatchFinishEvent;
import tc.oc.pgm.api.match.event.MatchStartEvent;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.setting.SettingKey;
import tc.oc.pgm.blitz.BlitzMatchModule;
import tc.oc.pgm.classes.ClassMatchModule;
import tc.oc.pgm.classes.PlayerClass;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.events.PlayerJoinMatchEvent;
import tc.oc.pgm.events.PlayerPartyChangeEvent;
import tc.oc.pgm.join.GenericJoinResult;
import tc.oc.pgm.join.JoinMatchModule;
import tc.oc.pgm.join.JoinResult;
import tc.oc.pgm.spawns.events.DeathKitApplyEvent;
import tc.oc.pgm.spawns.events.ObserverKitApplyEvent;
import tc.oc.pgm.teams.Team;
import tc.oc.pgm.teams.TeamMatchModule;
import tc.oc.util.StringUtils;
import tc.oc.util.components.ComponentUtils;

@ListenerScope(MatchScope.LOADED)
public class PickerMatchModule implements MatchModule, Listener {
  private static final String OPEN_BUTTON_PREFIX = ChatColor.GREEN + ChatColor.BOLD.toString();
  private static final int OPEN_BUTTON_SLOT = 2;

  private static final int LORE_WIDTH_PIXELS = 120;
  private static final int WIDTH = 9; // Inventory width in slots (for readability)

  enum Button {
    AUTO_JOIN(Material.CHAINMAIL_HELMET),
    TEAM_JOIN(Material.LEATHER_HELMET),
    JOIN(Material.LEATHER_HELMET),
    LEAVE(Material.LEATHER_BOOTS),
    ;

    public final Material material;

    Button(Material material) {
      this.material = material;
    }

    public boolean matches(MaterialData material) {
      return this.material.equals(material.getItemType());
    }
  }

  private final Match match;
  private final Set<MatchPlayer> picking = new HashSet<>();
  private final boolean hasTeams;
  private final boolean hasClasses;
  private final boolean isBlitz;

  public PickerMatchModule(Match match) {
    this.match = match;
    this.hasTeams = match.hasModule(TeamMatchModule.class);
    this.hasClasses = match.hasModule(ClassMatchModule.class);
    this.isBlitz = match.hasModule(BlitzMatchModule.class);
  }

  protected boolean settingEnabled(MatchPlayer player, boolean playerTriggered) {
    boolean hasPermission = player.getBukkit().hasPermission(Permissions.JOIN_CHOOSE);
    switch (player.getSettings().getValue(SettingKey.PICKER)) {
      case PICKER_OFF: // When off never show GUI
        return false;
      case PICKER_ON: // When on always show the GUI
        return true;
      default: // Display after map cycle, but check perms when clicking button.
        return (playerTriggered ? hasPermission : true);
    }
  }

  private boolean hasJoined(MatchPlayer joining) {
    return joining.isParticipating()
        || match.needModule(JoinMatchModule.class).isQueuedToJoin(joining);
  }

  private boolean canAutoJoin(MatchPlayer joining) {
    JoinResult result = match.needModule(JoinMatchModule.class).queryJoin(joining, null);
    return result.isSuccess()
        || ((result instanceof GenericJoinResult)
            && ((GenericJoinResult) result).getStatus() == GenericJoinResult.Status.FULL);
  }

  private boolean canChooseMultipleTeams(MatchPlayer joining) {
    return getChoosableTeams(joining).size() > 1;
  }

  private Set<Team> getChoosableTeams(MatchPlayer joining) {
    TeamMatchModule tmm = match.getModule(TeamMatchModule.class);
    if (tmm == null) return Collections.emptySet();

    Set<Team> teams = new HashSet<>();
    for (Team team : tmm.getTeams()) {
      JoinResult result = tmm.queryJoin(joining, team);
      // We still want to show the button if the team is full
      // or the player doesn't have join perms.
      if (result.isSuccess()
          || result instanceof GenericJoinResult
              && (((GenericJoinResult) result).getStatus() == GenericJoinResult.Status.FULL
                  || ((GenericJoinResult) result).getStatus()
                      == GenericJoinResult.Status.CHOICE_DENIED)) {

        teams.add(team);
      }
    }

    return teams;
  }

  /** Does the player have any use for the picker? */
  private boolean canUse(MatchPlayer player) {
    if (player == null) return false;

    // Player is eliminated from Blitz
    if (isBlitz && match.isRunning()) return false;

    // Player is not observing or dead
    if (!(player.isObserving() || player.isDead())) return false;

    // Not allowed to join teams
    if (!canAutoJoin(player)) return false;

    return true;
  }

  /**
   * Does the player have any use for the picker dialog? If the player can join, but there is
   * nothing to pick (i.e. FFA without classes) then this returns false, while {@link #canUse}
   * returns true.
   */
  private boolean canOpenWindow(MatchPlayer player) {
    return canUse(player) && (hasClasses || canChooseMultipleTeams(player));
  }

  private boolean isPicking(MatchPlayer player) {
    return picking.contains(player);
  }

  private void refreshCountsAll() {
    for (MatchPlayer player : ImmutableSet.copyOf(picking)) {
      refreshWindow(player);
    }
  }

  private String getWindowTitle(MatchPlayer player) {
    checkState(hasTeams || hasClasses); // Window should not open if there is nothing to pick

    String key;
    if (hasTeams && hasClasses) {
      key = "teamClass.picker.title";
    } else if (hasTeams) {
      key = "teamSelection.picker.title";
    } else {
      key = "class.picker.title";
    }

    return ChatColor.DARK_RED + AllTranslations.get().translate(key, player.getBukkit());
  }

  private ItemStack createJoinButton(final MatchPlayer player) {
    ItemStack stack = new ItemStack(Button.JOIN.material);

    ItemMeta meta = stack.getItemMeta();

    String key;
    if (!canOpenWindow(player)) {
      key = "ffa.picker.displayName";
    } else if (hasTeams && hasClasses) {
      key = "teamClass.picker.displayName";
    } else if (hasTeams) {
      key = "teamSelection.picker.displayName";
    } else if (hasClasses) {
      key = "class.picker.displayName";
    } else {
      key = "ffa.picker.displayName";
    }

    meta.setDisplayName(
        OPEN_BUTTON_PREFIX + AllTranslations.get().translate(key, player.getBukkit()));
    meta.setLore(
        Lists.newArrayList(
            ChatColor.DARK_PURPLE
                + AllTranslations.get()
                    .translate("teamSelection.picker.tooltip", player.getBukkit())));

    stack.setItemMeta(meta);
    return stack;
  }

  private ItemStack createLeaveButton(final MatchPlayer player) {
    ItemStack stack = new ItemStack(Button.LEAVE.material);

    ItemMeta meta = stack.getItemMeta();
    meta.setDisplayName(
        OPEN_BUTTON_PREFIX
            + AllTranslations.get().translate("leave.picker.displayName", player.getBukkit()));
    meta.setLore(
        Lists.newArrayList(
            ChatColor.DARK_PURPLE
                + AllTranslations.get().translate("leave.picker.tooltip", player.getBukkit())));

    stack.setItemMeta(meta);
    return stack;
  }

  public void refreshKit(final MatchPlayer player) {
    if (canUse(player)) {
      match.getLogger().fine("Giving kit to " + player);

      ItemStack stack;
      if (hasJoined(player) && !canOpenWindow(player)) {
        stack = createLeaveButton(player);
      } else {
        stack = createJoinButton(player);
      }

      player.getInventory().setItem(OPEN_BUTTON_SLOT, stack);
    } else if (player != null) {
      player.getInventory().setItem(OPEN_BUTTON_SLOT, null);
    }
  }

  private void refreshKitAll() {
    for (MatchPlayer player : match.getObservers()) {
      refreshKit(player);
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void join(PlayerJoinMatchEvent event) {
    final MatchPlayer player = event.getPlayer();
    if (!settingEnabled(player, false)) return;

    if (canOpenWindow(player)) {
      event
          .getMatch()
          .getScheduler(MatchScope.LOADED)
          .runTask(
              new Runnable() {
                @Override
                public void run() {
                  if (player.getBukkit().isOnline()) {
                    showWindow(player);
                  }
                }
              });
    }
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void checkInventoryClick(final InventoryClickEvent event) {
    if (event.getCurrentItem() == null
        || event.getCurrentItem().getItemMeta() == null
        || event.getCurrentItem().getItemMeta().getDisplayName() == null) return;
    if (event.getWhoClicked() instanceof Player) {
      MatchPlayer player = match.getPlayer((Player) event.getWhoClicked());
      if (player == null || !this.picking.contains(player)) return;

      this.handleInventoryClick(
          player,
          ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName()),
          event.getCurrentItem().getData());
      event.setCancelled(true);
    }
  }

  @EventHandler
  public void handleLocaleChange(final PlayerLocaleChangeEvent event) {
    refreshKit(match.getPlayer(event.getPlayer()));
  }

  @EventHandler
  public void closeMonitoredInventory(final InventoryCloseEvent event) {
    this.picking.remove(match.getPlayer((Player) event.getPlayer()));
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void rightClickIcon(final PlayerInteractEvent event) {
    if (!(event.getAction() == Action.RIGHT_CLICK_AIR
        || event.getAction() == Action.RIGHT_CLICK_BLOCK)) return;

    MatchPlayer player = match.getPlayer(event.getPlayer());
    if (player == null) return;

    if (!canUse(player)) return;

    ItemStack hand = event.getPlayer().getItemInHand();
    if (Items.isNothing(hand)) return;

    String displayName = hand.getItemMeta().getDisplayName();
    if (displayName == null) return;

    boolean handled = false;
    final JoinMatchModule jmm = match.needModule(JoinMatchModule.class);

    if (hand.getType() == Button.JOIN.material) {
      handled = true;
      if (canOpenWindow(player) && settingEnabled(player, true)) {
        showWindow(player);
      } else {
        // If there is nothing to pick or setting is disabled, just join immediately
        jmm.join(player, null);
      }
    } else if (hand.getType() == Button.LEAVE.material) {
      jmm.leave(player);
    }

    if (handled) {
      event.setUseInteractedBlock(Event.Result.DENY);
      event.setUseItemInHand(Event.Result.DENY);

      // Unfortunately, not opening the window seems to cause the player to put the helmet
      // on their head, no matter what we do server-side. So, we have to force an inventory
      // update to resync them.
      event.getPlayer().updateInventory();
    }
  }

  @EventHandler
  public void giveKitToObservers(final ObserverKitApplyEvent event) {
    refreshKit(event.getPlayer());
  }

  @EventHandler
  public void giveKitToDead(final DeathKitApplyEvent event) {
    refreshKit(event.getPlayer());
  }

  @EventHandler
  public void teamSwitch(final PlayerPartyChangeEvent event) {
    refreshCountsAll();
    refreshKit(event.getPlayer());

    if (event.getNewParty() == null) {
      picking.remove(event.getPlayer());
    }
  }

  @EventHandler
  public void matchBegin(final MatchStartEvent event) {
    refreshCountsAll();
    refreshKitAll();
  }

  @EventHandler
  public void matchEnd(final MatchFinishEvent event) {
    refreshCountsAll();
    refreshKitAll();
  }

  /**
   * Open the window for the given player, or refresh its contents if they already have it open, and
   * return the current contents.
   *
   * <p>If the window is currently open but too small to hold the current contents, it will be
   * closed and reopened.
   *
   * <p>If the player is not currently allowed to have the window open, close any window they have
   * open and return null.
   */
  private @Nullable Inventory showWindow(MatchPlayer player) {
    if (!checkWindow(player)) return null;

    ItemStack[] contents = createWindowContents(player);
    Inventory inv = getOpenWindow(player);
    if (inv != null && inv.getSize() < contents.length) {
      inv = null;
      closeWindow(player);
    }
    if (inv == null) {
      inv = openWindow(player, contents);
    } else {
      inv.setContents(contents);
    }
    return inv;
  }

  /**
   * If the given player currently has the window open, refresh its contents and return the updated
   * inventory. The window will be closed and reopened if it is too small to hold the current
   * contents.
   *
   * <p>If the window is open but should be closed, close it and return null.
   *
   * <p>If the player does not have the window open, return null.
   */
  private @Nullable Inventory refreshWindow(MatchPlayer player) {
    if (!checkWindow(player)) return null;

    Inventory inv = getOpenWindow(player);
    if (inv != null) {
      ItemStack[] contents = createWindowContents(player);
      if (inv.getSize() < contents.length) {
        closeWindow(player);
        inv = openWindow(player, contents);
      } else {
        inv.setContents(contents);
      }
    }
    return inv;
  }

  /**
   * Return true if the given player is currently allowed to have an open window. If they are not
   * allowed, close any window they have open, and return false.
   */
  private boolean checkWindow(MatchPlayer player) {
    if (!player.getBukkit().isOnline()) return false;

    if (!canOpenWindow(player)) {
      closeWindow(player);
      return false;
    }

    return true;
  }

  /**
   * Return the inventory of the given player's currently open window, or null if the player does
   * not have the window open.
   */
  private @Nullable Inventory getOpenWindow(MatchPlayer player) {
    if (picking.contains(player)) {
      return player.getBukkit().getOpenInventory().getTopInventory();
    }
    return null;
  }

  /** Close any window that is currently open for the given player */
  private void closeWindow(MatchPlayer player) {
    if (picking.contains(player)) {
      player.getBukkit().closeInventory();
    }
  }

  /** Open a new window for the given player displaying the given contents */
  private Inventory openWindow(MatchPlayer player, ItemStack[] contents) {
    closeWindow(player);
    Inventory inv =
        PGM.get()
            .getServer()
            .createInventory(
                player.getBukkit(),
                contents.length,
                StringUtils.truncate(getWindowTitle(player), 32));
    inv.setContents(contents);
    player.getBukkit().openInventory(inv);
    picking.add(player);
    return inv;
  }

  /** Generate current picker contents for the given player */
  private ItemStack[] createWindowContents(final MatchPlayer player) {
    final List<ItemStack> slots = new ArrayList<>();

    final Set<Team> teams = getChoosableTeams(player);
    if (!teams.isEmpty()) {
      // Auto-join button at start of row
      if (teams.size() > 1 && canAutoJoin(player)) {
        slots.add(createAutoJoinButton(player));
      }

      // Team buttons
      if (teams.size() > 1 || !hasJoined(player)) {
        for (Team team : teams) {
          slots.add(createTeamJoinButton(player, team));
        }
      }
    }

    // Skip to next empty row
    while (slots.size() % WIDTH != 0) slots.add(null);

    // Class buttons
    if (hasClasses) {
      for (PlayerClass cls : match.getModule(ClassMatchModule.class).getClasses()) {
        slots.add(createClassButton(player, cls));
      }
    }

    // Pad last row to width
    while (slots.size() % WIDTH != 0) slots.add(null);

    if (hasJoined(player)) {
      // Put leave button in first empty slot of the last column
      for (int slot = WIDTH - 1; ; slot += WIDTH) {
        while (slots.size() <= slot) {
          slots.add(null);
        }

        if (slots.get(slot) == null) {
          slots.set(slot, createLeaveButton(player));
          break;
        }
      }
    }

    return slots.toArray(new ItemStack[slots.size()]);
  }

  private ItemStack createClassButton(MatchPlayer viewer, PlayerClass cls) {
    ItemStack item = cls.getIcon().toItemStack(1);
    ItemMeta meta = item.getItemMeta();

    meta.setDisplayName(
        (cls.canUse(viewer.getBukkit()) ? ChatColor.GREEN : ChatColor.RED) + cls.getName());
    if (match.getModule(ClassMatchModule.class).getSelectedClass(viewer.getId()).equals(cls)) {
      meta.addEnchant(Enchantment.ARROW_INFINITE, 1, true);
    }

    List<String> lore = Lists.newArrayList();
    if (cls.getLongDescription() != null) {
      ComponentUtils.wordWrap(ChatColor.GOLD + cls.getLongDescription(), LORE_WIDTH_PIXELS, lore);
    } else if (cls.getDescription() != null) {
      lore.add(ChatColor.GOLD + cls.getDescription());
    }

    if (!cls.canUse(viewer.getBukkit())) {
      lore.add(
          ChatColor.RED
              + AllTranslations.get().translate("class.picker.restricted", viewer.getBukkit()));
    }

    meta.setLore(lore);
    item.setItemMeta(meta);

    return item;
  }

  private ItemStack createAutoJoinButton(MatchPlayer viewer) {
    ItemStack autojoin = new ItemStack(Button.AUTO_JOIN.material);

    ItemMeta autojoinMeta = autojoin.getItemMeta();
    autojoinMeta.setDisplayName(
        ChatColor.GRAY.toString()
            + ChatColor.BOLD
            + AllTranslations.get()
                .translate("teamSelection.picker.autoJoin.displayName", viewer.getBukkit()));
    autojoinMeta.setLore(
        Lists.newArrayList(
            this.getTeamSizeDescription(
                viewer.getMatch().getParticipants().size(), viewer.getMatch().getMaxPlayers()),
            ChatColor.AQUA
                + AllTranslations.get()
                    .translate("teamSelection.picker.autoJoin.tooltip", viewer.getBukkit())));
    autojoin.setItemMeta(autojoinMeta);

    return autojoin;
  }

  private ItemStack createTeamJoinButton(final MatchPlayer player, final Team team) {
    JoinMatchModule jmm = match.needModule(JoinMatchModule.class);

    ItemStack item = new ItemStack(Button.TEAM_JOIN.material);
    String capacityMessage =
        this.getTeamSizeDescription(team.getPlayers().size(), team.getMaxPlayers());
    List<String> lore = Lists.newArrayList(capacityMessage);

    JoinResult result = jmm.queryJoin(player, team);
    if (result instanceof GenericJoinResult) {
      switch (((GenericJoinResult) result).getStatus()) {
        default:
          lore.add(
              ChatColor.GREEN
                  + AllTranslations.get()
                      .translate("teamSelection.picker.clickToJoin", player.getBukkit()));
          break;

        case REJOINED:
          lore.add(
              ChatColor.GREEN
                  + AllTranslations.get()
                      .translate("teamSelection.picker.clickToRejoin", player.getBukkit()));
          break;

        case CHOICE_DENIED:
          lore.add(
              ChatColor.GOLD
                  + AllTranslations.get()
                      .translate("teamSelection.picker.noPermissions", player.getBukkit()));
          break;

        case FULL:
          lore.add(
              ChatColor.DARK_RED
                  + AllTranslations.get()
                      .translate("teamSelection.picker.capacity", player.getBukkit()));
          break;
      }
    }

    LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
    meta.setColor(team.getFullColor());
    meta.setDisplayName(team.getColor().toString() + ChatColor.BOLD + team.getName());
    meta.setLore(lore);
    item.setItemMeta(meta);

    return item;
  }

  private String getTeamSizeDescription(final int num, final int max) {
    return (num >= max ? ChatColor.RED : ChatColor.GREEN).toString()
        + num
        + ChatColor.GOLD
        + " / "
        + ChatColor.RED
        + max;
  }

  private void handleInventoryClick(
      final MatchPlayer player, final String name, final MaterialData material) {
    player.playSound(new Sound("random.click", 1, 2));

    if (hasClasses) {
      ClassMatchModule cmm = player.getMatch().needModule(ClassMatchModule.class);
      PlayerClass cls = cmm.getPlayerClass(name);

      if (cls != null && cls.getIcon().equals(material)) {
        if (!cls.canUse(player.getBukkit())) return;

        if (cls != cmm.getSelectedClass(player.getId())) {
          if (cmm.getCanChangeClass(player.getId())) {
            cmm.setPlayerClass(player.getId(), cls);
            player.sendMessage(
                ChatColor.GOLD
                    + AllTranslations.get()
                        .translate(
                            "command.class.select.confirm",
                            player.getBukkit(),
                            ChatColor.GREEN + name));
            scheduleRefresh(player);
          } else {
            player.sendMessage(
                ChatColor.RED
                    + AllTranslations.get()
                        .translate("command.class.stickyClass", player.getBukkit()));
          }
        }

        if (!canChooseMultipleTeams(player) && !hasJoined(player)) {
          this.scheduleClose(player);
          this.scheduleJoin(player, null);
        }

        return;
      }
    }

    if (hasTeams && Button.TEAM_JOIN.matches(material)) {
      Team team = player.getMatch().needModule(TeamMatchModule.class).bestFuzzyMatch(name, 1);
      if (team != null) {
        this.scheduleClose(player);
        this.scheduleJoin(player, team);
      }

    } else if (Button.AUTO_JOIN.matches(material)) {
      this.scheduleClose(player);
      this.scheduleJoin(player, null);

    } else if (Button.LEAVE.matches(material)) {
      this.scheduleClose(player);
      this.scheduleLeave(player);
    }
  }

  private void scheduleClose(final MatchPlayer player) {
    final Player bukkit = player.getBukkit();

    player
        .getMatch()
        .getScheduler(MatchScope.LOADED)
        .runTask(
            new Runnable() {
              @Override
              public void run() {
                if (bukkit.isOnline()) {
                  bukkit.getOpenInventory().getTopInventory().clear();
                  bukkit.closeInventory();
                }
              }
            });
  }

  private void scheduleJoin(final MatchPlayer player, @Nullable final Team team) {
    if (team == player.getParty() || (team == null && hasJoined(player))) return;

    final Player bukkit = player.getBukkit();
    player
        .getMatch()
        .getScheduler(MatchScope.LOADED)
        .runTask(
            new Runnable() {
              @Override
              public void run() {
                if (bukkit.isOnline()) {
                  match.needModule(JoinMatchModule.class).join(player, team);
                }
              }
            });
  }

  private void scheduleLeave(final MatchPlayer player) {
    if (!hasJoined(player)) return;

    final Player bukkit = player.getBukkit();
    player
        .getMatch()
        .getScheduler(MatchScope.LOADED)
        .runTask(
            new Runnable() {
              @Override
              public void run() {
                if (bukkit.isOnline()) {
                  match.needModule(JoinMatchModule.class).leave(player);
                }
              }
            });
  }

  private void scheduleRefresh(final MatchPlayer viewer) {
    viewer
        .getMatch()
        .getScheduler(MatchScope.LOADED)
        .runTask(
            new Runnable() {
              @Override
              public void run() {
                refreshWindow(viewer);
              }
            });
  }
}
