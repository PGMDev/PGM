package tc.oc.pgm.util.xml;

public interface Validator<T> {
  void validate(T t, Node node) throws InvalidXMLException;
}
