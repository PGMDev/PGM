package tc.oc.pgm.namedecorations;

import static net.kyori.adventure.text.Component.text;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.event.NameDecorationChangeEvent;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.events.PlayerJoinMatchEvent;
import tc.oc.pgm.events.PlayerPartyChangeEvent;
import tc.oc.pgm.util.named.NameDecorationProvider;
import tc.oc.pgm.util.player.PlayerComponent;
import tc.oc.pgm.util.text.TextFormatter;

@SuppressWarnings("UnstableApiUsage")
public class NameDecorationRegistryImpl implements NameDecorationRegistry, Listener {

  private final MetadataValue METADATA_VALUE = new FixedMetadataValue(PGM.get(), this);

  private NameDecorationProvider provider;
  private final LoadingCache<UUID, DecorationCacheEntry> decorationCache =
      CacheBuilder.newBuilder()
          .expireAfterAccess(15, TimeUnit.MINUTES)
          .build(
              new CacheLoader<UUID, DecorationCacheEntry>() {
                @Override
                public DecorationCacheEntry load(@NotNull UUID uuid) {
                  return new DecorationCacheEntry(uuid);
                }
              });

  public NameDecorationRegistryImpl(@Nullable NameDecorationProvider provider) {
    setProvider(provider);
  }

  @EventHandler(priority = EventPriority.LOW)
  public void onPlayerJoin(PlayerJoinEvent e) {
    e.getPlayer().setMetadata(METADATA_KEY, METADATA_VALUE);
  }

  @EventHandler
  public void onJoinMatch(PlayerJoinMatchEvent event) {
    Player player = event.getPlayer().getBukkit();
    Party party = event.getNewParty();
    player.setDisplayName(getDecoratedName(player, party == null ? null : party.getColor()));
  }

  @EventHandler
  public void onPartyChange(PlayerPartyChangeEvent event) {
    Player player = event.getPlayer().getBukkit();
    Party party = event.getNewParty();
    player.setDisplayName(getDecoratedName(player, party == null ? null : party.getColor()));
  }

  @EventHandler
  public void onNameDecorationChange(NameDecorationChangeEvent event) {
    if (event.getUUID() == null) return;
    decorationCache.invalidate(event.getUUID());
    PlayerComponent.RENDERER.decorationChanged(event.getUUID());

    final Player player = Bukkit.getPlayer(event.getUUID());
    final MatchPlayer matchPlayer = PGM.get().getMatchManager().getPlayer(player);
    if (matchPlayer == null) return;

    matchPlayer
        .getBukkit()
        .setDisplayName(getDecoratedName(player, matchPlayer.getParty().getColor()));
  }

  @Override
  public String getDecoratedName(Player player, ChatColor partyColor) {
    return getPrefix(player.getUniqueId())
        + (partyColor == null ? ChatColor.RESET : partyColor)
        + player.getName()
        + getSuffix(player.getUniqueId())
        + ChatColor.WHITE;
  }

  @Override
  public Component getDecoratedNameComponent(Player player, ChatColor partyColor) {
    return text()
        .append(getPrefixComponent(player.getUniqueId()))
        .append(
            text(
                player.getName(),
                partyColor == null ? NamedTextColor.WHITE : TextFormatter.convert(partyColor)))
        .append(getSuffixComponent(player.getUniqueId()))
        .build();
  }

  public String getPrefix(UUID uuid) {
    return decorationCache.getUnchecked(uuid).prefix;
  }

  public String getSuffix(UUID uuid) {
    return decorationCache.getUnchecked(uuid).suffix;
  }

  public TextColor getColor(UUID uuid) {
    MatchPlayer player = PGM.get().getMatchManager().getPlayer(uuid);
    if (player == null) return PlayerComponent.OFFLINE_COLOR;
    return TextFormatter.convert(player.getParty().getColor());
  }

  public Component getPrefixComponent(UUID uuid) {
    return decorationCache.getUnchecked(uuid).prefixComponent;
  }

  public Component getSuffixComponent(UUID uuid) {
    return decorationCache.getUnchecked(uuid).suffixComponent;
  }

  @Override
  public void setProvider(@Nullable NameDecorationProvider provider) {
    this.provider = provider == null ? NameDecorationProvider.DEFAULT : provider;
    this.decorationCache.invalidateAll();
  }

  @Override
  @NotNull
  public NameDecorationProvider getProvider() {
    return provider;
  }

  private class DecorationCacheEntry {
    private final String prefix, suffix;
    private final Component prefixComponent, suffixComponent;

    DecorationCacheEntry(UUID uuid) {
      this.prefix = provider.getPrefix(uuid);
      this.suffix = provider.getSuffix(uuid);
      this.prefixComponent = provider.getPrefixComponent(uuid);
      this.suffixComponent = provider.getSuffixComponent(uuid);
    }
  }
}
