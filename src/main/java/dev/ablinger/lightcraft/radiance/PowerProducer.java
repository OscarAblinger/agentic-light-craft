package dev.ablinger.lightcraft.radiance;

/**
 * A node that produces light: a generator, or any machine offering intensity to its network
 * (design report 4.10).
 *
 * <p>Producers never move energy themselves. During their own block-entity tick they declare
 * by calling {@link PowerNetwork#offer(Radiance, PowerProducer)}; nothing moves until the
 * network allocates once, after every declaration of the tick is in.
 */
public interface PowerProducer extends NetworkNode {
}
