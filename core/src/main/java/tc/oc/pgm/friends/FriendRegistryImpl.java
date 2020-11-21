package tc.oc.pgm.friends;

import javax.annotation.Nullable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.util.friends.FriendProvider;

public class FriendRegistryImpl implements FriendRegistry {

  private final MetadataValue METADATA_VALUE = new FixedMetadataValue(PGM.get(), this);

  private FriendProvider provider;

  public FriendRegistryImpl(@Nullable FriendProvider provider) {
    setProvider(provider);
  }

  @EventHandler(priority = EventPriority.LOW)
  public void onPlayerJoin(PlayerJoinEvent e) {
    e.getPlayer().setMetadata(METADATA_KEY, METADATA_VALUE);
  }

  @Override
  public FriendProvider getProvider() {
    return provider;
  }

  @Override
  public void setProvider(FriendProvider provider) {
    this.provider = provider == null ? FriendProvider.DEFAULT : provider;
  }
}
