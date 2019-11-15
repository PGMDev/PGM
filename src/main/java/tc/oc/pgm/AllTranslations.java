package tc.oc.pgm;

import tc.oc.pgm.api.PGM;
import tc.oc.translations.BaseTranslator;
import tc.oc.translations.provider.TranslationProvider;

public final class AllTranslations extends BaseTranslator {

  private static AllTranslations instance;

  public AllTranslations() {
    super(
        PGM.get().getLogger(),
        new TranslationProvider("chatmoderator.ChatModeratorErrors"),
        new TranslationProvider("chatmoderator.ChatModeratorMessages"),
        new TranslationProvider("adminchat.AdminChatErrors"),
        new TranslationProvider("adminchat.AdminChatMessages"),
        new TranslationProvider("commons.Commons"),
        new TranslationProvider("raindrops.RaindropsMessages"),
        new TranslationProvider("tourney.Tourney"),
        new TranslationProvider("pgm.PGMErrors"),
        new TranslationProvider("pgm.PGMMessages"),
        new TranslationProvider("pgm.PGMMiscellaneous"),
        new TranslationProvider("pgm.PGMUI"),
        new TranslationProvider("pgm.PGMDeath"),
        new TranslationProvider("projectares.PAErrors"),
        new TranslationProvider("projectares.PAMessages"),
        new TranslationProvider("projectares.PAMiscellaneous"),
        new TranslationProvider("projectares.PAUI"));
    instance = this;
  }

  public static AllTranslations get() {
    return instance == null ? new AllTranslations() : instance;
  }
}
