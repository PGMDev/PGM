package tc.oc.pgm.integration;

import java.util.concurrent.CompletableFuture;
import tc.oc.pgm.api.integration.TranslationIntegration;
import tc.oc.pgm.util.translation.Translation;

public class TranslationIntegrationImpl implements TranslationIntegration {

  @Override
  public CompletableFuture<Translation> translate(String message) {
    return CompletableFuture.completedFuture(new Translation(message));
  }
}
