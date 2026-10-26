package dev.ablinger.lightcraft.radiance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * The central, server-side object owning a network's shared state: the pooled intensity, the
 * capacity its storage contributes, and the live colour (design report 4.2, 4.3).
 *
 * <p>One network has exactly one colour at any moment, derived live from the colours being
 * produced plus the colorizer contributions, floored at {@link Radiance#DEFAULT_COLOUR}
 * (design report 4.13). It is never persisted per node and never stored at all: colour rises
 * while a strongly coloured producer runs and falls back to the dim anchor when it stops.
 *
 * <p>Per tick the network runs one declare-then-allocate step (design report 4.12): block
 * entities declare with {@link #offer(Radiance, PowerProducer)} and
 * {@link #request(long, PowerConsumer)} during their own ticks, in any order, and
 * {@link #tick()} - called once by the {@link PowerNetworkManager} after every declaration is
 * in - shares the pool through the current {@link DistributionStrategy}, delivers the grants
 * and stores what is left. Networks without declarations stay dormant and cost nothing.
 */
public final class PowerNetwork {

  private final NetworkId id;

  /** Capacity contributions of the network's storage nodes, keyed by node id (persisted). */
  private final Map<UUID, Long> storages = new LinkedHashMap<>();

  /** Fixed colour contributions of colorizer nodes, keyed by node id (persisted). */
  private final Map<UUID, Radiance> colorizers = new LinkedHashMap<>();

  /** The offers declared since the last allocation, in declaration order (never persisted). */
  private final Map<PowerProducer, Radiance> offers = new LinkedHashMap<>();

  /** The requests declared since the last allocation, in declaration order (never persisted). */
  private final Map<PowerConsumer, Long> requests = new LinkedHashMap<>();

  private long stored;
  private long capacity;
  private DistributionStrategy strategy = DistributionStrategy.MAX_MIN_FAIR;

  /** Cached live colour; {@code null} when it needs recomputing from the current sources. */
  private Radiance colour;
  private boolean active;

  /** The manager this network is tracked by; {@code null} while detached (unit tests). */
  private PowerNetworkManager manager;

  public PowerNetwork(NetworkId id) {
    this.id = Objects.requireNonNull(id, "id");
  }

  public NetworkId id() {
    return id;
  }

  /**
   * The network's live colour: the component-wise highest of the colours currently being
   * produced and of every colorizer contribution, floored at #010101 (design report 4.13).
   * The intensity of the returned value is always zero - the pool size is {@link #intensity()}.
   */
  public Radiance colour() {
    if (colour == null) {
      int red = 0;
      int green = 0;
      int blue = 0;
      for (Radiance source : colorizers.values()) {
        red = Math.max(red, source.red255());
        green = Math.max(green, source.green255());
        blue = Math.max(blue, source.blue255());
      }
      for (Radiance offer : offers.values()) {
        red = Math.max(red, offer.red255());
        green = Math.max(green, offer.green255());
        blue = Math.max(blue, offer.blue255());
      }
      colour = new Radiance(
          (byte) Math.max(red, Radiance.DEFAULT_COLOUR.red255()),
          (byte) Math.max(green, Radiance.DEFAULT_COLOUR.green255()),
          (byte) Math.max(blue, Radiance.DEFAULT_COLOUR.blue255()),
          0L);
    }
    return colour;
  }

  /** The stored pool, the intensity the network holds right now. */
  public long intensity() {
    return stored;
  }

  /** The network's capacity: the sum of its storage contributions, with no ceiling. */
  public long capacity() {
    return capacity;
  }

  public DistributionStrategy strategy() {
    return strategy;
  }

  /** Replaces the distribution strategy; a hub later uses this to change how the pool shares. */
  public void setStrategy(DistributionStrategy strategy) {
    this.strategy = Objects.requireNonNull(strategy, "strategy");
  }

  /** Whether this network has declarations to allocate, i.e. is worth ticking. */
  public boolean isActive() {
    return active;
  }

  /**
   * Declares an offer for this tick: how much light a producer brings in, at which colour.
   * Nothing moves yet; {@link #tick()} adds the offers to the pool. Offers of zero intensity
   * are ignored - a colour-only source is a colorizer, not production.
   */
  public void offer(Radiance offer, PowerProducer source) {
    Objects.requireNonNull(offer, "offer");
    Objects.requireNonNull(source, "source");
    if (offer.intensity() <= 0L) {
      return;
    }
    Radiance previous = offers.get(source);
    offers.put(source, previous == null
        ? offer
        : previous.withIntensity(previous.intensity() + offer.intensity()));
    colour = null;
    wake();
  }

  /** Declares a demand for this tick. The grant arrives through the sink's next callback. */
  public void request(long amount, PowerConsumer sink) {
    Objects.requireNonNull(sink, "sink");
    if (amount <= 0L) {
      return;
    }
    requests.merge(sink, amount, Long::sum);
    wake();
  }

  /**
   * Sets the capacity contribution of a storage node. Calling it again with the same amount -
   * as a block entity reload does - replaces the previous contribution instead of doubling it.
   */
  public void addStorage(NetworkNode node, long contribution) {
    Objects.requireNonNull(node, "node");
    putStorage(node.nodeId(), contribution);
    dataChanged();
  }

  /** Drops a storage node's capacity contribution, e.g. when it is broken or unlinked. */
  public void removeStorage(NetworkNode node) {
    Objects.requireNonNull(node, "node");
    Long previous = storages.remove(node.nodeId());
    if (previous != null) {
      capacity -= previous;
      dataChanged();
    }
  }

  /** Sets a colorizer node's fixed colour contribution, a permanent source next to production. */
  public void addColorizer(NetworkNode node, Radiance contribution) {
    Objects.requireNonNull(node, "node");
    putColorizer(node.nodeId(), Objects.requireNonNull(contribution, "contribution"));
    colour = null;
    dataChanged();
  }

  /** Drops a colorizer node's colour contribution. */
  public void removeColorizer(NetworkNode node) {
    Objects.requireNonNull(node, "node");
    if (colorizers.remove(node.nodeId()) != null) {
      colour = null;
      dataChanged();
    }
  }

  /**
   * The one allocation step of the tick (design report 4.12): the pool is stored intensity
   * plus this tick's offers, shared across the declared requests by the strategy; the grants
   * are delivered, and what the network cannot hold afterwards is discarded.
   */
  void tick() {
    long available = stored;
    for (Radiance offer : offers.values()) {
      available += offer.intensity();
    }

    Map<PowerConsumer, Long> grants = requests.isEmpty()
        ? Map.of()
        : strategy.allocate(available, requests);

    // The declarations of this tick are spent. The network goes back to sleep unless somebody
    // declares again while the grants are delivered.
    requests.clear();
    active = false;

    long granted = 0L;
    for (Map.Entry<PowerConsumer, Long> grant : grants.entrySet()) {
      if (grant.getValue() > 0L) {
        grant.getKey().onGranted(grant.getValue());
        granted += grant.getValue();
      }
    }

    // The colour stays live through the delivery - FE adapters convert their grants at it -
    // and falls back to the colorizer contributions once production of this tick is over.
    offers.clear();
    colour = null;

    stored = Math.min(capacity, available - granted);
    dataChanged();
  }

  /**
   * Absorbs another network into this one, the merge half of linking (design report 4.11):
   * intensities sum, capacity sums so nothing is clamped, and the live colour becomes the
   * component-wise highest of both sides' sources. Merges are never refused for colour.
   * The absorbed network is left empty and detached; the manager drops it from the SavedData.
   */
  void absorb(PowerNetwork other) {
    stored += other.stored;
    other.storages.forEach(this::putStorage);
    colorizers.putAll(other.colorizers);
    offers.putAll(other.offers);
    requests.putAll(other.requests);
    active = active || other.active;

    other.stored = 0L;
    other.storages.clear();
    other.capacity = 0L;
    other.colorizers.clear();
    other.offers.clear();
    other.requests.clear();
    other.colour = null;
    other.active = false;
    other.manager = null;

    colour = null;
    if (active) {
      wake();
    } else {
      dataChanged();
    }
  }

  /**
   * Divides a stored intensity across the capacities the split results in (design report
   * 4.11): each part gets its capacity's proportional share - 9,000R over capacities
   * 10,000R/20,000R become 3,000R/6,000R - and an all-zero capacity divides evenly. The
   * remainder of the division goes one R at a time to the largest capacities, so the total is
   * never reduced by rounding.
   *
   * @return one portion per given capacity, in the same order
   */
  public static long[] divideProportionally(long stored, long... capacities) {
    if (capacities.length == 0) {
      return new long[0];
    }
    long positiveStored = Math.max(0L, stored);
    long totalCapacity = 0L;
    for (long contribution : capacities) {
      totalCapacity += Math.max(0L, contribution);
    }

    long[] portions = new long[capacities.length];
    long assigned = 0L;
    for (int index = 0; index < capacities.length; index++) {
      portions[index] = totalCapacity > 0L
          ? positiveStored * Math.max(0L, capacities[index]) / totalCapacity
          : positiveStored / capacities.length;
      assigned += portions[index];
    }

    // Largest capacity first for the remainder; the stable sort keeps declaration order on ties.
    List<Integer> byCapacity = new ArrayList<>(capacities.length);
    for (int index = 0; index < capacities.length; index++) {
      byCapacity.add(index);
    }
    byCapacity.sort((left, right) ->
        Long.compare(Math.max(0L, capacities[right]), Math.max(0L, capacities[left])));
    for (int handed = 0; handed < positiveStored - assigned; handed++) {
      portions[byCapacity.get(handed)]++;
    }
    return portions;
  }

  void attachManager(PowerNetworkManager manager) {
    this.manager = manager;
  }

  /** The stored pool, restored from the SavedData on load or handed over by a merge/split. */
  void setStored(long amount) {
    stored = Math.max(0L, amount);
    dataChanged();
  }

  /** Restores the persisted state of this network verbatim; the SavedData is the source. */
  void restore(long stored, Map<UUID, Long> storages, Map<UUID, Radiance> colorizers) {
    this.stored = Math.max(0L, stored);
    this.storages.putAll(storages);
    this.colorizers.putAll(colorizers);
    capacity = 0L;
    for (long contribution : storages.values()) {
      capacity += contribution;
    }
  }

  Map<UUID, Long> storages() {
    return Collections.unmodifiableMap(storages);
  }

  Map<UUID, Radiance> colorizers() {
    return Collections.unmodifiableMap(colorizers);
  }

  private void putStorage(UUID nodeId, long contribution) {
    Long previous = storages.put(nodeId, contribution);
    capacity += contribution - (previous == null ? 0L : previous);
  }

  private void putColorizer(UUID nodeId, Radiance contribution) {
    colorizers.put(nodeId, contribution.withIntensity(0L));
  }

  private void wake() {
    active = true;
    if (manager != null) {
      manager.activate(this);
    }
  }

  private void dataChanged() {
    if (manager != null) {
      manager.markDirty();
    }
  }
}
