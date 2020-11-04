package tc.oc.pgm.core;

import static net.kyori.adventure.text.Component.text;

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;

public class CoreConvertMonitor implements Runnable {
  private final CoreMatchModule parent;
  private Material nextMaterial;

  public CoreConvertMonitor(CoreMatchModule parent) {
    this.parent = parent;
    this.nextMaterial = getNext(parent.cores.iterator().next().getMaterial().getItemType());
  }

  @Override
  @SuppressWarnings("deprecation")
  public void run() {
    if (this.nextMaterial != null) {
      for (Core core : this.parent.cores) {
        core.replaceBlocks(this.nextMaterial.getNewData((byte) 0));
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
      this.nextMaterial = getNext(this.nextMaterial);
    }
  }

  public static Material getNext(Material old) {
    switch (old) {
      case OBSIDIAN:
        return Material.GOLD_BLOCK;
      case GOLD_BLOCK:
        return Material.GLASS;
      default:
        return null;
    }
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
