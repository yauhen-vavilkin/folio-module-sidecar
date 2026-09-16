package org.folio.sidecar.service.token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.when;

import com.github.benmanes.caffeine.cache.Cache;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import org.folio.sidecar.configuration.properties.TokenCacheProperties;
import org.folio.sidecar.integration.keycloak.model.TokenResponse;
import org.folio.support.types.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class TokenCacheFactoryTest {

  private static final String TENANT = "test_tenant";

  @Mock private TokenCacheProperties cacheProperties;
  @InjectMocks private TokenCacheFactory factory;

  @Test
  void createCache_positive() {
    when(cacheProperties.getInitialCapacity()).thenReturn(10);
    when(cacheProperties.getMaxCapacity()).thenReturn(50);

    var actual = factory.createCache(refreshFunction());
    assertThat(actual).isNotNull();
  }

  @Test
  void createCache_positive_withoutRefresh() {
    when(cacheProperties.getInitialCapacity()).thenReturn(10);
    when(cacheProperties.getMaxCapacity()).thenReturn(50);

    var actual = factory.createCache();
    assertThat(actual).isNotNull();
  }

  @Test
  void createCache_positive_earlyExpiration() {
    var cache = tokenCacheFactory(60).createCache();

    cache.put(TENANT, token(3600L));

    assertThat(expiresAfter(cache, TENANT, TimeUnit.SECONDS)).isCloseTo(3540L, within(1L));
  }

  @Test
  void createCache_positive_fallbackToNinetyPercentTtl() {
    var cache = tokenCacheFactory(3595).createCache();

    cache.put(TENANT, token(3600L));

    assertThat(expiresAfter(cache, TENANT, TimeUnit.SECONDS)).isCloseTo(3240L, within(1L));
  }

  @Test
  void createCache_positive_fallbackToNinetyPercentTtl_subSecondTtl() {
    var cache = tokenCacheFactory(1).createCache();

    cache.put(TENANT, token(1L));

    assertThat(expiresAfter(cache, TENANT, TimeUnit.MILLISECONDS)).isCloseTo(900L, within(1L));
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

  private static TokenCacheFactory tokenCacheFactory(int refreshBeforeExpirySeconds) {
    return new TokenCacheFactory(new TokenCacheProperties(10, 50, refreshBeforeExpirySeconds, 30));
  }

  private static long expiresAfter(Cache<String, TokenResponse> cache, String key, TimeUnit unit) {
    return cache.policy().expireVariably().orElseThrow().getExpiresAfter(key, unit).orElseThrow();
  }

  private static TokenResponse token(Long expiresIn) {
    return new TokenResponse("access-token", "refresh-token", expiresIn);
  }
}
