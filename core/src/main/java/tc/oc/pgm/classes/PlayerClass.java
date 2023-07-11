package tc.oc.pgm.classes;

import static tc.oc.pgm.util.Assert.assertNotNull;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import org.bukkit.material.MaterialData;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.filters.query.PlayerQuery;
import tc.oc.pgm.kits.Kit;

public class PlayerClass {
  private final String name;
  private final String familyName;
  private final @Nullable String description;
  private final @Nullable String longdescription;
  private final boolean sticky;
  private final Set<Kit> kits;
  private final MaterialData icon;
  private final boolean restrict;
  private final Filter filter;
  private final String denyMessage;

  public PlayerClass(
      String name,
      String familyName,
      @Nullable String description,
      @Nullable String longdescription,
      boolean sticky,
      Set<Kit> kits,
      MaterialData icon,
      boolean restrict,
      Filter filter,
      String denyMessage) {
    this.name = assertNotNull(name, "name");
    this.familyName = assertNotNull(familyName, "family name");
    this.description = description;
    this.longdescription = longdescription;
    this.sticky = sticky;
    this.kits = ImmutableSet.copyOf(assertNotNull(kits, "kits"));
    this.icon = assertNotNull(icon, "icon");
    this.restrict = restrict;
    this.filter = filter;
    this.denyMessage = denyMessage;
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

  public boolean canUse(MatchPlayer player) {
    if (player == null) return false;
    PlayerQuery playerQuery = new PlayerQuery(null, player);
    Filter.QueryResponse response = filter.query(playerQuery);
    return response.isAllowed();
    // Restricted classes are removed:
    // return response.isAllowed() && (!this.isRestricted() || player.getBukkit().isOp());
  }

  @Nullable
  public String getDenyMessage() {
    return denyMessage;
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
    result = prime * result + this.filter.hashCode();
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
    if (!this.filter.equals(other.filter)) return false;
    if (!this.denyMessage.equals(other.denyMessage)) return false;
    return true;
  }
}
