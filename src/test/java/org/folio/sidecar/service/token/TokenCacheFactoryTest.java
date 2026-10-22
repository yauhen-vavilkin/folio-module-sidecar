package org.folio.sidecar.service.token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

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

  @ParameterizedTest
  @CsvSource({
    "3600, 60, 3540000",
    "3600, 3595, 3240000",
    "3600, 3570, 3240000",
    "3600, 3569, 31000",
    "1, 1, 900"
  })
  void calculateTtl_positive(long expiresIn, int refreshBefore, long expectedMillis) {
    var props = new TokenCacheProperties();
    props.setInitialCapacity(10);
    props.setMaxCapacity(10);
    props.setRefreshBeforeExpirySeconds(refreshBefore);
    var token = new TokenResponse();
    token.setExpiresIn(expiresIn);

    var ttl = new TokenCacheFactory(props).calculateTtl(token);

    assertThat(TimeUnit.NANOSECONDS.toMillis(ttl)).isEqualTo(expectedMillis);
  }

  private static BiConsumer<String, TokenResponse> refreshFunction() {
    return (tenant, token) -> {};
  }
}
