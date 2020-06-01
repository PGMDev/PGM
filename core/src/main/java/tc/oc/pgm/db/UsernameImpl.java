package tc.oc.pgm.db;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.UUID;
import javax.annotation.Nullable;
import net.kyori.text.Component;
import net.kyori.text.TranslatableComponent;
import net.kyori.text.format.TextColor;
import net.kyori.text.format.TextDecoration;
import org.bukkit.Bukkit;
import tc.oc.pgm.api.player.Username;
import tc.oc.pgm.util.UsernameResolver;
import tc.oc.pgm.util.component.types.PersonalizedPlayer;
import tc.oc.pgm.util.named.NameStyle;

class UsernameImpl implements Username {

  private final UUID id;
  private String name;

  UsernameImpl(UUID id, @Nullable String name) {
    this.id = checkNotNull(id, "username id is null");
    setName(name);
  }

  @Override
  public final UUID getId() {
    return id;
  }

  @Nullable
  @Override
  public String getName() {
    return name;
  }

  @Override
  public Component getName(NameStyle style) {
    return name == null
        ? TranslatableComponent.of("misc.unknown", TextColor.DARK_AQUA, TextDecoration.ITALIC)
        : new PersonalizedPlayer(Bukkit.getPlayer(id), name, style).render();
  }

  @Override
  public void setName(@Nullable String name) {
    if (name == null) {
      UsernameResolver.resolve(id, this::setName);
    } else {
      this.name = name;
    }
  }

  @Override
  public int hashCode() {
    return id.hashCode();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Username)) return false;
    return getId().equals(((Username) o).getId());
  }

  @Override
  public String toString() {
    return name == null ? id.toString() : name;
  }
}
