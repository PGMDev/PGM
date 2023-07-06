package tc.oc.pgm.util.nms.reflect;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Reflect {
  @Retention(RetentionPolicy.RUNTIME)
  @Repeatable(NMS.List.class)
  @interface NMS {
    String value();

    Class<?>[] parameters() default {};

    @Retention(RetentionPolicy.RUNTIME)
    @interface List {
      NMS[] value();
    }
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Repeatable(CB.List.class)
  @interface CB {
    String value();

    Class<?>[] parameters() default {};

    @Retention(RetentionPolicy.RUNTIME)
    @interface List {
      CB[] value();
    }
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Repeatable(B.List.class)
  @interface B {
    String value();

    Class<?>[] parameters() default {};

    @Retention(RetentionPolicy.RUNTIME)
    @interface List {
      B[] value();
    }
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Repeatable(StaticMethod.List.class)
  @interface StaticMethod {
    String value();

    Class<?>[] parameters() default {};

    @Retention(RetentionPolicy.RUNTIME)
    @interface List {
      StaticMethod[] value();
    }
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Repeatable(Method.List.class)
  @interface Method {
    String value();

    Class<?>[] parameters() default {};

    @Retention(RetentionPolicy.RUNTIME)
    @interface List {
      Method[] value();
    }
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Repeatable(Field.List.class)
  @interface Field {
    String value();

    @Retention(RetentionPolicy.RUNTIME)
    @interface List {
      Field[] value();
    }
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Repeatable(Constructor.List.class)
  @interface Constructor {
    Class<?>[] value() default {};

    @Retention(RetentionPolicy.RUNTIME)
    @interface List {
      Constructor[] value();
    }
  }
}
