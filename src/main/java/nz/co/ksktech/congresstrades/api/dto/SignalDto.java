package nz.co.ksktech.congresstrades.api.dto;

import nz.co.ksktech.congresstrades.domain.Signal;
import nz.co.ksktech.congresstrades.domain.enums.SignalType;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Read model for a detected {@link Signal}.
 */
public record SignalDto(
        Long id,
        SignalType signalType,
        String ticker,
        String description,
        Double score,
        LocalDateTime detectedAt,
        List<Long> relatedTradeIds
) {
    public static SignalDto from(Signal s) {
        return new SignalDto(
                s.id,
                s.signalType,
                s.ticker,
                s.description,
                s.score,
                s.detectedAt,
                s.relatedTradeIdList());
    }
}
