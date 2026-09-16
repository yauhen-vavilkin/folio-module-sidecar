package org.folio.sidecar.service.token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Policy.VarExpiration;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import org.folio.sidecar.configuration.properties.TokenCacheProperties;
import org.folio.sidecar.integration.keycloak.model.TokenResponse;
import org.folio.support.types.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class TokenCacheFactoryTest {

  @Mock private TokenCacheProperties cacheProperties;
  @InjectMocks private TokenCacheFactory factory;

  @Test
  void createCache_positive() {
    when(cacheProperties.getInitialCapacity()).thenReturn(10);
    when(cacheProperties.getMaxCapacity()).thenReturn(50);

    var actual = factory.createCache(refreshFunction());
    assertThat(actual).isNotNull();
  }

  @ParameterizedTest(name = "expiresIn = {0}, refreshBeforeExpiry = {1}, expectedTtlSec = {2}")
  @CsvSource({
    "3600, 60, 3540",   // standard early expiration is preserved
    "3600, 3595, 3240", // near-expiry fallback uses 90% of token TTL
    "60, 30, 54"        // early expiration equal to the threshold falls back to 90% of token TTL
  })
  void createCache_positive_calculateTtl(int expiresIn, int refreshBeforeExpiry, long expectedTtlSec) {
    when(cacheProperties.getInitialCapacity()).thenReturn(10);
    when(cacheProperties.getMaxCapacity()).thenReturn(50);
    when(cacheProperties.getRefreshBeforeExpirySeconds()).thenReturn(refreshBeforeExpiry);

    Cache<String, TokenResponse> cache = factory.createCache();
    var token = new TokenResponse("access-token", "refresh-token", (long) expiresIn);
    cache.put("test-tenant", token);

    VarExpiration<String, TokenResponse> expiryPolicy = cache.policy().expireVariably().orElseThrow();
    var actualTtlSec = expiryPolicy.getExpiresAfter("test-tenant", TimeUnit.SECONDS).orElseThrow();

    assertThat(actualTtlSec).isBetween(expectedTtlSec - 1, expectedTtlSec);
  }

  @Test
  void createCache_positive_withoutRefresh() {
    when(cacheProperties.getInitialCapacity()).thenReturn(10);
    when(cacheProperties.getMaxCapacity()).thenReturn(50);

    var actual = factory.createCache();
    assertThat(actual).isNotNull();
  }

  @Test
  void constructor_validation_negative_nullObject() {
    assertThatThrownBy(() -> new TokenCacheFactory(null))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("Token cache properties must be provided");
  }

  @Test
  void constructor_validation_negative_nullInitialCapacity() {
    var cacheProperties = new TokenCacheProperties();
    cacheProperties.setMaxCapacity(10);
    cacheProperties.setRefreshBeforeExpirySeconds(10);
    assertThatThrownBy(() -> new TokenCacheFactory(cacheProperties))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("Token cache initial capacity must be set");
  }

  @Test
  void constructor_validation_negative_nullMaxCapacity() {
    var cacheProperties = new TokenCacheProperties();
    cacheProperties.setInitialCapacity(10);
    cacheProperties.setRefreshBeforeExpirySeconds(10);
    assertThatThrownBy(() -> new TokenCacheFactory(cacheProperties))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("Token cache max capacity must be set");
  }

  @Test
  void constructor_validation_negative_nullRefreshBeforeExpirySeconds() {
    var cacheProperties = new TokenCacheProperties();
    cacheProperties.setInitialCapacity(10);
    cacheProperties.setMaxCapacity(10);
    assertThatThrownBy(() -> new TokenCacheFactory(cacheProperties))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("Token cache refresh before expiry must be set");
  }

  private static BiConsumer<String, TokenResponse> refreshFunction() {
    return (tenant, token) -> {};
  }
}
