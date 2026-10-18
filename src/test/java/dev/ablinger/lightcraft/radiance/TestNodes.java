package dev.ablinger.lightcraft.radiance;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Node doubles for the network core tests: the network needs nothing from a node beyond its
 * two identities, and only consumers answer callbacks, so that is all these carry.
 */
final class TestNodes {

  private TestNodes() {
  }

  /** A node bound to a fresh network id, or to the one given. */
  static class Node implements NetworkNode {

    private final UUID nodeId = UUID.randomUUID();
    private final NetworkId networkId;

    Node() {
      this(NetworkId.random());
    }

    Node(NetworkId networkId) {
      this.networkId = networkId;
    }

    @Override
    public UUID nodeId() {
      return nodeId;
    }

    @Override
    public NetworkId networkId() {
      return networkId;
    }
  }

  /** A producer double; offering happens through the network, so nothing is remembered here. */
  static final class Producer extends Node implements PowerProducer {

    Producer() {
      super();
    }
  }

  /** A consumer double remembering every grant it is handed. */
  static final class Consumer extends Node implements PowerConsumer {

    private final List<Long> grants = new ArrayList<>();

    Consumer() {
      super();
    }

    @Override
    public void onGranted(long amount) {
      grants.add(amount);
    }

    /** How many grants were delivered, so tests can tell "not allocated yet" from "zero". */
    int grantCount() {
      return grants.size();
    }

    long lastGrant() {
      return grants.isEmpty() ? 0L : grants.get(grants.size() - 1);
    }
  }

  /** A radiance of the given colour and intensity; channels are the actual 0-255 values. */
  static Radiance radiance(int red, int green, int blue, long intensity) {
    return new Radiance((byte) red, (byte) green, (byte) blue, intensity);
  }
}
