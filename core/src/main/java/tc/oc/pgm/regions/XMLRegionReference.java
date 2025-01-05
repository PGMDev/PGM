package tc.oc.pgm.regions;

import java.util.Collection;
import java.util.Iterator;
import java.util.Random;
import java.util.stream.Stream;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.bukkit.util.BlockVector;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.filter.query.LocationQuery;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.region.Region;
import tc.oc.pgm.api.region.RegionDefinition;
import tc.oc.pgm.features.FeatureDefinitionContext;
import tc.oc.pgm.features.XMLFeatureReference;
import tc.oc.pgm.util.chunk.ChunkVector;
import tc.oc.pgm.util.xml.Node;

public class XMLRegionReference extends XMLFeatureReference<RegionDefinition>
    implements Region.Static {

  public XMLRegionReference(
      FeatureDefinitionContext context,
      Node node,
      @Nullable String id,
      Class<RegionDefinition> type) {
    super(context, node, id, type);
  }

  @Override
  public boolean contains(Vector point) {
    return getStatic().contains(point);
  }

  @Override
  public boolean contains(Location point) {
    return get().contains(point);
  }

  @Override
  public boolean contains(BlockVector pos) {
    return getStatic().contains(pos);
  }

  @Override
  public boolean contains(Block block) {
    return get().contains(block);
  }

  @Override
  public boolean contains(BlockState block) {
    return get().contains(block);
  }

  @Override
  public boolean contains(Entity entity) {
    return get().contains(entity);
  }

  @Override
  public boolean contains(LocationQuery query) {
    return get().contains(query);
  }

  @Override
  public boolean enters(Location from, Location to) {
    return get().enters(from, to);
  }

  @Override
  public boolean exits(Location from, Location to) {
    return get().exits(from, to);
  }

  @Override
  public boolean canGetRandom() {
    return get().canGetRandom();
  }

  @Override
  public Vector getRandom(Random random) {
    return get().getRandom(random);
  }

  @Override
  public boolean isBlockBounded() {
    return get().isBlockBounded();
  }

  @Override
  public boolean isStatic() {
    return get().isStatic();
  }

  @Override
  public Region.Static getStatic() {
    return get().getStatic();
  }

  @Override
  public Region.Static getStaticImpl(Match match) {
    return get().getStaticImpl(match);
  }

  @Override
  public Region.Static getStatic(Match match) {
    return get().getStatic(match);
  }

  @Override
  public Region.Static getStatic(World world) {
    return get().getStatic(world);
  }

  @Override
  public Bounds getBounds() {
    return get().getBounds();
  }

  @Override
  public boolean isEmpty() {
    return get().isEmpty();
  }

  @Override
  public Iterator<BlockVector> getBlockVectorIterator() {
    return getStatic().getBlockVectorIterator();
  }

  @Override
  public Iterable<BlockVector> getBlockVectors() {
    return getStatic().getBlockVectors();
  }

  @Override
  public Collection<Class<? extends Event>> getRelevantEvents() {
    return get().getRelevantEvents();
  }

  @Override
  public boolean matches(LocationQuery query) {
    return get().matches(query);
  }

  @Override
  public Iterable<Block> getBlocks(World world) {
    return get().getBlocks(world);
  }

  public Stream<ChunkVector> getChunkPositions() {
    return get().getChunkPositions();
  }
}
