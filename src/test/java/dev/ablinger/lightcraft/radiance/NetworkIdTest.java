package dev.ablinger.lightcraft.radiance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Unit tests of the {@link NetworkId}: the identity every node persists and the SavedData
 * keys its networks by, so it must be unique per network and value-based in its equality.
 */
class NetworkIdTest {

  @Test
  void eachRandomIdIsItsOwnNetwork() {
    assertNotEquals(NetworkId.random(), NetworkId.random());
  }

  @Test
  void idsOfTheSameUuidAreEqualAndDifferentUuidsAreNot() {
    UUID value = UUID.randomUUID();

    assertEquals(new NetworkId(value), new NetworkId(value));
    assertEquals(new NetworkId(value).hashCode(), new NetworkId(value).hashCode());
    assertNotEquals(new NetworkId(value), new NetworkId(UUID.randomUUID()));
  }

  @Test
  void theIdCarriesItsUuid() {
    UUID value = UUID.randomUUID();

    assertEquals(value, new NetworkId(value).value());
  }
}
