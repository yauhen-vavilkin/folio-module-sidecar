package org.folio.sidecar.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import io.smallrye.config.PropertiesConfigSource;
import io.smallrye.config.SmallRyeConfigBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.folio.sidecar.configuration.properties.WebClientConfig;
import org.folio.support.types.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
class WebClientTimeoutConfigTest {

  @Test
  void ingressTimeout_positive_defaultsWhenEnvVariablesNotSet() {
    var timeout = webClientConfig(Map.of()).ingress().timeout();

    assertThat(timeout.connect()).isEqualTo(60000);
    assertThat(timeout.keepAlive()).isEqualTo(60);
    assertThat(timeout.idle()).isZero();
    assertThat(timeout.readIdle()).isZero();
    assertThat(timeout.writeIdle()).isZero();
  }

  @Test
  void egressTimeout_positive_defaultsWhenEnvVariablesNotSet() {
    var timeout = webClientConfig(Map.of()).egress().timeout();

    assertThat(timeout.connect()).isEqualTo(60000);
    assertThat(timeout.keepAlive()).isEqualTo(60);
    assertThat(timeout.idle()).isZero();
    assertThat(timeout.readIdle()).isZero();
    assertThat(timeout.writeIdle()).isZero();
  }

  @Test
  void ingressTimeout_positive_configurableViaEnvVariables() {
    var envVariables = Map.of(
      "WEB_CLIENT_INGRESS_TIMEOUT_CONNECT", "30000",
      "WEB_CLIENT_INGRESS_TIMEOUT_KEEP_ALIVE", "90",
      "WEB_CLIENT_INGRESS_TIMEOUT_IDLE", "10",
      "WEB_CLIENT_INGRESS_TIMEOUT_READ_IDLE", "15",
      "WEB_CLIENT_INGRESS_TIMEOUT_WRITE_IDLE", "20");

    var timeout = webClientConfig(envVariables).ingress().timeout();

    assertThat(timeout.connect()).isEqualTo(30000);
    assertThat(timeout.keepAlive()).isEqualTo(90);
    assertThat(timeout.idle()).isEqualTo(10);
    assertThat(timeout.readIdle()).isEqualTo(15);
    assertThat(timeout.writeIdle()).isEqualTo(20);
  }

  @Test
  void egressTimeout_positive_configurableViaEnvVariables() {
    var envVariables = Map.of(
      "WEB_CLIENT_EGRESS_TIMEOUT_CONNECT", "45000",
      "WEB_CLIENT_EGRESS_TIMEOUT_KEEP_ALIVE", "120",
      "WEB_CLIENT_EGRESS_TIMEOUT_IDLE", "11",
      "WEB_CLIENT_EGRESS_TIMEOUT_READ_IDLE", "16",
      "WEB_CLIENT_EGRESS_TIMEOUT_WRITE_IDLE", "21");

    var timeout = webClientConfig(envVariables).egress().timeout();

    assertThat(timeout.connect()).isEqualTo(45000);
    assertThat(timeout.keepAlive()).isEqualTo(120);
    assertThat(timeout.idle()).isEqualTo(11);
    assertThat(timeout.readIdle()).isEqualTo(16);
    assertThat(timeout.writeIdle()).isEqualTo(21);
  }

  private static WebClientConfig webClientConfig(Map<String, String> envVariables) {
    var builder = new SmallRyeConfigBuilder()
      .withMapping(WebClientConfig.class)
      .addDefaultInterceptors()
      .withSources(applicationPropertiesSource());
    if (!envVariables.isEmpty()) {
      builder.withSources(new PropertiesConfigSource(envVariables, "web-client-timeout-env-variables", 300));
    }
    return builder.build().getConfigMapping(WebClientConfig.class);
  }

  private static PropertiesConfigSource applicationPropertiesSource() {
    var properties = new Properties();
    var path = Path.of("src", "main", "resources", "application.properties");
    try (InputStream inputStream = Files.newInputStream(path)) {
      properties.load(inputStream);
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to read application.properties", e);
    }
    var source = new HashMap<String, String>();
    for (var entry : properties.entrySet()) {
      source.put((String) entry.getKey(), (String) entry.getValue());
    }
    return new PropertiesConfigSource(source, "application.properties", 250);
  }
}
