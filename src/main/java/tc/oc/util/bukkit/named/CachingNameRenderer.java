package tc.oc.util.bukkit.named;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import javax.annotation.Nullable;
import net.md_5.bungee.api.ChatColor;
import tc.oc.util.bukkit.component.Component;
import tc.oc.util.bukkit.identity.Identity;

/**
 * Caches rendered names, both component and legacy. The cache is keyed on the {@link Identity} and
 * {@link NameType} that the name is generated from.
 */
public class CachingNameRenderer implements NameRenderer {

  private final NameRenderer nameRenderer;
  private final Table<Identity, NameType, Component> components = HashBasedTable.create();
  private final Table<Identity, NameType, String> legacy = HashBasedTable.create();

  public CachingNameRenderer(NameRenderer nameRenderer) {
    this.nameRenderer = nameRenderer;
  }

  @Override
  public ChatColor getColor(Identity identity, NameType type) {
    return nameRenderer.getColor(identity, type);
  }

  @Override
  public String getLegacyName(Identity identity, NameType type) {
    String rendered = legacy.get(identity, type);
    if (rendered == null) {
      rendered = nameRenderer.getLegacyName(identity, type);
      legacy.put(identity, type, rendered);
    }
    return rendered;
  }

  @Override
  public Component getComponentName(Identity identity, NameType type) {
    Component rendered = components.get(identity, type);
    if (rendered == null) {
      rendered = nameRenderer.getComponentName(identity, type);
      components.put(identity, type, rendered);
    }
    return rendered;
  }

  @Override
  public void invalidateCache(@Nullable Identity identity) {
    if (identity == null) {
      components.clear();
      legacy.clear();
    } else {
      components.rowKeySet().remove(identity);
      legacy.rowKeySet().remove(identity);
    }
  }
}
