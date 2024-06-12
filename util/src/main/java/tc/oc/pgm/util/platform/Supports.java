package tc.oc.pgm.util.platform;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@Repeatable(Supports.List.class)
public @interface Supports {
  Variant value();

  String minVersion() default "";

  String maxVersion() default "";

  Priority priority() default Priority.MEDIUM;

  enum Variant {
    SPORTPAPER,
    SPIGOT,
    PAPER;
  }

  enum Priority {
    LOWEST,
    LOW,
    MEDIUM,
    HIGH,
    HIGHEST
  }

  @Retention(RetentionPolicy.RUNTIME)
  @interface List {
    Supports[] value();
  }
}
