package tc.oc.pgm.picker;

import static net.kyori.adventure.key.Key.key;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;
import static tc.oc.pgm.util.Assert.assertTrue;
import static tc.oc.pgm.util.bukkit.InventoryViewUtil.INVENTORY_VIEW;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.match.event.MatchFinishEvent;
import tc.oc.pgm.api.match.event.MatchStartEvent;
import tc.oc.pgm.api.match.factory.MatchModuleFactory;
import tc.oc.pgm.api.module.exception.ModuleLoadException;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.event.ObserverInteractEvent;
import tc.oc.pgm.api.player.event.PlayerVanishEvent;
import tc.oc.pgm.api.setting.SettingKey;
import tc.oc.pgm.blitz.BlitzMatchModule;
import tc.oc.pgm.classes.ClassMatchModule;
import tc.oc.pgm.classes.PlayerClass;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.events.PlayerJoinMatchEvent;
import tc.oc.pgm.events.PlayerPartyChangeEvent;
import tc.oc.pgm.join.JoinMatchModule;
import tc.oc.pgm.join.JoinRequest;
import tc.oc.pgm.join.JoinResult;
import tc.oc.pgm.join.JoinResultOption;
import tc.oc.pgm.match.ObserverParty;
import tc.oc.pgm.spawns.events.DeathKitApplyEvent;
import tc.oc.pgm.spawns.events.ObserverKitApplyEvent;
import tc.oc.pgm.teams.Team;
import tc.oc.pgm.teams.TeamMatchModule;
import tc.oc.pgm.util.LegacyFormatUtils;
import tc.oc.pgm.util.StringUtils;
import tc.oc.pgm.util.bukkit.Enchantments;
import tc.oc.pgm.util.bukkit.Sounds;
import tc.oc.pgm.util.event.player.PlayerLocaleChangeEvent;
import tc.oc.pgm.util.inventory.InventoryUtils;
import tc.oc.pgm.util.material.MaterialData;
import tc.oc.pgm.util.text.TextTranslations;

@ListenerScope(MatchScope.LOADED)
public class PickerMatchModule implements MatchModule, Listener {

  public static class Factory implements MatchModuleFactory<PickerMatchModule> {
    @Override
    public Collection<Class<? extends MatchModule>> getWeakDependencies() {
      return ImmutableList.of(
          TeamMatchModule.class, ClassMatchModule.class, BlitzMatchModule.class);
    }

    @Override
    public Collection<Class<? extends MatchModule>> getHardDependencies() {
      return ImmutableList.of(JoinMatchModule.class);
    }

    @Override
    public PickerMatchModule createMatchModule(Match match) throws ModuleLoadException {
      return new PickerMatchModule(match);
    }
  }

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

    public boolean matches(ItemStack item) {
      return this.material.equals(item.getType());
    }
  }

  private final Match match;
  private final Set<MatchPlayer> picking = new HashSet<>();
  private final boolean hasTeams;
  private final boolean hasClasses;
  private final boolean isBlitz;

  private PickerMatchModule(Match match) {
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
      case PICKER_MANUAL: // Only show the GUI when right clicked
        return playerTriggered;
      default: // Display after map cycle, but check perms when clicking button.
        return !playerTriggered || hasPermission || hasClasses;
    }
  }

  private boolean hasJoined(MatchPlayer joining) {
    return joining.isParticipating()
        || match.needModule(JoinMatchModule.class).isQueuedToJoin(joining);
  }

  private boolean canAutoJoin(MatchPlayer joining) {
    JoinResult result = match
        .needModule(JoinMatchModule.class)
        .queryJoin(joining, JoinRequest.fromPlayer(joining, null));
    return result.isSuccess() || result.getOption() == JoinResultOption.FULL;
  }

  private boolean canChooseMultipleTeams(MatchPlayer joining) {
    return getChoosableTeams(joining).size() > 1;
  }

  private Set<Team> getChoosableTeams(MatchPlayer joining) {
    TeamMatchModule tmm = match.getModule(TeamMatchModule.class);
    if (tmm == null) return Collections.emptySet();

    Set<Team> teams = new HashSet<>();
    for (Team team : tmm.getTeams()) {
      JoinResult result = tmm.queryJoin(joining, JoinRequest.fromPlayer(joining, team));
      // We still want to show the button if the team is full
      // or the player doesn't have join perms.
      if (result.isSuccess()
          || result.getOption() == JoinResultOption.FULL
          || result.getOption() == JoinResultOption.CHOICE_DENIED) {
        teams.add(team);
      }
    }

    return teams;
  }

  /** Does the player have any use for the picker? */
  private boolean canUse(MatchPlayer player) {
    if (player == null) return false;

    // Player is eliminated from Blitz
    if (isBlitz && !match.needModule(BlitzMatchModule.class).canJoin(player, null)) return false;

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

  private void refreshCountsAll() {
    for (MatchPlayer player : ImmutableSet.copyOf(picking)) {
      refreshWindow(player);
    }
  }

  private String getWindowTitle(MatchPlayer player) {
    assertTrue(hasTeams || hasClasses); // Window should not open if there is nothing to pick

    String key;
    if (hasTeams && hasClasses) {
      key = "picker.windowTitle.teamClass";
    } else if (hasTeams) {
      key = "picker.windowTitle.team";
    } else {
      key = "picker.windowTitle.class";
    }

    return ChatColor.DARK_RED + TextTranslations.translate(key, player.getBukkit());
  }

  private ItemStack createJoinButton(final MatchPlayer player) {
    ItemStack stack = new ItemStack(Button.JOIN.material);

    ItemMeta meta = stack.getItemMeta();

    String key;
    if (!canOpenWindow(player)) {
      key = "picker.title.ffa";
    } else if (hasTeams && hasClasses) {
      key = "picker.title.teamClass";
    } else if (hasTeams) {
      key = "picker.title.team";
    } else if (hasClasses) {
      key = "picker.title.class";
    } else {
      key = "picker.title.ffa";
    }

    meta.setDisplayName(OPEN_BUTTON_PREFIX + TextTranslations.translate(key, player.getBukkit()));
    meta.setLore(Lists.newArrayList(
        ChatColor.DARK_PURPLE + TextTranslations.translate("picker.tooltip", player.getBukkit())));

    // Color the leather helmet to match player team
    if (player != null
        && player.getParty() != null
        && !(player.getParty() instanceof ObserverParty)) {
      LeatherArmorMeta armorMeta = (LeatherArmorMeta) meta;
      armorMeta.setColor(player.getParty().getFullColor());
      meta = armorMeta;
    }

    stack.setItemMeta(meta);
    return stack;
  }

  private ItemStack createLeaveButton(final MatchPlayer player) {
    ItemStack stack = new ItemStack(Button.LEAVE.material);

    ItemMeta meta = stack.getItemMeta();
    meta.setDisplayName(
        OPEN_BUTTON_PREFIX + TextTranslations.translate("picker.title.leave", player.getBukkit()));
    meta.setLore(Lists.newArrayList(ChatColor.DARK_PURPLE
        + TextTranslations.translate("picker.tooltipObserver", player.getBukkit())));

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
      match.getExecutor(MatchScope.LOADED).execute(() -> {
        if (player.getBukkit().isOnline()) {
          showWindow(player);
        }
      });
    }
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void cancelArmorEquip(ObserverInteractEvent event) {
    if (event.getClickType() == ClickType.RIGHT
        && event.getClickedItem() != null
        && event.getClickedItem().getType() == Button.TEAM_JOIN.material) {
      event.setCancelled(true);
      event.getPlayer().getBukkit().updateInventory();
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
          event.getCurrentItem());
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

  @EventHandler
  public void rightClickIcon(final ObserverInteractEvent event) {
    final boolean right = event.getClickType() == ClickType.RIGHT;
    final boolean left = event.getClickType() == ClickType.LEFT;
    if ((!right && !left) || InventoryUtils.isNothing(event.getClickedItem())) return;

    final ItemStack hand = event.getClickedItem();
    String displayName = hand.getItemMeta().getDisplayName();
    if (displayName == null) return;

    final MatchPlayer player = event.getPlayer();
    if (!canUse(player)) return;

    boolean handled = false;
    final JoinMatchModule jmm = match.needModule(JoinMatchModule.class);

    if (hand.getType() == Button.JOIN.material) {
      handled = true;
      if (right && canOpenWindow(player) && settingEnabled(player, true)) {
        showWindow(player);
      } else {
        // If there is nothing to pick or setting is disabled, just join immediately
        jmm.join(player, JoinRequest.fromPlayer(player, null));
      }
    } else if (hand.getType() == Button.LEAVE.material && left) {
      jmm.leave(player, JoinRequest.empty());
    }

    if (handled) {
      event.setCancelled(true);

      // Unfortunately, not opening the window seems to cause the player to put the helmet
      // on their head, no matter what we do server-side. So, we have to force an inventory
      // update to resync them.
      player.getBukkit().updateInventory();
    }
  }

  @EventHandler
  public void giveKitToObservers(final ObserverKitApplyEvent event) {
    refreshKit(event.getPlayer());
  }

  @EventHandler
  public void playerVanishRefresh(final PlayerVanishEvent event) {
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
      return INVENTORY_VIEW.getTopInventory(player.getBukkit().getOpenInventory());
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
    Inventory inv = PGM.get()
        .getServer()
        .createInventory(
            player.getBukkit(), contents.length, StringUtils.truncate(getWindowTitle(player), 32));
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
      meta.addEnchant(Enchantments.INFINITY, 1, true);
    }

    List<String> lore = Lists.newArrayList();
    if (cls.getLongDescription() != null) {
      LegacyFormatUtils.wordWrap(
          ChatColor.GOLD + cls.getLongDescription(), LORE_WIDTH_PIXELS, lore);
    } else if (cls.getDescription() != null) {
      lore.add(ChatColor.GOLD + cls.getDescription());
    }

    if (!cls.canUse(viewer.getBukkit())) {
      lore.add(ChatColor.RED
          + TextTranslations.translate("class.picker.restricted", viewer.getBukkit()));
    }

    meta.setLore(lore);
    item.setItemMeta(meta);

    return item;
  }

  private ItemStack createAutoJoinButton(MatchPlayer viewer) {
    ItemStack autojoin = new ItemStack(Button.AUTO_JOIN.material);

    ItemMeta autojoinMeta = autojoin.getItemMeta();
    autojoinMeta.setDisplayName(ChatColor.GRAY.toString()
        + ChatColor.BOLD
        + TextTranslations.translate("picker.autoJoin.displayName", viewer.getBukkit()));
    autojoinMeta.setLore(Lists.newArrayList(
        this.getTeamSizeDescription(
            viewer.getMatch().getParticipants().size(), viewer.getMatch().getMaxPlayers()),
        ChatColor.AQUA
            + TextTranslations.translate("picker.autoJoin.tooltip", viewer.getBukkit())));
    autojoin.setItemMeta(autojoinMeta);

    return autojoin;
  }

  private ItemStack createTeamJoinButton(final MatchPlayer player, final Team team) {
    JoinMatchModule jmm = match.needModule(JoinMatchModule.class);

    ItemStack item = new ItemStack(Button.TEAM_JOIN.material);
    String capacityMessage =
        this.getTeamSizeDescription(team.getPlayers().size(), team.getMaxPlayers());
    List<String> lore = Lists.newArrayList(capacityMessage);

    JoinResult result = jmm.queryJoin(player, JoinRequest.fromPlayer(player, team));
    if (result instanceof JoinResultOption) {
      switch ((JoinResultOption) result) {
        default:
          lore.add(ChatColor.GREEN
              + TextTranslations.translate("picker.clickToJoin", player.getBukkit()));
          break;

        case REJOINED:
          lore.add(ChatColor.GREEN
              + TextTranslations.translate("picker.clickToRejoin", player.getBukkit()));
          break;

        case CHOICE_DENIED:
          lore.add(ChatColor.GOLD
              + TextTranslations.translate("picker.noPermissions", player.getBukkit()));
          break;

        case FULL:
          lore.add(ChatColor.DARK_RED
              + TextTranslations.translate("picker.capacity", player.getBukkit()));
          break;
      }
    }

    LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
    meta.setColor(team.getFullColor());
    meta.setDisplayName(team.getColor().toString() + ChatColor.BOLD + team.getNameLegacy());
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
      final MatchPlayer player, final String name, final ItemStack item) {
    player.playSound(Sounds.INVENTORY_CLICK);

    if (hasClasses) {
      ClassMatchModule cmm = player.getMatch().needModule(ClassMatchModule.class);
      PlayerClass cls = cmm.getPlayerClass(name);

      if (cls != null && cls.getIcon().equals(MaterialData.item(item))) {
        if (!cls.canUse(player.getBukkit())) return;

        if (cls != cmm.getSelectedClass(player.getId())) {
          if (cmm.getCanChangeClass(player.getId())) {
            cmm.setPlayerClass(player.getId(), cls);
            player.sendMessage(translatable(
                "match.class.ok", NamedTextColor.GOLD, text(name, NamedTextColor.GREEN)));
            scheduleRefresh(player);
          } else {
            player.sendMessage(translatable("match.class.sticky", NamedTextColor.RED));
          }
        }

        if (!canChooseMultipleTeams(player) && !hasJoined(player)) {
          this.scheduleClose(player);
          this.scheduleJoin(player, null);
        }

        return;
      }
    }

    if (hasTeams && Button.TEAM_JOIN.matches(item)) {
      Team team = player.getMatch().needModule(TeamMatchModule.class).getTeam(name);
      if (team != null) {
        this.scheduleClose(player);
        this.scheduleJoin(player, team);
      }

    } else if (Button.AUTO_JOIN.matches(item)) {
      this.scheduleClose(player);
      this.scheduleJoin(player, null);

    } else if (Button.LEAVE.matches(item)) {
      this.scheduleClose(player);
      this.scheduleLeave(player);
    }
  }

  private void scheduleClose(final MatchPlayer player) {
    final Player bukkit = player.getBukkit();

    match.getExecutor(MatchScope.LOADED).execute(() -> {
      if (bukkit.isOnline()) {
        INVENTORY_VIEW.getTopInventory(bukkit.getOpenInventory()).clear();
        bukkit.closeInventory();
      }
    });
  }

  private void scheduleJoin(final MatchPlayer player, @Nullable final Team team) {
    if (team == player.getParty() || (team == null && hasJoined(player))) return;

    final Player bukkit = player.getBukkit();
    match.getExecutor(MatchScope.LOADED).execute(() -> {
      if (bukkit.isOnline()) {
        match.needModule(JoinMatchModule.class).join(player, team);
      }
    });
  }

  private void scheduleLeave(final MatchPlayer player) {
    if (!hasJoined(player)) return;

    final Player bukkit = player.getBukkit();
    match.getExecutor(MatchScope.LOADED).execute(() -> {
      if (bukkit.isOnline()) {
        match.needModule(JoinMatchModule.class).leave(player, JoinRequest.empty());
      }
    });
  }

  private void scheduleRefresh(final MatchPlayer viewer) {
    match.getExecutor(MatchScope.LOADED).execute(() -> refreshWindow(viewer));
  }
}
