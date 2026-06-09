package nz.co.ksktech.congresstrades.api.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * The daily digest payload: the LLM-generated narrative, the structured signals it was based on,
 * summary counts, and the mandatory not-financial-advice disclaimer.
 */
public record DigestResponse(
    LocalDate date,
    String narrative,
    int tradesConsidered,
    List<SignalDto> signals,
    String disclaimer) {}
