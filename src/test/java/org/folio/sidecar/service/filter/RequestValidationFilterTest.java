package org.folio.sidecar.service.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.sidecar.service.filter.IngressFilterOrder.REQUEST_VALIDATION;
import static org.folio.sidecar.support.TestConstants.TENANT_NAME;
import static org.mockito.Mockito.when;

import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;
import jakarta.ws.rs.BadRequestException;
import java.util.Set;
import org.folio.sidecar.integration.okapi.OkapiHeaders;
import org.folio.support.types.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class RequestValidationFilterTest {

  @Mock private RoutingContext routingContext;
  @Mock private HttpServerRequest request;
  @Mock private MultiMap headers;

  private final RequestValidationFilter requestValidationFilter = new RequestValidationFilter();

  @Test
  void filter_positive_noBodyForGetRequestWhenContentLengthDoesNotExist() {
    when(routingContext.request()).thenReturn(request);
    when(routingContext.request().method()).thenReturn(HttpMethod.GET);
    when(request.headers()).thenReturn(headers);
    when(headers.contains("Content-Length")).thenReturn(false);
    when(headers.names()).thenReturn(Set.of());

    var result = requestValidationFilter.filter(routingContext);

    assertThat(result.succeeded()).isTrue();
  }

  @Test
  void filter_positive_noBodyForGetRequestWhenContentLengthIsZero() {
    when(routingContext.request()).thenReturn(request);
    when(routingContext.request().method()).thenReturn(HttpMethod.GET);
    when(request.headers()).thenReturn(headers);
    when(headers.contains("Content-Length")).thenReturn(true);
    when(headers.get("Content-Length")).thenReturn("0");
    when(headers.names()).thenReturn(Set.of());

    var result = requestValidationFilter.filter(routingContext);

    assertThat(result.succeeded()).isTrue();
  }

  @Test
  void filter_positive_noDuplicatedOkapiHeaders() {
    var multiMap = MultiMap.caseInsensitiveMultiMap()
      .add(OkapiHeaders.TENANT, TENANT_NAME)
      .add(OkapiHeaders.TOKEN, "token");

    when(routingContext.request()).thenReturn(request);
    when(request.headers()).thenReturn(multiMap);

    var result = requestValidationFilter.filter(routingContext);

    assertThat(result.succeeded()).isTrue();
  }

  @Test
  void filter_positive_duplicatedNonOkapiHeader() {
    var multiMap = MultiMap.caseInsensitiveMultiMap()
      .add(OkapiHeaders.TENANT, TENANT_NAME)
      .add(OkapiHeaders.AUTHORIZATION, "Bearer token")
      .add(OkapiHeaders.AUTHORIZATION, "Bearer duplicated");

    when(routingContext.request()).thenReturn(request);
    when(request.headers()).thenReturn(multiMap);

    var result = requestValidationFilter.filter(routingContext);

    assertThat(result.succeeded()).isTrue();
  }

  @Test
  void filter_negative_bodyPresentForGetRequest() {
    when(routingContext.request()).thenReturn(request);
    when(routingContext.request().method()).thenReturn(HttpMethod.GET);
    when(request.headers()).thenReturn(headers);
    when(headers.contains("Content-Length")).thenReturn(true);
    when(headers.get("Content-Length")).thenReturn("1");

    var result = requestValidationFilter.filter(routingContext);

    assertThat(result.failed()).isTrue();
    assertThat(result.cause())
      .isInstanceOf(BadRequestException.class)
      .hasMessage("GET requests should not have a body");
  }

  @Test
  void filter_negative_duplicatedOkapiHeaderWithSameValue() {
    var multiMap = MultiMap.caseInsensitiveMultiMap()
      .add(OkapiHeaders.TENANT, TENANT_NAME)
      .add(OkapiHeaders.TENANT, TENANT_NAME);

    when(routingContext.request()).thenReturn(request);
    when(request.headers()).thenReturn(multiMap);

    var result = requestValidationFilter.filter(routingContext);

    assertThat(result.failed()).isTrue();
    assertThat(result.cause())
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Request contains duplicate x-okapi-* headers: " + OkapiHeaders.TENANT);
  }

  @Test
  void filter_negative_duplicatedOkapiHeaderWithDifferentValues() {
    var multiMap = MultiMap.caseInsensitiveMultiMap()
      .add(OkapiHeaders.TOKEN, "token")
      .add(OkapiHeaders.TENANT, TENANT_NAME)
      .add(OkapiHeaders.TENANT, "duplicated");

    when(routingContext.request()).thenReturn(request);
    when(request.headers()).thenReturn(multiMap);

    var result = requestValidationFilter.filter(routingContext);

    assertThat(result.failed()).isTrue();
    assertThat(result.cause())
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Request contains duplicate x-okapi-* headers: " + OkapiHeaders.TENANT);
  }

  @Test
  void filter_negative_duplicatedOkapiHeaderWithDifferentCase() {
    var multiMap = MultiMap.caseInsensitiveMultiMap()
      .add("X-Okapi-Tenant", TENANT_NAME)
      .add("x-okapi-tenant", "duplicated");

    when(routingContext.request()).thenReturn(request);
    when(request.headers()).thenReturn(multiMap);

    var result = requestValidationFilter.filter(routingContext);

    assertThat(result.failed()).isTrue();
    assertThat(result.cause())
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Request contains duplicate x-okapi-* headers: X-Okapi-Tenant");
  }

  @Test
  void filter_negative_multipleDuplicatedOkapiHeaders() {
    var multiMap = MultiMap.caseInsensitiveMultiMap()
      .add(OkapiHeaders.TENANT, TENANT_NAME)
      .add(OkapiHeaders.TENANT, "duplicated")
      .add(OkapiHeaders.TOKEN, "token")
      .add(OkapiHeaders.TOKEN, "duplicated-token");

    when(routingContext.request()).thenReturn(request);
    when(request.headers()).thenReturn(multiMap);

    var result = requestValidationFilter.filter(routingContext);

    assertThat(result.failed()).isTrue();
    assertThat(result.cause())
      .isInstanceOf(BadRequestException.class)
      .hasMessageContaining("Request contains duplicate x-okapi-* headers:")
      .hasMessageContaining(OkapiHeaders.TENANT)
      .hasMessageContaining(OkapiHeaders.TOKEN);
  }

  @Test
  void getOrder_positive() {
    var order = requestValidationFilter.getOrder();

    assertThat(order).isEqualTo(REQUEST_VALIDATION.getOrder());
  }
}
