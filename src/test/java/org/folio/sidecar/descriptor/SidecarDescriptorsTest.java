package org.folio.sidecar.descriptor;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.folio.support.types.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
class SidecarDescriptorsTest {

  private static final Path DESCRIPTORS = Path.of("src/main/descriptors");
  private static final ObjectMapper MAPPER = new ObjectMapper();

  @Test
  void moduleDescriptor_positive_declaresOnlyConsumedEvents() throws IOException {
    var md = MAPPER.readTree(DESCRIPTORS.resolve("ModuleDescriptor.json").toFile());

    assertThat(md.has("provides")).isFalse();
    assertThat(md.has("permissionSets")).isFalse();
    assertThat(md.has("producesEvents")).isFalse();
    assertThat(ids(md.get("requires"), "id"))
      .containsExactlyInAnyOrder("mgr-applications", "mgr-tenant-entitlements", "mgr-tenants");

    var events = md.get("consumesEvents");
    assertThat(ids(events, "eventName")).containsExactly("discovery", "entitlement", "mod-login-keycloak.logout");
    assertThat(events.get(0).has("optional")).isFalse();
    assertThat(events.get(1).has("optional")).isFalse();
    assertThat(events.get(2).get("optional").asBoolean()).isTrue();
  }

  @Test
  void applicationDescriptor_positive_wrapsModuleDescriptorWithMatchingVersion() throws IOException {
    var md = MAPPER.readTree(DESCRIPTORS.resolve("ModuleDescriptor.json").toFile());
    var app = MAPPER.readTree(DESCRIPTORS.resolve("ApplicationDescriptor.json").toFile());

    assertThat(app.get("name").asText()).isEqualTo("app-sidecar");
    assertThat(app.has("scope")).isFalse();
    assertThat(app.get("id").asText()).isEqualTo("app-sidecar-" + app.get("version").asText());
    assertThat(app.get("modules")).hasSize(1);
    assertThat(app.get("modules").get(0).get("id").asText()).isEqualTo(md.get("id").asText());
  }

  private static List<String> ids(JsonNode array, String field) {
    var result = new ArrayList<String>();
    array.forEach(node -> result.add(node.get(field).asText()));
    return result;
  }
}
