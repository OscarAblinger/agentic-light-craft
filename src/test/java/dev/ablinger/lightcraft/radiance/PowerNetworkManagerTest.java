package dev.ablinger.lightcraft.radiance;

import static dev.ablinger.lightcraft.radiance.TestNodes.radiance;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

/**
 * Unit tests of the {@link PowerNetworkManager}: rebind creating the named network when it is
 * missing, activation and dormancy - idle networks are not worked on - and the round-robin
 * per-tick budget (design report 4.4, 7.2).
 */
class PowerNetworkManagerTest {

  private final PowerNetworkSavedData data = new PowerNetworkSavedData();
  private final PowerNetworkManager manager = new PowerNetworkManager(data);

  @Test
  void rebindCreatesTheNetworkMissingFromTheSavedData() {
    TestNodes.Node node = new TestNodes.Node();

    PowerNetwork network = manager.rebind(node);

    // The node's persisted id names the network, which was created because it was missing.
    assertSame(network, data.networks().get(node.networkId()));
    assertSame(network, manager.network(node.networkId()));
  }

  @Test
  void rebindFindsTheNetworkThatAlreadyExists() {
    TestNodes.Node node = new TestNodes.Node();

    assertSame(manager.rebind(node), manager.rebind(node));
  }

  @Test
  void declaringWakesTheNetworkAndAnIdleTickPutsItBackToSleep() {
    TestNodes.Producer producer = new TestNodes.Producer();
    TestNodes.Consumer consumer = new TestNodes.Consumer();
    PowerNetwork network = manager.create();
    network.addStorage(new TestNodes.Node(), 1_000L);

    network.offer(radiance(1, 1, 1, 100L), producer);
    network.request(40L, consumer);
    assertEquals(1, manager.activeNetworkCount());

    manager.tick(128);

    assertEquals(40L, consumer.lastGrant());
    assertEquals(0, manager.activeNetworkCount()); // nothing declared further, so it sleeps

    // The dormant network is not worked on: its consumer got exactly one grant, not a new one.
    manager.tick(128);
    assertEquals(1, consumer.grantCount());
    assertEquals(0, manager.activeNetworkCount());
  }

  @Test
  void theBudgetSpreadsActiveNetworksAcrossTicks() {
    TestNodes.Consumer firstConsumer = new TestNodes.Consumer();
    TestNodes.Consumer secondConsumer = new TestNodes.Consumer();
    declareOn(firstConsumer);
    declareOn(secondConsumer);
    assertEquals(2, manager.activeNetworkCount());

    // A budget of one processes the first network only; the second waits in the rotation.
    manager.tick(1);
    assertEquals(1, firstConsumer.grantCount());
    assertEquals(0, secondConsumer.grantCount());
    assertEquals(1, manager.activeNetworkCount());

    manager.tick(1);
    assertEquals(1, secondConsumer.grantCount());
    assertEquals(0, manager.activeNetworkCount());
  }

  @Test
  void mergingDropsTheAbsorbedNetworkFromTheSavedData() {
    PowerNetwork first = manager.create();
    PowerNetwork second = manager.create();
    second.addStorage(new TestNodes.Node(), 300L);
    second.setStored(300L);

    manager.merge(first, second);

    assertEquals(300L, first.intensity());
    assertNull(manager.network(second.id()));
    assertFalse(data.networks().containsKey(second.id()));
  }

  /** A fresh network whose consumer requests everything the network can offer it. */
  private void declareOn(TestNodes.Consumer consumer) {
    PowerNetwork network = manager.create();
    network.addStorage(new TestNodes.Node(), 1_000L);
    network.offer(radiance(1, 1, 1, 50L), new TestNodes.Producer());
    network.request(50L, consumer);
  }
}
