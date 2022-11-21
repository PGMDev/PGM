package tc.oc.pgm.api.integration;

import java.util.concurrent.CompletableFuture;
import tc.oc.pgm.util.translation.Translation;

public interface TranslationIntegration {

  CompletableFuture<Translation> translate(String message);
}
