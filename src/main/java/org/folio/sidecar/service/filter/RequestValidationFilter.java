package org.folio.sidecar.service.filter;

import static io.vertx.core.Future.failedFuture;
import static io.vertx.core.Future.succeededFuture;
import static org.folio.sidecar.service.filter.IngressFilterOrder.REQUEST_VALIDATION;

import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.BadRequestException;
import java.util.List;
import java.util.Locale;
import org.folio.sidecar.integration.okapi.OkapiHeaders;

@ApplicationScoped
public class RequestValidationFilter implements IngressRequestFilter {

  private static final String CONTENT_LENGTH_HEADER = "Content-Length";
  private static final String OKAPI_HEADERS_PREFIX = OkapiHeaders.PREFIX.toLowerCase(Locale.ROOT) + '-';

  @Override
  public Future<RoutingContext> filter(RoutingContext rc) {
    var headers = rc.request().headers();
    if (HttpMethod.GET.equals(rc.request().method()) && isContentLengthByHeaderExist(headers)) {
      return failedFuture(new BadRequestException("GET requests should not have a body"));
    }

    var duplicatedHeaders = findDuplicatedOkapiHeaders(headers);
    if (!duplicatedHeaders.isEmpty()) {
      return failedFuture(new BadRequestException(
        "Request contains duplicate x-okapi-* headers: " + String.join(", ", duplicatedHeaders)));
    }

    return succeededFuture(rc);
  }

  @Override
  public int getOrder() {
    return REQUEST_VALIDATION.getOrder();
  }

  private List<String> findDuplicatedOkapiHeaders(MultiMap headers) {
    return headers.names().stream()
      .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(OKAPI_HEADERS_PREFIX))
      .filter(name -> headers.getAll(name).size() > 1)
      .toList();
  }

  private boolean isContentLengthByHeaderExist(MultiMap headers) {
    if (headers.contains(CONTENT_LENGTH_HEADER)) {
      var length = headers.get(CONTENT_LENGTH_HEADER);
      return Integer.parseInt(length) > 0;
    }
    return false;
  }
}
