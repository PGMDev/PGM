package tc.oc.pgm.util.attribute;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.lang.Validate;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.util.NumberConversions;
import tc.oc.pgm.api.attribute.AttributeModifier;

/** Concrete implementation of an attribute modifier. */
public class AttributeModifierImpl implements ConfigurationSerializable, AttributeModifier {

  private final UUID uuid;
  private final String name;
  private final double amount;
  private final Operation operation;

  public AttributeModifierImpl(String name, double amount, Operation operation) {
    this(UUID.randomUUID(), name, amount, operation);
  }

  public AttributeModifierImpl(UUID uuid, String name, double amount, Operation operation) {
    Validate.notNull(uuid, "uuid");
    Validate.notEmpty(name, "Name cannot be empty");
    Validate.notNull(operation, "operation");

    this.uuid = uuid;
    this.name = name;
    this.amount = amount;
    this.operation = operation;
  }

  /**
   * Get the unique ID for this modifier.
   *
   * @return unique id
   */
  @Override
  public UUID getUniqueId() {
    return uuid;
  }

  /**
   * Get the name of this modifier.
   *
   * @return name
   */
  @Override
  public String getName() {
    return name;
  }

  /**
   * Get the amount by which this modifier will apply its {@link Operation}.
   *
   * @return modification amount
   */
  @Override
  public double getAmount() {
    return amount;
  }

  /**
   * Get the operation this modifier will apply.
   *
   * @return operation
   */
  @Override
  public Operation getOperation() {
    return operation;
  }

  @Override
  public Map<String, Object> serialize() {
    Map<String, Object> data = new HashMap<String, Object>();
    data.put("uuid", uuid.toString());
    data.put("name", name);
    data.put("operation", operation.ordinal());
    data.put("amount", amount);
    return data;
  }

  public static AttributeModifierImpl deserialize(Map<String, Object> args) {
    return new AttributeModifierImpl(
        UUID.fromString((String) args.get("uuid")),
        (String) args.get("name"),
        NumberConversions.toDouble(args.get("amount")),
        Operation.values()[NumberConversions.toInt(args.get("operation"))]);
  }
}
