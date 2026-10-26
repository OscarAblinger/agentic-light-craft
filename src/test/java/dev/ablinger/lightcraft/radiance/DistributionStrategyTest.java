package dev.ablinger.lightcraft.radiance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Unit tests of the default max-min fair {@link DistributionStrategy}: equal shares up to
 * demand - nobody gets more than requested, the scarce supply is shared as equally as whole R
 * allow, and no grant is ever lost to rounding.
 */
class DistributionStrategyTest {

  private final TestNodes.Consumer first = new TestNodes.Consumer();
  private final TestNodes.Consumer second = new TestNodes.Consumer();
  private final TestNodes.Consumer third = new TestNodes.Consumer();

  @Test
  void everyoneGetsExactlyTheirRequestWhenThereIsEnough() {
    Map<PowerConsumer, Long> grants = DistributionStrategy.MAX_MIN_FAIR.allocate(100L, requests(40L, 60L));

    assertEquals(List.of(40L, 60L), List.copyOf(grants.values()));
  }

  @Test
  void scarceSupplyIsSharedInEqualCappedShares() {
    // 100R for three consumers asking 60R each: as equal as whole R allow, summing to 100.
    Map<PowerConsumer, Long> grants = DistributionStrategy.MAX_MIN_FAIR.allocate(100L, requests(60L, 60L, 60L));

    assertEquals(List.of(34L, 33L, 33L), List.copyOf(grants.values()));
  }

  @Test
  void smallDemandsAreServedBeforeTheRestSharesWhatIsLeft() {
    // The first consumer is satiated with its 10R; the two big ones split the remaining 90R.
    Map<PowerConsumer, Long> grants = DistributionStrategy.MAX_MIN_FAIR.allocate(100L, requests(10L, 1_000L, 1_000L));

    assertEquals(List.of(10L, 45L, 45L), List.copyOf(grants.values()));
  }

  @Test
  void nobodyEverGetsMoreThanRequested() {
    Map<PowerConsumer, Long> grants = DistributionStrategy.MAX_MIN_FAIR.allocate(1_000_000L, requests(1L, 2L, 3L));

    assertEquals(List.of(1L, 2L, 3L), List.copyOf(grants.values()));
  }

  @Test
  void anEmptyPoolGrantsNothing() {
    Map<PowerConsumer, Long> grants = DistributionStrategy.MAX_MIN_FAIR.allocate(0L, requests(50L, 50L));

    assertTrue(grants.isEmpty());
  }

  @Test
  void theRemainderOfTheEqualSharesIsNeverLost() {
    // 10R among three consumers asking 100R each: 4+3+3, not 3+3+3 with an R destroyed.
    Map<PowerConsumer, Long> grants = DistributionStrategy.MAX_MIN_FAIR.allocate(10L, requests(100L, 100L, 100L));

    assertEquals(List.of(4L, 3L, 3L), List.copyOf(grants.values()));
  }

  @Test
  void consumersThatAskedForNothingAreNoPartOfTheSharing() {
    Map<PowerConsumer, Long> requests = requests(10L, 0L, -5L);

    Map<PowerConsumer, Long> grants = DistributionStrategy.MAX_MIN_FAIR.allocate(100L, requests);

    assertEquals(List.of(10L), List.copyOf(grants.values()));
  }

  /** Requests in a stable order, so the strategy's tie-breaking stays visible in the results. */
  private Map<PowerConsumer, Long> requests(long... amounts) {
    TestNodes.Consumer[] consumers = {first, second, third};
    Map<PowerConsumer, Long> requests = new LinkedHashMap<>();
    for (int index = 0; index < amounts.length; index++) {
      requests.put(consumers[index], amounts[index]);
    }
    return requests;
  }
}
