package tc.oc.pgm.core;

import static net.kyori.adventure.text.Component.text;

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import tc.oc.pgm.util.material.MaterialData;

public class CoreConvertMonitor implements Runnable {
  private final CoreMatchModule parent;
  private Material nextMaterial;

  public CoreConvertMonitor(CoreMatchModule parent) {
    this.parent = parent;
    this.nextMaterial = getNext(parent.cores.iterator().next());
  }

  @Override
  public void run() {
    if (this.nextMaterial != null) {
      for (Core core : this.parent.cores) {
        core.replaceBlocks(MaterialData.from(this.nextMaterial));
      }
      String name = getName(this.nextMaterial);
      if (name == null) {
        name = this.nextMaterial.toString();
      }
      this.parent.match.sendMessage(
          text()
              .append(text("> > > > ", NamedTextColor.DARK_AQUA))
              .append(text(name + " CORE MODE", NamedTextColor.RED))
              .append(text(" < < < <", NamedTextColor.DARK_AQUA))
              .build());
      this.nextMaterial = getNext(parent.cores.iterator().next());
    }
  }

  public static Material getNext(Core core) {
    if (core.isCoreMaterial(MaterialData.from(Material.OBSIDIAN))) return Material.GOLD_BLOCK;
    if (core.isCoreMaterial(MaterialData.from(Material.GOLD_BLOCK))) return Material.GLASS;
    return null;
  }

  public static String getName(Material material) {
    switch (material) {
      case GOLD_BLOCK:
        return "GOLD";
      default:
        return null;
    }
  }
}
