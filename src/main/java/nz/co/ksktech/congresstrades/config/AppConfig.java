package nz.co.ksktech.congresstrades.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

import java.util.Optional;

/**
 * Strongly-typed application configuration, bound from {@code application.properties}
 * (and overridable by environment variables). API keys are referenced here but
 * resolved from env vars — see {@code FINNHUB_API_KEY} / {@code ANTHROPIC_API_KEY}.
 */
@ConfigMapping(prefix = "watcher")
public interface AppConfig {

    Finnhub finnhub();

    Llm llm();

    Anthropic anthropic();

    Gemini gemini();

    Signals signals();

    interface Finnhub {
        /** Empty when {@code FINNHUB_API_KEY} is unset; the app still boots. */
        Optional<String> apiKey();
    }

    interface Llm {
        /** Which narration provider to use: {@code gemini} or {@code anthropic}. */
        @WithDefault("gemini")
        String provider();
    }

    interface Anthropic {
        /** Empty when {@code ANTHROPIC_API_KEY} is unset; the app still boots. */
        Optional<String> apiKey();

        @WithDefault("claude-sonnet-4-5")
        String model();

        @WithDefault("1500")
        int maxTokens();
    }

    interface Gemini {
        /** Empty when {@code GEMINI_API_KEY} is unset; the app still boots. */
        Optional<String> apiKey();

        @WithDefault("gemini-flash-latest")
        String model();

        @WithDefault("2048")
        int maxTokens();

        /** Tokens the 2.5-series model may spend "thinking"; 0 disables it
         *  (recommended for narration), negative omits the field. */
        @WithDefault("0")
        int thinkingBudget();
    }

    interface Signals {
        /** Look-back window (days) for the digest and on-demand detection. */
        @WithDefault("45")
        int lookbackDays();

        /** Distinct buyers needed within the cluster window. */
        @WithDefault("3")
        int clusterMinMembers();

        /** Cluster window length in days. */
        @WithDefault("14")
        int clusterWindowDays();

        /** Multiple of the member's median that counts as an outlier. */
        @WithDefault("3.0")
        double outlierMultiplier();

        /** daysToDisclose strictly above this is flagged LATE_DISCLOSURE. */
        @WithDefault("40")
        int lateDisclosureThreshold();

        /** Trades in the same ticker by one member within this window. */
        @WithDefault("30")
        int concentrationWindowDays();

        /** Trade count in the concentration window that triggers a signal. */
        @WithDefault("3")
        int concentrationMinTrades();
    }
}
