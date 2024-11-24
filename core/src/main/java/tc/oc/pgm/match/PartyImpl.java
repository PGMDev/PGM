package tc.oc.pgm.match;

import static net.kyori.adventure.text.Component.text;
import static tc.oc.pgm.util.Assert.assertNotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.party.event.PartyRenameEvent;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.filters.query.PartyQuery;
import tc.oc.pgm.util.Audience;
import tc.oc.pgm.util.bukkit.BukkitUtils;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.text.TextFormatter;

public abstract class PartyImpl implements Party, Audience {

  private final Match match;
  private final Map<UUID, MatchPlayer> memberMap;
  private final Collection<MatchPlayer> memberList;
  private final PartyQuery query;
  private final Audience audience;
  private final String id;
  private final ChatColor chatColor;
  private final Color color;
  private final DyeColor dyeColor;
  private final NamedTextColor textColor;
  private String legacyName;
  private Component name;
  private Component prefix;
  private boolean plural;

  public PartyImpl(
      final Match match,
      final String name,
      final ChatColor chatColor,
      final @Nullable DyeColor dyeColor) {
    this.match = assertNotNull(match);
    this.query = new PartyQuery(null, this);
    this.memberMap = new HashMap<>();
    this.memberList = Collections.unmodifiableCollection(this.memberMap.values());
    this.audience = Audience.get(this.memberMap.values());
    this.id = assertNotNull(name);
    this.chatColor = chatColor == null ? ChatColor.WHITE : chatColor;
    this.color = BukkitUtils.colorOf(this.chatColor);
    this.dyeColor = dyeColor == null ? BukkitUtils.chatColorToDyeColor(this.chatColor) : dyeColor;
    this.textColor = TextFormatter.convert(this.chatColor);
    this.setName(name);
  }

  @Override
  public Match getMatch() {
    return this.match;
  }

  @Override
  public PartyQuery getQuery() {
    return this.query;
  }

  @Override
  public Audience audience() {
    return this.audience;
  }

  @Override
  public Collection<MatchPlayer> getPlayers() {
    return this.memberList;
  }

  @Override
  public void addPlayer(final MatchPlayer player) {
    this.memberMap.put(assertNotNull(player).getId(), player);
  }

  @Override
  public void removePlayer(final UUID playerId) {
    this.memberMap.remove(playerId);
  }

  @Override
  public @Nullable MatchPlayer getPlayer(final UUID playerId) {
    return this.memberMap.get(playerId);
  }

  @Override
  public void setName(String name) {
    // If name is null, restore to the original name
    if (name == null) {
      name = this.id;
    }

    final String oldName = this.legacyName;
    final TextColor color = TextFormatter.convert(this.chatColor);

    this.legacyName = name;
    this.name = text(name, color);
    if (name.equalsIgnoreCase("Observers")) {
      this.prefix = text("(Obs) ", color);
    } else {
      this.prefix = text("(" + name + ") ", color);
    }
    this.plural = name.endsWith("s");

    // Prevent calling rename in the constructor
    if (oldName != null) {
      match.callEvent(new PartyRenameEvent(this, oldName, name));
    }
  }

  @Override
  public Component getName() {
    return this.name;
  }

  @Override
  public String getNameLegacy() {
    return this.legacyName;
  }

  @Override
  public Component getName(final NameStyle style) {
    return this.name;
  }

  @Override
  public String getDefaultName() {
    return this.id;
  }

  @Override
  public boolean isNamePlural() {
    return this.plural;
  }

  @Override
  public ChatColor getColor() {
    return this.chatColor;
  }

  @Override
  public Color getFullColor() {
    return this.color;
  }

  @Override
  public DyeColor getDyeColor() {
    return this.dyeColor;
  }

  @Override
  public NamedTextColor getTextColor() {
    return this.textColor;
  }

  @Override
  public Component getChatPrefix() {
    return this.prefix;
  }

  @Override
  public boolean isAutomatic() {
    return false;
  }
}
