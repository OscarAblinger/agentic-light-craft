package dev.ablinger.lightcraft.radiance;

/**
 * A node that consumes light: a light machine, an emitter drawing beam energy, or an FE
 * machine behind its adapter (design report 4.10).
 *
 * <p>Consumers declare by calling {@link PowerNetwork#request(long, PowerConsumer)} during
 * their own block-entity tick and receive the allocator's decision through
 * {@link #onGranted(long)}. The grant arrives with the usual one-tick request-to-grant delay
 * (design report 4.12), so light machines buffer it and spend it on their next tick.
 */
public interface PowerConsumer extends NetworkNode {

  /** The allocator's decision for this request; {@code 0} when the pool was empty. */
  void onGranted(long amount);
}
