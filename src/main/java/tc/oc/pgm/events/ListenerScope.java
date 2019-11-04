package tc.oc.pgm.events;

import java.lang.annotation.*;
import tc.oc.pgm.api.match.MatchScope;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface ListenerScope {
  MatchScope value();
}
