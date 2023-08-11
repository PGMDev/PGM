package tc.oc.pgm.util.nms.reflect;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Reflect {
  @Retention(RetentionPolicy.RUNTIME)
  @interface NMS {
    String value();

    Class<?>[] parameters() default {};
  }

  @Retention(RetentionPolicy.RUNTIME)
  @interface CB {
    String value();

    Class<?>[] parameters() default {};
  }

  @Retention(RetentionPolicy.RUNTIME)
  @interface B {
    String value();

    Class<?>[] parameters() default {};
  }

  @Retention(RetentionPolicy.RUNTIME)
  @interface StaticMethod {
    String value();

    Class<?>[] parameters() default {};
  }

  @Retention(RetentionPolicy.RUNTIME)
  @interface Method {
    String value();

    Class<?>[] parameters() default {};
  }

  @Retention(RetentionPolicy.RUNTIME)
  @interface Field {
    String value();
  }

  @Retention(RetentionPolicy.RUNTIME)
  @interface Constructor {
    Class<?>[] value() default {};
  }
}
