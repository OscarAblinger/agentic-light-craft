package dev.ablinger.lightcraft.radiance;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * How a network shares its available intensity among the consumers that requested some
 * (design report 4.10). Producers and consumers only declare; the strategy is the network's
 * one allocation decision per tick.
 *
 * <p>The default is {@link #MAX_MIN_FAIR}. The hub may later replace it with another strategy
 * (design report 4.9) - the network holds whichever strategy is current, defaults apply when
 * no hub is present.
 */
@FunctionalInterface
public interface DistributionStrategy {

  /**
   * Equal shares up to demand (design report 1.1): nobody gets more than they asked for, and
   * the scarce supply is shared as equally as whole R allow. The smallest demands are served
   * first, so the leftovers of a satiated consumer flow to the still-unsatiated ones.
   */
  DistributionStrategy MAX_MIN_FAIR = DistributionStrategy::maxMinFair;

  /**
   * Shares {@code available} among the declared requests.
   *
   * @param available the intensity to share: the network's stored pool plus this tick's offers
   * @param requests the declared demands, in declaration order
   * @return the granted amount per consumer, never above its demand; the grants sum to at most
   *         {@code available}
   */
  Map<PowerConsumer, Long> allocate(long available, Map<PowerConsumer, Long> requests);

  /** Max-min fair allocation: equal shares, never more than requested, nothing destroyed. */
  private static Map<PowerConsumer, Long> maxMinFair(long available, Map<PowerConsumer, Long> requests) {
    Map<PowerConsumer, Long> grants = new LinkedHashMap<>();
    long remaining = Math.max(0L, available);

    long totalRequested = 0L;
    for (long demand : requests.values()) {
      totalRequested += Math.max(0L, demand);
    }
    if (remaining >= totalRequested) {
      // Everything asked for is available: everyone gets exactly their request.
      for (Map.Entry<PowerConsumer, Long> request : requests.entrySet()) {
        if (request.getValue() > 0L) {
          grants.put(request.getKey(), request.getValue());
        }
      }
      return grants;
    }

    // Serve the smallest demands first; a consumer whose demand fits into the current equal
    // share is satiated and its remainder flows on. The sort is stable, so equally hungry
    // consumers keep their declaration order. Consumers that asked for nothing are no part
    // of the sharing at all.
    List<Map.Entry<PowerConsumer, Long>> byDemand = new ArrayList<>(requests.size());
    for (Map.Entry<PowerConsumer, Long> request : requests.entrySet()) {
      if (request.getValue() > 0L) {
        byDemand.add(request);
      }
    }
    byDemand.sort(Comparator.comparing(Map.Entry::getValue));

    for (int index = 0; index < byDemand.size() && remaining > 0L; index++) {
      long demand = byDemand.get(index).getValue();
      int unsatiated = byDemand.size() - index;
      long share = remaining / unsatiated;
      if (demand <= share) {
        grants.put(byDemand.get(index).getKey(), demand);
        remaining -= demand;
        continue;
      }
      // Everyone from here on gets an equal share. The division's remainder goes one R at a
      // time to the first of them, so the grants always add up to the available amount.
      long remainder = remaining % unsatiated;
      for (int offset = 0; offset < unsatiated; offset++) {
        long granted = share + (offset < remainder ? 1L : 0L);
        grants.put(byDemand.get(index + offset).getKey(), granted);
      }
      break;
    }
    return grants;
  }
}
