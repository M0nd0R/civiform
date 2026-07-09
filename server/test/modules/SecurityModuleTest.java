package modules;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import auth.FakeAdminClient;
import auth.GuestClient;
import auth.ProfileFactory;
import com.google.common.collect.ImmutableMap;
import com.typesafe.config.ConfigFactory;
import org.junit.Test;
import org.pac4j.core.client.Client;
import org.pac4j.core.client.Clients;
import org.pac4j.http.client.direct.DirectBasicAuthClient;
import play.Environment;
import play.Mode;
import services.DeploymentType;

public class SecurityModuleTest {

  @Test
  public void provideClients_prodWithDefaultLocalhostBaseUrl_doesNotRegisterFakeAdminClient() {
    FakeAdminClient fakeAdminClient = fakeAdminClient("localhost");
    Clients clients =
        clientsFor(
            Mode.PROD,
            "http://localhost:9000",
            "",
            new DeploymentType(/* isDev= */ false, /* isStaging= */ false),
            fakeAdminClient);

    assertThat(hasClient(clients, fakeAdminClient)).isFalse();
  }

  @Test
  public void provideClients_devWithLocalhostBaseUrl_registersFakeAdminClient() {
    FakeAdminClient fakeAdminClient = fakeAdminClient("localhost");
    Clients clients =
        clientsFor(
            Mode.DEV,
            "http://localhost:9000",
            "",
            new DeploymentType(/* isDev= */ true, /* isStaging= */ false),
            fakeAdminClient);

    assertThat(hasClient(clients, fakeAdminClient)).isTrue();
  }

  @Test
  public void provideClients_stagingWithMatchingBaseUrl_registersFakeAdminClient() {
    FakeAdminClient fakeAdminClient = fakeAdminClient("staging.example.com");
    Clients clients =
        clientsFor(
            Mode.PROD,
            "https://staging.example.com",
            "staging.example.com",
            new DeploymentType(/* isDev= */ false, /* isStaging= */ true),
            fakeAdminClient);

    assertThat(hasClient(clients, fakeAdminClient)).isTrue();
  }

  @Test
  public void provideClients_stagingWithNonMatchingHost_doesNotRegisterFakeAdminClient() {
    FakeAdminClient fakeAdminClient = fakeAdminClient("staging.example.com");
    Clients clients =
        clientsFor(
            Mode.PROD,
            "https://production.example.com",
            "staging.example.com",
            new DeploymentType(/* isDev= */ false, /* isStaging= */ true),
            fakeAdminClient);

    assertThat(hasClient(clients, fakeAdminClient)).isFalse();
  }

  private static FakeAdminClient fakeAdminClient(String stagingHostname) {
    return new FakeAdminClient(
        mock(ProfileFactory.class),
        ConfigFactory.parseMap(ImmutableMap.of("staging_hostname", stagingHostname)));
  }

  private static Clients clientsFor(
      Mode mode,
      String baseUrl,
      String stagingHostname,
      DeploymentType deploymentType,
      FakeAdminClient fakeAdminClient) {
    SecurityModule module =
        new SecurityModule(
            new Environment(mode),
            ConfigFactory.parseMap(
                ImmutableMap.of("base_url", baseUrl, "staging_hostname", stagingHostname)));

    return module.provideClients(
        mock(GuestClient.class),
        /* applicantAuthClient= */ null,
        /* adminAuthClient= */ null,
        fakeAdminClient,
        mock(DirectBasicAuthClient.class),
        deploymentType);
  }

  private static boolean hasClient(Clients clients, Client expectedClient) {
    return clients.getClients().contains(expectedClient);
  }
}
