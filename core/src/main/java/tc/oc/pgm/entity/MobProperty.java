package tc.oc.pgm.entity;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.bukkit.entity.Entity;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;

public record MobProperty<E, V>(
    Class<E> ownerType, String xmlName, Parser<V> parser, BiConsumer<E, V> setter) {

  @SuppressWarnings("unchecked")
  public Consumer<Entity> parseValue(Node node) throws InvalidXMLException {
    V value = parser.parse(node);
    return entity -> setter.accept((E) entity, value);
  }

  @FunctionalInterface
  public interface Parser<V> {
    V parse(Node node) throws InvalidXMLException;
  }
}
