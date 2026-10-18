package dev.ablinger.lightcraft.radiance;

import static dev.ablinger.lightcraft.radiance.TestNodes.radiance;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

/**
 * Unit tests of the {@link PowerNetwork}: the declare-then-allocate flow of one tick, the
 * live colour composed from production and colorizers with #010101 as its floor, and the
 * merge and split rules of the design report (4.11, 4.12, 4.13).
 */
class PowerNetworkTest {

  /** #500101, a strongly red production colour. */
  private static final int RED = 0x50;
  /** #015001, a strongly green production colour. */
  private static final int GREEN = 0x50;

  private final PowerNetwork network = new PowerNetwork(NetworkId.random());
  private final TestNodes.Producer producer = new TestNodes.Producer();
  private final TestNodes.Consumer consumer = new TestNodes.Consumer();
  private final TestNodes.Node storage = new TestNodes.Node();
  private final TestNodes.Node colorizer = new TestNodes.Node();

  @Test
  void offersAndRequestsOnlyDeclareAndAreAllocatedByTheTick() {
    network.addStorage(storage, 1_000L);
    network.offer(radiance(RED, 1, 1, 100L), producer);
    network.request(60L, consumer);

    // Nothing has moved during the declarations.
    assertEquals(0, consumer.grantCount());

    network.tick();

    assertEquals(60L, consumer.lastGrant());
    assertEquals(40L, network.intensity()); // what was not requested stays in the pool
  }

  @Test
  void surplusBeyondTheCapacityIsDiscarded() {
    network.addStorage(storage, 50L);
    network.offer(radiance(255, 255, 255, 100L), producer);
    network.request(10L, consumer);

    network.tick();

    assertEquals(10L, consumer.lastGrant());
    assertEquals(50L, network.intensity()); // the pool holds its capacity, the surplus is gone
  }

  @Test
  void theStoredPoolIsPartOfWhatIsShared() {
    network.addStorage(storage, 500L);
    network.setStored(500L);
    network.request(600L, consumer);

    network.tick();

    assertEquals(500L, consumer.lastGrant()); // everything the pool held, and no more
    assertEquals(0L, network.intensity());
  }

  @Test
  void storageContributionsAreReplacedWhenAChunkReloadsThem() {
    network.addStorage(storage, 1_000L);
    network.addStorage(storage, 1_000L); // the same node re-registering must not double its capacity
    assertEquals(1_000L, network.capacity());

    network.removeStorage(storage);
    assertEquals(0L, network.capacity());
  }

  @Test
  void colourIsTheHighestOfProductionAndColorizersFlooredAtTheDimAnchor() {
    // No colour source at all: the network runs on the dim anchor #010101.
    assertEquals(Radiance.DEFAULT_COLOUR, network.colour());

    network.offer(radiance(RED, 1, 1, 10L), producer);
    assertEquals(radiance(RED, 1, 1, 0L), network.colour());

    // A colorizer joins the composition: #500101 over #015001 is #505001.
    network.addColorizer(colorizer, radiance(1, GREEN, 1, 0L));
    assertEquals(radiance(RED, GREEN, 1, 0L), network.colour());
  }

  @Test
  void colourFallsBackAfterTheTickButKeepsTheColorizers() {
    network.addColorizer(colorizer, radiance(RED, 1, 1, 0L));
    network.offer(radiance(1, 1, 0x50, 5L), producer);
    assertEquals(radiance(RED, 1, 0x50, 0L), network.colour());

    network.tick();

    // Production of that tick is over; the colorizer is a permanent source and stays.
    assertEquals(radiance(RED, 1, 1, 0L), network.colour());

    network.removeColorizer(colorizer);
    assertEquals(Radiance.DEFAULT_COLOUR, network.colour());
  }

  @Test
  void mergingSumsPoolsAndCapacitiesAndTakesTheHighestColour() {
    PowerNetwork other = new PowerNetwork(NetworkId.random());
    network.addStorage(storage, 1_000L);
    network.setStored(500L);
    network.offer(radiance(RED, 1, 1, 30L), producer);
    other.addStorage(new TestNodes.Node(), 2_000L);
    other.setStored(100L);
    other.offer(radiance(1, GREEN, 1, 20L), new TestNodes.Producer());

    network.absorb(other);

    assertEquals(600L, network.intensity()); // intensities sum
    assertEquals(3_000L, network.capacity()); // capacities sum, nothing is clamped
    assertEquals(radiance(RED, GREEN, 1, 0L), network.colour()); // component-wise maximum

    // The absorbed network is emptied and detached, ready to be dropped from the SavedData.
    assertEquals(0L, other.intensity());
    assertEquals(0L, other.capacity());
    assertFalse(other.isActive());
  }

  @Test
  void splittingDividesThePoolProportionallyToTheResultingCapacities() {
    assertArrayEquals(new long[] {3_000L, 6_000L}, PowerNetwork.divideProportionally(9_000L, 10_000L, 20_000L));
  }

  @Test
  void splittingWithoutAnyCapacityDividesEvenly() {
    assertArrayEquals(new long[] {500L, 500L}, PowerNetwork.divideProportionally(1_000L, 0L, 0L));
  }

  @Test
  void splittingKeepsTheTotalWhenTheDivisionIsUneven() {
    long[] portions = PowerNetwork.divideProportionally(1_000L, 10L, 20L);

    assertArrayEquals(new long[] {333L, 667L}, portions); // the remainder lands on the larger capacity
    assertEquals(1_000L, portions[0] + portions[1]); // and no R is destroyed by rounding
  }
}
