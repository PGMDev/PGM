package tc.oc.pgm.namedecorations;

import static net.kyori.adventure.text.Component.text;

import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import tc.oc.pgm.api.Config;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.named.NameDecorationProvider;

/**
 * A simple, config-based decoration provider, that will assign prefixes and suffixes based on
 * player permissions
 */
public class ConfigDecorationProvider implements NameDecorationProvider {

  @Override
  public String getPrefix(UUID uuid) {
    return groups(uuid)
        .filter(g -> g.getPrefix() != null)
        .map(Config.Group::getPrefix)
        .collect(Collectors.joining());
  }

  @Override
  public String getSuffix(UUID uuid) {
    return groups(uuid)
        .filter(g -> g.getSuffix() != null)
        .map(Config.Group::getSuffix)
        .collect(Collectors.joining());
  }

  @Override
  public Component getPrefixComponent(UUID uuid) {
    return generateFlair(groups(uuid), true);
  }

  @Override
  public Component getSuffixComponent(UUID uuid) {
    return generateFlair(groups(uuid), false);
  }

  private Component generateFlair(Stream<? extends Config.Group> flairs, boolean prefix) {
    TextComponent.Builder builder = text();
    flairs
        .filter(p -> prefix ? p.getPrefix() != null : p.getSuffix() != null)
        .map(Config.Group::getFlair)
        .forEach(flair -> builder.append(flair.getComponent(prefix)));
    return builder.build();
  }

  private Stream<? extends Config.Group> groups(UUID uuid) {
    final Player player = Bukkit.getPlayer(uuid);
    if (player == null) return Stream.empty();

    return PGM.get().getConfiguration().getGroups().stream()
        .filter(g -> player.hasPermission(g.getPermission()));
  }
}
