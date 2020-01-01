/*
 * Copyright 2020 Aleksander Jagiełło <themolkapl@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package tc.oc.pgm.maptag;

import static com.google.common.base.Preconditions.*;

import java.util.function.Predicate;
import tc.oc.pgm.map.PGMMap;

public class StandardMapTag extends MapTag implements Predicate<PGMMap> {
  private final Predicate<PGMMap> ifApplicable;

  protected StandardMapTag(String name, Predicate<PGMMap> ifApplicable) {
    super(name);
    this.ifApplicable = checkNotNull(ifApplicable);
  }

  @Override
  public boolean test(PGMMap pgmMap) {
    return this.ifApplicable.test(pgmMap);
  }
}
