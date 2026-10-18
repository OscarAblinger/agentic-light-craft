package dev.ablinger.lightcraft.radiance;

import java.util.Objects;
import java.util.UUID;

/**
 * Stable identity of a power network. Every node stores exactly one {@code NetworkId} in its
 * block entity NBT; the per-level {@code PowerNetworkSavedData} keeps the matching
 * {@link PowerNetwork} objects keyed by it.
 *
 * <p>The identity is a UUID rather than a position or a counter so it stays valid while blocks
 * move (see the moving-structures seam of the design report): a network survives its members
 * being unloaded, relocated or captured by a contraption.
 */
public record NetworkId(UUID value) {

  public NetworkId {
    Objects.requireNonNull(value, "value");
  }

  /** A fresh, globally unique network id, used when a new network is formed. */
  public static NetworkId random() {
    return new NetworkId(UUID.randomUUID());
  }
}
