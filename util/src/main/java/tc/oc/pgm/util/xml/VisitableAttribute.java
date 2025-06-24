package tc.oc.pgm.util.xml;

import java.util.concurrent.atomic.AtomicBoolean;
import org.jdom2.Attribute;
import org.jdom2.AttributeType;
import org.jdom2.Namespace;

public class VisitableAttribute extends Attribute {

  private AtomicBoolean visited = new AtomicBoolean();

  public VisitableAttribute() {}

  public VisitableAttribute(String name, String value, Namespace namespace) {
    super(name, value, namespace);
  }

  public VisitableAttribute(String name, String value, AttributeType type, Namespace namespace) {
    super(name, value, type, namespace);
  }

  public VisitableAttribute(String name, String value) {
    super(name, value);
  }

  public VisitableAttribute(String name, String value, AttributeType type) {
    super(name, value, type);
  }

  public VisitableAttribute(String name, String value, int type) {
    super(name, value, type);
  }

  @Override
  public String getValue() {
    if (((DocumentWrapper) getDocument()).isVisitingAllowed()) visited.set(true);
    return super.getValue();
  }

  public boolean wasVisited() {
    return visited.get();
  }

  @Override
  public Attribute clone() {
    VisitableAttribute copy = (VisitableAttribute) super.clone();
    copy.visited = visited;
    return copy;
  }
}
