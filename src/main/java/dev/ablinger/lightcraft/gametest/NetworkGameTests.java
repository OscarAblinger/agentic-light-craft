package dev.ablinger.lightcraft.gametest;

import dev.ablinger.lightcraft.radiance.NetworkId;
import dev.ablinger.lightcraft.radiance.NetworkNode;
import dev.ablinger.lightcraft.radiance.PowerNetwork;
import dev.ablinger.lightcraft.radiance.PowerNetworkManager;
import dev.ablinger.lightcraft.radiance.PowerNetworkSavedData;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;

/**
 * Game tests of the network core: the binding of nodes to the networks persisted per level.
 *
 * <p>No network block exists yet (the transport node is the next story), so the tests speak
 * to the manager directly with minimal node doubles - exactly what a block entity's load path
 * will do once it has one.
 */
public final class NetworkGameTests {

  private NetworkGameTests() {
  }

  /**
   * Loads a node that carries a {@link NetworkId} nothing has heard of yet: the manager must
   * create that network in the level's SavedData and bind the node to it. Loading the node
   * again rebinds it to the very same network, and a second node carrying the same id joins
   * that network instead of forming a new one.
   */
  public static void nodeLoadRebindsToSavedDataNetwork(GameTestHelper helper) {
    ServerLevel level = helper.getLevel();
    PowerNetworkManager manager = PowerNetworkManager.get(level);

    NetworkId id = NetworkId.random();
    PowerNetwork network = manager.rebind(new TestNode(id));

    helper.assertValueEqual(id, network.id(), "network of the loaded node");
    helper.assertTrue(PowerNetworkSavedData.get(level).networks().containsKey(id),
        "the loaded node's network exists in the SavedData");

    // Reloading the node rebinds to the same network instead of creating a second one.
    helper.assertValueEqual(network, manager.rebind(new TestNode(id)), "network after a reload");
    // A sibling node persisted with the same NetworkId joins that very network.
    helper.assertValueEqual(network, manager.rebind(new TestNode(id)), "network of a sibling node");
    helper.succeed();
  }

  /** The two identities a node must carry for the manager - what a block entity will persist. */
  private record TestNode(UUID nodeId, NetworkId networkId) implements NetworkNode {

    TestNode(NetworkId networkId) {
      this(UUID.randomUUID(), networkId);
    }
  }
}
