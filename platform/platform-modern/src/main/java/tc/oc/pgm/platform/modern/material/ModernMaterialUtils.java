package tc.oc.pgm.platform.modern.material;

import static tc.oc.pgm.util.platform.Supports.Variant.PAPER;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.type.Door;
import org.bukkit.craftbukkit.legacy.CraftLegacy;
import org.bukkit.craftbukkit.util.CraftMagicNumbers;
import org.bukkit.entity.GlowItemFrame;
import org.bukkit.entity.Hanging;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.LeashHitch;
import org.bukkit.entity.Painting;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BlockVector;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.util.chunk.ChunkVector;
import tc.oc.pgm.util.material.BlockMaterialData;
import tc.oc.pgm.util.material.ItemMaterialData;
import tc.oc.pgm.util.material.MaterialMatcher;
import tc.oc.pgm.util.material.MaterialUtils;
import tc.oc.pgm.util.platform.Supports;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;

@Supports(value = PAPER, minVersion = "1.20.6")
@SuppressWarnings("deprecation")
public class ModernMaterialUtils implements MaterialUtils {

  @Override
  public BlockMaterialData createBlockData(Material material) {
    if (!material.isBlock()) {
      throw new IllegalStateException("Material " + material + " is not a block");
    }
    return new ModernBlockData(material.createBlockData());
  }

  @Override
  public BlockMaterialData createBlockData(BlockState block) {
    return new ModernBlockData(block.getBlockData());
  }

  @Override
  public BlockMaterialData createBlockData(ChunkSnapshot chunk, BlockVector chunkPos) {
    return new ModernBlockData(
        chunk.getBlockData(chunkPos.getBlockX(), chunkPos.getBlockY(), chunkPos.getBlockZ()));
  }

  @Override
  public BlockMaterialData getTo(EntityChangeBlockEvent event) {
    return new ModernBlockData(event.getBlockData());
  }

  @Override
  public ItemMaterialData createItemData(ItemStack item) {
    return new ModernItemData(item.getType());
  }

  @Override
  public ItemMaterialData createItemData(Material material, short data) {
    return ModernMaterialParser.parseItem(material, data);
  }

  @Override
  public ItemMaterialData createItemData(Hanging hanging) {
    return new ModernItemData(
        switch (hanging) {
          case Painting ignored -> Material.PAINTING;
          case GlowItemFrame ignored -> Material.GLOW_ITEM_FRAME;
          case ItemFrame ignored -> Material.ITEM_FRAME;
          case LeashHitch ignored -> Material.LEAD;
          default -> null;
        });
  }

  @Override
  public BlockMaterialData decode(int encoded) {
    return ModernEncodeUtil.decode(encoded);
  }

  @Override
  public Iterator<tc.oc.pgm.util.block.BlockData> iterator(
      Map<ChunkVector, ChunkSnapshot> chunks, Iterator<BlockVector> vectors) {
    return new BlockDataIterator(chunks, vectors);
  }

  @Override
  public Material parseMaterial(String text, @Nullable Node node) throws InvalidXMLException {
    return ModernMaterialParser.parseMaterial(text, node);
  }

  @Override
  public ItemMaterialData parseItemMaterialData(String text, @Nullable Node node)
      throws InvalidXMLException {
    return ModernMaterialParser.parseItem(text, node);
  }

  @Override
  public ItemMaterialData parseItemMaterialData(String text, short dmg, @Nullable Node node)
      throws InvalidXMLException {
    return ModernMaterialParser.parseItem(text, dmg, node);
  }

  @Override
  public BlockMaterialData parseBlockMaterialData(String text, @Nullable Node node)
      throws InvalidXMLException {
    var md = ModernMaterialParser.parseBlock(text, node);
    if (!md.getItemType().isBlock()) {
      throw new InvalidXMLException(
          "Material " + md.getItemType().name() + " is not a block", node);
    }
    return md;
  }

  @Override
  public BlockMaterialData fromLegacyBlock(Material material, byte data) {
    return new ModernBlockData(Bukkit.getUnsafe().fromLegacy(material, data));
  }

  @Override
  public Set<BlockMaterialData> getPossibleBlocks(Material material) {
    // Get all possible blockstates off of nms
    var block = CraftMagicNumbers.getBlock(material);
    var states = block.getStateDefinition().getPossibleStates();
    Set<BlockMaterialData> materials = new HashSet<>(states.size());
    for (var state : states) {
      materials.add(new ModernBlockData(state.createCraftBlockData()));
    }
    return materials;
  }

  @Override
  public MaterialMatcher.Builder matcherBuilder() {
    return new MaterialMatcherBuilderImpl();
  }

  @Override
  public boolean isUpperHalfOfDoor(Block block) {
    return block.getState().getBlockData() instanceof Door door
        && door.getHalf() == Bisected.Half.TOP;
  }

  private static class MaterialMatcherBuilderImpl extends MaterialMatcher.BuilderImpl
      implements ModernMaterialParser.Adapter<MaterialMatcher.Builder> {
    private Node currentNode = null;

    @Override
    public MaterialMatcher.Builder visit(Material material) {
      return add(material, true);
    }

    @Override
    public MaterialMatcher.Builder visit(Material material, short data) {
      // Filter out air so any air=invalid later on, and any modern material
      if (material.isAir() || !material.isLegacy()) return add(CraftLegacy.fromLegacy(material));

      var item = CraftLegacy.fromLegacyData(material, data);
      var itemMaterial = CraftMagicNumbers.getMaterial(item);

      // Treat as item, upgrade the material to its modern post-flattening version and we're done
      if (!itemMaterial.isAir() && material.getId() > 255) return add(itemMaterial);

      var block = CraftLegacy.fromLegacyData(material, (byte) data);
      var blockMaterial = block.getBukkitMaterial();

      if (itemMaterial.isAir() && blockMaterial.isAir()) {
        var ex = new InvalidXMLException(
            "Material doesn't exist (did it ever?)'" + material + ":" + data + "'", currentNode);
        PGM.get().getGameLogger().log(Level.WARNING, null, ex);
        return this;
      }

      return switch (ModernMaterialParser.getUpgradeStrategy(material, (byte) data)) {
        // Exists as single-state in modern, eg: wool:0 -> white_wool
        // Or a single legacy state can translate into it, eg:
        // stained_glass_pane:8 -> light_gray_stained_glass_pane[*]
        // wall:0 -> cobblestone_wall[*]
        case MATERIAL -> add(blockMaterial);
        // There is a 1:1 mapping between upgraded states and modern states eg:
        // log:0 -> oak_log[axis=y],
        // log:4 -> oak_log[axis=x],
        // log:8 -> oak_log[axis=z]
        case BLOCK_STATE -> add(new BlockStateMaterialMatcher(block.createCraftBlockData()));
        // Legacy translates into LESS states than are available in modern, eg:
        // step:0 -> smooth_stone_slab[type=bottom,waterlogged=false],
        // step:8 -> smooth_stone_slab[type=top,waterlogged=false]
        // TODO: step:0 could use a new matcher for smooth_stone_slab[type=bottom,waterlogged=*]
        // Basically any water-loggable block goes thru here
        case GOOD_LUCK -> add(blockMaterial);
      };
    }

    @Override
    public MaterialMatcher.Builder add(Material material, boolean flatten) {
      return flatten
          ? addAll(ModernMaterialParser.flatten(material))
          : add(CraftLegacy.fromLegacy(material));
    }

    @Override
    public MaterialMatcher.Builder add(ItemStack item, boolean flatten) {
      return add(item.getType(), flatten);
    }

    @Override
    protected void parseSingle(String text, @Nullable Node node) throws InvalidXMLException {
      if (blocksOnly) {
        var md = ModernMaterialParser.parseBlock(text, node);
        if (!md.getItemType().isBlock())
          throw new InvalidXMLException(
              "Material " + md.getItemType().name() + " is not a block", node);
      }

      try {
        currentNode = node;
        ModernMaterialParser.parse(text, node, materialsOnly, this);
      } finally {
        currentNode = null;
      }
    }
  }
}
