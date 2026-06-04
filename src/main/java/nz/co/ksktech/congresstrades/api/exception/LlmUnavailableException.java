package nz.co.ksktech.congresstrades.api.exception;

/**
 * Thrown when the Anthropic digest could not be generated (missing API key,
 * timeout, circuit open, etc.). Mapped to HTTP 503 by {@code GlobalExceptionMapper}.
 */
public class LlmUnavailableException extends RuntimeException {
    public LlmUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }

    public LlmUnavailableException(String message) {
        super(message);
    }
}
