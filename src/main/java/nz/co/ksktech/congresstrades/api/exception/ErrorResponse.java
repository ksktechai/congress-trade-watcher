package nz.co.ksktech.congresstrades.api.exception;

import java.time.Instant;

/** Uniform JSON error envelope returned by {@code GlobalExceptionMapper}. */
public record ErrorResponse(int status, String error, String message, Instant timestamp) {
  public static ErrorResponse of(int status, String error, String message) {
    return new ErrorResponse(status, error, message, Instant.now());
  }
}
