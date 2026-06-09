package nz.co.ksktech.congresstrades.config;

/**
 * Central home for the honest-framing disclaimer. Surfaced in the digest API output and referenced
 * in the README. This app is a research/learning tool, not a trading-signal generator.
 */
public final class Disclaimers {

  public static final String NOT_FINANCIAL_ADVICE =
      "This is a research and learning tool, NOT financial advice and NOT a trading-signal generator. "
          + "Congressional trades are disclosed up to 45 days after they occur, amounts are reported only "
          + "as broad ranges (not exact figures), and following these trades does not reliably beat the "
          + "market. Signals surface patterns for human research only. Do your own due diligence and "
          + "consult a licensed professional before making any investment decision.";

  private Disclaimers() {}
}
