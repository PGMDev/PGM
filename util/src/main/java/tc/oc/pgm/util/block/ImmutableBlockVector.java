package tc.oc.pgm.util.block;

import org.bukkit.util.BlockVector;
import org.bukkit.util.Vector;

/**
 * Override all mutating methods of {@link BlockVector} and throw an {@link
 * UnsupportedOperationException}. Subclasses can still access the component fields directly since
 * they have protected access.
 */
public class ImmutableBlockVector extends BlockVector {

  public ImmutableBlockVector() {}

  public ImmutableBlockVector(Vector vec) {
    super(vec);
  }

  public ImmutableBlockVector(int x, int y, int z) {
    super(x, y, z);
  }

  public ImmutableBlockVector(double x, double y, double z) {
    super(x, y, z);
  }

  public ImmutableBlockVector(float x, float y, float z) {
    super(x, y, z);
  }

  protected RuntimeException ex() {
    return new UnsupportedOperationException("This object is immutable");
  }

  @Override
  public Vector add(Vector vec) {
    throw ex();
  }

  @Override
  public Vector subtract(Vector vec) {
    throw ex();
  }

  @Override
  public Vector multiply(Vector vec) {
    throw ex();
  }

  @Override
  public Vector divide(Vector vec) {
    throw ex();
  }

  @Override
  public Vector multiply(int m) {
    throw ex();
  }

  @Override
  public Vector multiply(double m) {
    throw ex();
  }

  @Override
  public Vector multiply(float m) {
    throw ex();
  }

  @Override
  public Vector normalize() {
    throw ex();
  }

  @Override
  public Vector zero() {
    throw ex();
  }

  @Override
  public Vector crossProduct(Vector o) {
    throw ex();
  }

  @Override
  public Vector setX(int x) {
    throw ex();
  }

  @Override
  public Vector setX(double x) {
    throw ex();
  }

  @Override
  public Vector setX(float x) {
    throw ex();
  }

  @Override
  public Vector setY(int y) {
    throw ex();
  }

  @Override
  public Vector setY(double y) {
    throw ex();
  }

  @Override
  public Vector setY(float y) {
    throw ex();
  }

  @Override
  public Vector setZ(double z) {
    throw ex();
  }

  @Override
  public Vector setZ(int z) {
    throw ex();
  }

  @Override
  public Vector setZ(float z) {
    throw ex();
  }

  @Override
  public Vector copy(Vector vec) {
    throw ex();
  }
}
