package nz.co.ksktech.congresstrades.api.exception;

/**
 * Thrown when a requested domain resource (e.g. a member) does not exist.
 * Mapped to HTTP 404 by {@code GlobalExceptionMapper}.
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
