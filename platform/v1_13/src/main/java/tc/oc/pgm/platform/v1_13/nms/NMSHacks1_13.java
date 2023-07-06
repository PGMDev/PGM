package tc.oc.pgm.platform.v1_13.nms;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scoreboard.NameTagVisibility;
import tc.oc.pgm.platform.v1_13.NullChunkGenerator;
import tc.oc.pgm.platform.v1_13.material.LegacyMaterialUtils;
import tc.oc.pgm.platform.v1_13.material.MaterialDataProvider1_13;
import tc.oc.pgm.util.nms.material.MaterialData;
import tc.oc.pgm.util.nms.material.MaterialDataProvider;
import tc.oc.pgm.util.nms.material.MaterialDataProviderPlatform;
import tc.oc.pgm.util.nms.reflect.Refl;
import tc.oc.pgm.util.nms.reflect.ReflectionProxy;
import tc.oc.pgm.util.nms.v1_10_12.NMSHacks1_10_12;

public class NMSHacks1_13 extends NMSHacks1_10_12 {
  public static Refl.IBlockData reflIBlockData = ReflectionProxy.getProxy(Refl.IBlockData.class);

  @Override
  public Set<Material> getMaterialCollection(ItemMeta itemMeta, String key) {
    Map<String, Object> unhandledTags = refl.getUnhandledTags(itemMeta);
    if (!unhandledTags.containsKey(key)) return EnumSet.noneOf(Material.class);
    EnumSet<Material> materialSet = EnumSet.noneOf(Material.class);
    Object canDestroyList = unhandledTags.get(key);

    for (Object item : (List<Object>) nbtTagList.getListField(canDestroyList)) {
      String blockString = nbtTagString.getString(item);
      Material material = Material.matchMaterial(blockString);
      if (material != null) {
        materialSet.add(material);
      }
    }

    return materialSet;
  }

  @Override
  public boolean canMineBlock(Material material, ItemStack tool) {
    if (!material.isBlock()) {
      throw new IllegalArgumentException("Material '" + material + "' is not a block");
    }

    Object nmsBlock = craftMagicNumbers.getBlock(material);
    Object nmsTool = tool == null ? null : craftMagicNumbers.getItem(tool.getType());

    Object iBlockData = reflBlock.getBlockData(nmsBlock);

    boolean alwaysDestroyable = refl.isAlwaysDestroyable(reflIBlockData.getMaterial(nmsBlock));
    boolean toolCanDestroy = nmsTool != null && refl.canDestroySpecialBlock(nmsTool, iBlockData);
    return nmsBlock != null && (alwaysDestroyable || toolCanDestroy);
  }

  @Override
  public ItemStack craftItemCopy(ItemStack item) {
    if (item.getType().equals(Material.POTION)) {
      item = LegacyMaterialUtils.buildLegacyPotion(item.getDurability(), item.getAmount());
    }

    return craftItemStack.asCraftCopy(item);
  }

  @Override
  public ChunkGenerator nullChunkGenerator() {
    return new NullChunkGenerator();
  }

  static Map<DyeColor, Material> dyeColorMaterialMap = new HashMap<>();

  @Override
  public void spawnFlagParticles(Player bukkitPlayer, DyeColor dyeColor, Location location) {
    BlockData blockData =
        dyeColorMaterialMap
            .computeIfAbsent(dyeColor, (color -> Material.valueOf(color.name() + "_WOOL")))
            .createBlockData();
    bukkitPlayer.spawnParticle(
        Particle.BLOCK_DUST, location.clone().add(0, 56, 0), 50, 0.05f, 24f, 0.05f, 0f, blockData);
  }

  @Override
  public void spawnCritArrowParticles(Player playerBukkit, Location projectileLocation) {
    playerBukkit.spawnParticle(Particle.CRIT, projectileLocation, 1);
  }

  @Override
  public void spawnColoredArrowParticles(
      Color color, Player playerBukkit, Location projectileLocation) {
    playerBukkit.spawnParticle(
        Particle.REDSTONE, projectileLocation, 1, new Particle.DustOptions(color, 1));
  }

  @Override
  public void spawnPayloadParticles(World world, Location loc, Color color) {
    world.spawnParticle(Particle.REDSTONE, loc, 1, new Particle.DustOptions(color, 1));
  }

  @Override
  public void showExplosionParticle(Location explosion, Player playerBukkit) {
    playerBukkit.spawnParticle(Particle.EXPLOSION_HUGE, explosion, 1);
  }

  @Override
  public void spawnSpawnerParticles(World world, Location location) {
    world.spawnParticle(Particle.FLAME, location, 40, 0, 0.15f, 0, 0);
  }

  @Override
  public MaterialDataProviderPlatform getMaterialDataProvider() {
    return new MaterialDataProvider1_13();
  }

  @Override
  public Set<MaterialData> getBlockStates(Material material) {
    return LegacyMaterialUtils.getSimilarMaterials(material).stream()
        .map(MaterialDataProvider::from)
        .collect(Collectors.toSet());
  }

  @Override
  public Object teamPacket(
      int operation,
      String name,
      String displayName,
      String prefix,
      String suffix,
      boolean friendlyFire,
      boolean seeFriendlyInvisibles,
      NameTagVisibility nameTagVisibility,
      Collection<String> players) {
    PacketContainer packet = new PacketContainer(PacketType.Play.Server.SCOREBOARD_TEAM);

    String nameTagVisString = null;
    if (nameTagVisibility != null) {
      switch (nameTagVisibility) {
        case ALWAYS:
          nameTagVisString = "always";
          break;
        case NEVER:
          nameTagVisString = "never";
          break;
        case HIDE_FOR_OTHER_TEAMS:
          nameTagVisString = "hideForOtherTeams";
          break;
        case HIDE_FOR_OWN_TEAM:
          nameTagVisString = "hideForOwnTeam";
          break;
      }
    }

    packet
        .getStrings()
        .write(0, name)
        .write(1, nameTagVisString)
        .write(2, "never"); // collide with other players

    packet
        .getChatComponents()
        .write(0, WrappedChatComponent.fromLegacyText(displayName))
        .write(1, WrappedChatComponent.fromLegacyText(prefix))
        .write(2, WrappedChatComponent.fromLegacyText(suffix));

    int flags = 0;
    if (friendlyFire) {
      flags |= 1;
    }
    if (seeFriendlyInvisibles) {
      flags |= 2;
    }

    packet.getIntegers().write(0, operation).write(1, flags);

    packet.getSpecificModifier(Collection.class).write(0, players);

    return packet;
  }
}
