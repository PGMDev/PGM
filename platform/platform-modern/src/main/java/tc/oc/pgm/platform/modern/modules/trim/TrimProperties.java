package tc.oc.pgm.platform.modern.modules.trim;

import java.util.Map;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record TrimProperties(Map<EquipmentSlot, TrimPattern> patterns) {}
