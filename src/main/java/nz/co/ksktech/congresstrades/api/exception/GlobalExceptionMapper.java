package nz.co.ksktech.congresstrades.api.exception;

import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

/**
 * Translates exceptions into the uniform {@link ErrorResponse} JSON envelope and an appropriate
 * HTTP status. Keeps stack traces out of API responses.
 */
@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Throwable> {

  private static final Logger LOG = Logger.getLogger(GlobalExceptionMapper.class);

  @Override
  public Response toResponse(Throwable exception) {
    if (exception instanceof ResourceNotFoundException) {
      return build(Response.Status.NOT_FOUND, "Not Found", exception.getMessage());
    }
    if (exception instanceof ConstraintViolationException cve) {
      return build(Response.Status.BAD_REQUEST, "Validation Failed", cve.getMessage());
    }
    if (exception instanceof IllegalArgumentException) {
      return build(Response.Status.BAD_REQUEST, "Bad Request", exception.getMessage());
    }
    if (exception instanceof LlmUnavailableException) {
      LOG.warn("Digest generation failed", exception);
      return build(
          Response.Status.SERVICE_UNAVAILABLE, "Digest Unavailable", exception.getMessage());
    }
    if (exception instanceof ExternalServiceException) {
      LOG.warn("Upstream provider failed", exception);
      return build(Response.Status.BAD_GATEWAY, "Upstream Error", exception.getMessage());
    }
    if (exception instanceof WebApplicationException wae) {
      return build(
          Response.Status.fromStatusCode(wae.getResponse().getStatus()),
          "Request Error",
          exception.getMessage());
    }
    LOG.error("Unhandled exception", exception);
    return build(
        Response.Status.INTERNAL_SERVER_ERROR,
        "Internal Server Error",
        "An unexpected error occurred.");
  }

  private Response build(Response.Status status, String error, String message) {
    return Response.status(status)
        .entity(ErrorResponse.of(status.getStatusCode(), error, message))
        .build();
  }
}
