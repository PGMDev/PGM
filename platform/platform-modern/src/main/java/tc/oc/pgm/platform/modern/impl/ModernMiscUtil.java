package tc.oc.pgm.platform.modern.impl;

import static tc.oc.pgm.util.platform.Supports.Variant.PAPER;

import com.google.gson.JsonObject;
import java.nio.file.Path;
import java.util.List;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.Event;
import org.bukkit.event.EventException;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scoreboard.Team;
import tc.oc.pgm.platform.modern.material.ModernBlockMaterialData;
import tc.oc.pgm.util.bukkit.MiscUtils;
import tc.oc.pgm.util.material.BlockMaterialData;
import tc.oc.pgm.util.platform.Supports;

@Supports(value = PAPER, minVersion = "1.20.6")
public class ModernMiscUtil implements MiscUtils {
  @Override
  public boolean yield(Event event) {
    return false;
  }

  @Override
  public JsonObject getServerListExtra(ServerListPingEvent event, Plugin plugin) {
    // TODO: PLATFORM 1.20 no support for extra fields in server ping
    return new JsonObject();
  }

  @Override
  public EventException createEventException(Throwable cause, Event event) {
    return new EventException(cause);
  }

  @Override
  public void createExplosion(Entity source, float power, boolean fire, boolean destroy) {
    source.getWorld().createExplosion(source, source.getLocation(), power, fire, destroy);
  }

  @Override
  @SuppressWarnings({"deprecation", "UnstableApiUsage"})
  public PlayerDeathEvent createDeathEvent(
      Player player, EntityDamageEvent.DamageCause dmg, List<ItemStack> drops, String msg) {
    return new PlayerDeathEvent(
        player, DamageSource.builder(DamageType.GENERIC_KILL).build(), drops, 0, msg);
  }

  @Override
  public EntityChangeBlockEvent createEntityChangeBlockEvent(
      Player player, Block block, BlockMaterialData md) {
    return new EntityChangeBlockEvent(player, block, ((ModernBlockMaterialData) md).getBlock());
  }

  @Override
  public int getDurationTicks(EntityCombustEvent event) {
    // Seconds to ticks
    return (int) (event.getDuration() * 20);
  }

  @Override
  public ThrownPotion spawnPotion(Location loc, ItemStack item) {
    var world = ((CraftWorld) loc.getWorld()).getHandle();
    var potion = new net.minecraft.world.entity.projectile.ThrownPotion(
        world, loc.getX(), loc.getY(), loc.getZ());
    potion.setItem(CraftItemStack.asNMSCopy(item));
    world.addFreshEntity(potion);
    return (ThrownPotion) potion.getBukkitEntity();
  }

  @Override
  public double getArrowDamage(Arrow arrow) {
    return arrow.getDamage();
  }

  @Override
  public int getWorldDataVersion(Path levelDat) {
    // Constant from LevelStorageSource, sounds way too high (104mb) but better than unbounded
    long MAX_HEAP = 104857600L;
    try {
      var root = NbtIo.readCompressed(levelDat, NbtAccounter.create(MAX_HEAP));
      return NbtUtils.getDataVersion(root.getCompound("Data"), -1);
    } catch (Throwable ignored) {
      // In case we cannot read the level.dat file, return a constant
      return -1;
    }
  }

  @Override
  public Key getSound(Sound enumConstant) {
    return enumConstant.key();
  }

  @Override
  public void initScoreboardTeam(Team team, NamedTextColor color) {
    team.color(color);
    team.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
  }
}
