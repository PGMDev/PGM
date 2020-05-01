package tc.oc.pgm.classes;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import javax.annotation.Nullable;
import org.bukkit.entity.Player;
import org.bukkit.material.MaterialData;
import tc.oc.pgm.kits.Kit;

public class PlayerClass {
  public PlayerClass(
      String name,
      String familyName,
      @Nullable String description,
      @Nullable String longdescription,
      boolean sticky,
      Set<Kit> kits,
      MaterialData icon,
      boolean restrict) {
    this.name = checkNotNull(name, "name");
    this.familyName = checkNotNull(familyName, "family name");
    this.description = description;
    this.longdescription = longdescription;
    this.sticky = sticky;
    this.kits = ImmutableSet.copyOf(checkNotNull(kits, "kits"));
    this.icon = checkNotNull(icon, "icon");
    this.restrict = restrict;
  }

  public String getName() {
    return this.name;
  }

  public String getFamilyName() {
    return this.familyName;
  }

  public @Nullable String getDescription() {
    return this.description;
  }

  public @Nullable String getLongDescription() {
    return this.longdescription;
  }

  public boolean isSticky() {
    return this.sticky;
  }

  public Set<Kit> getKits() {
    return this.kits;
  }

  public MaterialData getIcon() {
    return this.icon;
  }

  public boolean isRestricted() {
    return this.restrict;
  }

  public boolean canUse(Player player) {
    return true; // Restricted classes are removed: !this.isRestricted() || player.isOp();
  }

  @Override
  public String toString() {
    return this.name;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + this.name.hashCode();
    result = prime * result + this.familyName.hashCode();
    result = prime * result + ((this.description == null) ? 0 : this.description.hashCode());
    result = prime * result + (this.sticky ? 1231 : 1237);
    result = prime * result + this.kits.hashCode();
    result = prime * result + this.icon.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (!(obj instanceof PlayerClass)) return false;
    PlayerClass other = (PlayerClass) obj;
    if (!this.name.equals(other.name)) return false;
    if (!this.familyName.equals(other.familyName)) return false;
    if (this.description == null) {
      if (other.description != null) return false;
    } else if (!this.description.equals(other.description)) return false;
    if (this.longdescription == null) {
      if (other.longdescription != null) return false;
    } else if (!this.longdescription.equals(other.longdescription)) return false;
    if (this.sticky != other.sticky) return false;
    if (!this.kits.equals(other.kits)) return false;
    if (!this.icon.equals(other.icon)) return false;
    return true;
  }

  final String name;
  final String familyName;
  final @Nullable String description;
  final @Nullable String longdescription;
  final boolean sticky;
  final Set<Kit> kits;
  final MaterialData icon;
  final boolean restrict;
}
