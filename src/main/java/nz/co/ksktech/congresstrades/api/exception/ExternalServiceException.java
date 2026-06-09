package nz.co.ksktech.congresstrades.api.exception;

/**
 * Thrown when an upstream provider (Finnhub) fails in a way the app cannot recover from. Mapped to
 * HTTP 502 by {@code GlobalExceptionMapper}.
 */
public class ExternalServiceException extends RuntimeException {
  public ExternalServiceException(String message, Throwable cause) {
    super(message, cause);
  }
}
