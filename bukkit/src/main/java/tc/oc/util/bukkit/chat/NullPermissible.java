package tc.oc.util.bukkit.chat;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import org.bukkit.permissions.Permissible;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.Plugin;

/**
 * {@link Permissible} that never has any permissions. Mutating methods can be called, but will have
 * no effect. The {@link #addAttachment} methods will create and return a {@link
 * PermissionAttachment}, with the given permission set, but it will not actually be attached to
 * anything.
 */
public class NullPermissible implements Permissible {

  public static final NullPermissible INSTANCE = new NullPermissible();

  @Override
  public boolean isOp() {
    return false;
  }

  @Override
  public void setOp(boolean value) {}

  @Override
  public boolean isPermissionSet(String name) {
    return false;
  }

  @Override
  public boolean isPermissionSet(Permission perm) {
    return false;
  }

  @Override
  public boolean hasPermission(String inName) {
    return false;
  }

  @Override
  public boolean hasPermission(Permission perm) {
    return false;
  }

  @Override
  public PermissionAttachment addAttachment(Plugin plugin) {
    return new PermissionAttachment(plugin, this);
  }

  @Override
  public PermissionAttachment addAttachment(Plugin plugin, String name, boolean value) {
    PermissionAttachment attachment = addAttachment(plugin);
    attachment.setPermission(name, value);
    return attachment;
  }

  @Override
  public PermissionAttachment addAttachment(Plugin plugin, int i) {
    return addAttachment(plugin);
  }

  @Override
  public PermissionAttachment addAttachment(Plugin plugin, String name, boolean value, int ticks) {
    return addAttachment(plugin, name, value);
  }

  @Override
  public void removeAttachment(PermissionAttachment attachment) {}

  @Override
  public void recalculatePermissions() {}

  @Override
  public Set<PermissionAttachmentInfo> getEffectivePermissions() {
    return Collections.emptySet();
  }

  @Override
  public boolean removeAttachments(Plugin plugin) {
    return false;
  }

  @Override
  public boolean removeAttachments(String name) {
    return false;
  }

  @Override
  public boolean removeAttachments(Permission permission) {
    return false;
  }

  @Override
  public boolean removeAttachments(Plugin plugin, String name) {
    return false;
  }

  @Override
  public boolean removeAttachments(Plugin plugin, Permission permission) {
    return false;
  }

  @Override
  public PermissionAttachmentInfo getEffectivePermission(String name) {
    return null;
  }

  @Override
  public Collection<PermissionAttachmentInfo> getAttachments() {
    return Collections.emptyList();
  }

  @Override
  public Collection<PermissionAttachmentInfo> getAttachments(Plugin plugin) {
    return Collections.emptyList();
  }

  @Override
  public Collection<PermissionAttachmentInfo> getAttachments(String name) {
    return Collections.emptyList();
  }

  @Override
  public Collection<PermissionAttachmentInfo> getAttachments(Permission permission) {
    return Collections.emptyList();
  }

  @Override
  public Collection<PermissionAttachmentInfo> getAttachments(Plugin plugin, String name) {
    return Collections.emptyList();
  }

  @Override
  public Collection<PermissionAttachmentInfo> getAttachments(Plugin plugin, Permission permission) {
    return Collections.emptyList();
  }
}
