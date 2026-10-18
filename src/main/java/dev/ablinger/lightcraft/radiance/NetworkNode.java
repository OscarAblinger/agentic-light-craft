package dev.ablinger.lightcraft.radiance;

import java.util.UUID;

/**
 * Any member of a power network: transport nodes, storage, generators, light machines,
 * emitters, linked FE machines and the hub are all nodes (design report 4.1).
 *
 * <p>A node belongs to <em>exactly one</em> network at any time; membership changes go only
 * through {@link PowerNetworkManager}, which merges or splits the networks themselves, so no
 * node is ever left in two.
 *
 * <p>Block entities own their node: they persist both identities in their NBT and call
 * {@link PowerNetworkManager#rebind(NetworkNode)} on load, which creates the named network
 * when it is missing from the {@code SavedData}.
 */
public interface NetworkNode {

  /**
   * Stable identity of this node itself, independent of its position. Persisted in the block
   * entity NBT; required so per-node contributions (storage capacity, colour) survive the
   * node being unloaded and reloaded.
   */
  UUID nodeId();

  /** The network this node belongs to. */
  NetworkId networkId();
}
