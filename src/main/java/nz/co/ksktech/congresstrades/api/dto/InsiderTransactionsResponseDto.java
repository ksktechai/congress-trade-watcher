package nz.co.ksktech.congresstrades.api.dto;

import java.util.List;
import nz.co.ksktech.congresstrades.client.dto.InsiderTransaction;

/**
 * API payload for the insider-transactions endpoint: the requested symbol and date window, a count,
 * the transactions, and the mandatory disclaimer.
 */
public record InsiderTransactionsResponseDto(
    String symbol,
    String from,
    String to,
    int count,
    List<InsiderTransaction> transactions,
    String disclaimer) {}
