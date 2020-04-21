package tc.oc.pgm.regions;

import java.util.Iterator;
import java.util.Random;
import javax.annotation.Nullable;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.util.BlockVector;
import org.bukkit.util.Vector;
import tc.oc.pgm.api.filter.query.Query;
import tc.oc.pgm.api.region.Region;
import tc.oc.pgm.api.region.RegionDefinition;
import tc.oc.pgm.features.FeatureDefinitionContext;
import tc.oc.pgm.features.XMLFeatureReference;
import tc.oc.pgm.util.xml.Node;

public class XMLRegionReference extends XMLFeatureReference<RegionDefinition> implements Region {

  public XMLRegionReference(
      FeatureDefinitionContext context, Node node, Class<RegionDefinition> type) {
    super(context, node, type);
  }

  public XMLRegionReference(
      FeatureDefinitionContext context,
      Node node,
      @Nullable String id,
      Class<RegionDefinition> type) {
    super(context, node, id, type);
  }

  @Override
  public boolean contains(Vector point) {
    return get().contains(point);
  }

  @Override
  public boolean contains(Location point) {
    return get().contains(point);
  }

  @Override
  public boolean contains(BlockVector pos) {
    return get().contains(pos);
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
  public boolean enters(Location from, Location to) {
    return get().enters(from, to);
  }

  @Override
  public boolean enters(Vector from, Vector to) {
    return get().enters(from, to);
  }

  @Override
  public boolean exits(Location from, Location to) {
    return get().exits(from, to);
  }

  @Override
  public boolean exits(Vector from, Vector to) {
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
  public Bounds getBounds() {
    return get().getBounds();
  }

  @Override
  public boolean isEmpty() {
    return get().isEmpty();
  }

  @Override
  public Iterator<BlockVector> getBlockVectorIterator() {
    return get().getBlockVectorIterator();
  }

  @Override
  public Iterable<BlockVector> getBlockVectors() {
    return get().getBlockVectors();
  }

  @Override
  public Class<? extends Query> getQueryType() {
    return get().getQueryType();
  }

  @Override
  public QueryResponse query(Query query) {
    return get().query(query);
  }
}
