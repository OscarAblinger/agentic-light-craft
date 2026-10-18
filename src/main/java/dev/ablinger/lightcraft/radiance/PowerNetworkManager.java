package dev.ablinger.lightcraft.radiance;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import net.minecraft.server.level.ServerLevel;

/**
 * The per-level owner of the power networks (design report 4.2): it holds the authoritative
 * {@link PowerNetworkSavedData}, is the only way nodes join, merge or split networks, and
 * drives every active network once per server tick.
 *
 * <p>Only active networks are worked on (design report 7.2): a network wakes the moment
 * somebody declares an offer or request on it and goes back to sleep in the tick it had
 * nothing left to allocate, so idle networks cost nothing. The per-tick budget keeps a spike
 * of many simultaneously active networks from landing in one tick: at most
 * {@code budget} networks are processed per tick, and the ones already awake are picked in
 * rotation - a network put back after its turn waits behind the others.
 */
public final class PowerNetworkManager {

  private final PowerNetworkSavedData data;

  /** The networks waiting for their allocation this tick, in rotation order. */
  private final Set<PowerNetwork> active = new LinkedHashSet<>();

  PowerNetworkManager(PowerNetworkSavedData data) {
    this.data = data;
    // The networks loaded from disk need their driver back.
    for (PowerNetwork network : data.networks().values()) {
      network.attachManager(this);
    }
  }

  /** The manager of a level, loading its persisted networks on first use. */
  public static PowerNetworkManager get(ServerLevel level) {
    return PowerNetworkSavedData.get(level).manager();
  }

  /** The network of the given id, or {@code null} when no node of it has loaded. */
  public PowerNetwork network(NetworkId id) {
    return data.networks().get(id);
  }

  /**
   * Rebinds a loaded node to the network its persisted {@link NetworkNode#networkId()} names,
   * creating that network when it is missing from the SavedData (design report 4.4). The
   * caller - the node's block entity - keeps its own reference to the returned network.
   */
  public PowerNetwork rebind(NetworkNode node) {
    Objects.requireNonNull(node, "node");
    NetworkId id = Objects.requireNonNull(node.networkId(),
        "A node must carry its persisted NetworkId before it can rebind");
    PowerNetwork network = data.getOrCreate(id);
    network.attachManager(this);
    return network;
  }

  /**
   * A brand-new, empty network. Nodes placed without a stored {@link NetworkId} - a first
   * placement, or one side of a fresh link - start a network this way and persist its id.
   */
  public PowerNetwork create() {
    PowerNetwork network = new PowerNetwork(NetworkId.random());
    data.put(network);
    network.attachManager(this);
    return network;
  }

  /**
   * Merges two networks, the network-side of linking two nodes (design report 4.11): the
   * second is absorbed into the first, its id is dropped from the SavedData, and the caller
   * updates the nodes of the second to name the survivor's id.
   */
  public PowerNetwork merge(PowerNetwork first, PowerNetwork second) {
    Objects.requireNonNull(first, "first");
    Objects.requireNonNull(second, "second");
    if (first != second) {
      first.absorb(second);
      active.remove(second);
      data.remove(second.id());
    }
    return first;
  }

  /**
   * The central tick driver (design report 4.12, 7.2), called once per level and server tick
   * after every block entity has declared. It allocates at most {@code budget} of the active
   * networks and puts those that declared nothing further back to sleep.
   *
   * @param budget how many networks may be processed in this tick, e.g. from the
   *        {@code network.networksPerTickBudget} config key
   */
  public void tick(int budget) {
    if (active.isEmpty() || budget <= 0) {
      return;
    }
    // The rotation: take this tick's share off the front, tick them, and re-queue the ones
    // that are still awake at the back. Networks activated meanwhile simply join the set.
    List<PowerNetwork> processed = new ArrayList<>(Math.min(budget, active.size()));
    Iterator<PowerNetwork> rotation = active.iterator();
    while (rotation.hasNext() && processed.size() < budget) {
      PowerNetwork network = rotation.next();
      rotation.remove();
      processed.add(network);
    }
    for (PowerNetwork network : processed) {
      network.tick();
      if (network.isActive()) {
        active.add(network);
      }
    }
  }

  /** How many networks currently wait for an allocation; 0 when everything is dormant. */
  public int activeNetworkCount() {
    return active.size();
  }

  void activate(PowerNetwork network) {
    active.add(network);
  }

  void markDirty() {
    data.setDirty();
  }
}
