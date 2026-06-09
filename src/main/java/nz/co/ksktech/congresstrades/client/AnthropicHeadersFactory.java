package nz.co.ksktech.congresstrades.client;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import nz.co.ksktech.congresstrades.config.AppConfig;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;

/**
 * Injects the Anthropic auth and protocol headers on every request: {@code x-api-key} (from the
 * {@code ANTHROPIC_API_KEY} env var, via {@link AppConfig}), {@code anthropic-version}, and {@code
 * content-type}.
 *
 * <p>The API key is read from configuration, never hard-coded, so no secret touches source control.
 */
@ApplicationScoped
public class AnthropicHeadersFactory implements ClientHeadersFactory {

  @Inject AppConfig appConfig;

  @ConfigProperty(name = "anthropic.api.version", defaultValue = "2023-06-01")
  String anthropicVersion;

  @Override
  public MultivaluedMap<String, String> update(
      MultivaluedMap<String, String> incomingHeaders,
      MultivaluedMap<String, String> clientOutgoingHeaders) {
    MultivaluedMap<String, String> result = new MultivaluedHashMap<>();
    result.add("x-api-key", appConfig.anthropic().apiKey().orElse(""));
    result.add("anthropic-version", anthropicVersion);
    result.add("content-type", "application/json");
    return result;
  }
}
