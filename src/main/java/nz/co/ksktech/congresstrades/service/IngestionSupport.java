package nz.co.ksktech.congresstrades.service;

import nz.co.ksktech.congresstrades.domain.enums.Chamber;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Locale;

/**
 * Small, side-effect-free helpers shared by ingestion sources.
 */
public final class IngestionSupport {

    private IngestionSupport() {
    }

    /**
     * Tolerant ISO date parse. Accepts {@code yyyy-MM-dd} (optionally with a time
     * component) and returns {@code null} for blank/unparseable input.
     */
    public static LocalDate parseIsoDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            String v = value.trim();
            return LocalDate.parse(v.length() > 10 ? v.substring(0, 10) : v);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /**
     * Maps a free-text chamber/branch label onto the {@link Chamber} enum.
     */
    public static Chamber chamberOf(String raw) {
        if (raw == null) {
            return Chamber.UNKNOWN;
        }
        String value = raw.toLowerCase(Locale.ROOT);
        if (value.contains("house")) {
            return Chamber.HOUSE;
        }
        if (value.contains("senate")) {
            return Chamber.SENATE;
        }
        return Chamber.UNKNOWN;
    }
}
