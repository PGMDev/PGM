package tc.oc.pgm.util.platform;

public abstract class AbstractPlatform {

  public AbstractPlatform() {
    Supports support = getClass().getAnnotation(Supports.class);
  }

  public abstract <T> T getInstance(Class<T> clazz);
}
