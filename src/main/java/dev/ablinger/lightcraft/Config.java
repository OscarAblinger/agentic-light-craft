package dev.ablinger.lightcraft;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * The mod's common config, {@code lightcraft-common.toml}.
 *
 * <p>It holds exactly the power system's two categories: {@code [radiance]} for the power value
 * and its FE conversion, and {@code [network]} for topology, tiers and delivery. There is no
 * mod-name prefix because the file itself is already per-mod.
 *
 * <p>Java runs the static initializers below in textual order, so the {@code push()}/{@code pop()}
 * blocks bracket each category's keys exactly as they appear in the TOML file.
 */
public class Config {

  private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

  // [radiance] - the power value and its FE conversion.
  static {
    BUILDER.push("radiance");
  }

  /** FE per intensity of white light (#FFFFFF), the upper conversion anchor. */
  public static final ModConfigSpec.DoubleValue WHITE_FE_PER_INTENSITY = BUILDER
      .comment("FE per intensity of white light (#FFFFFF), the upper conversion anchor.")
      .defineInRange("whiteFePerIntensity", 100.0, 0.0, 1_000_000.0);

  /** FE per intensity of the dim anchor (#010101), the lower conversion anchor. */
  public static final ModConfigSpec.DoubleValue DIM_FE_PER_INTENSITY = BUILDER
      .comment("FE per intensity of the dim anchor (#010101), the lower conversion anchor.")
      .defineInRange("dimFePerIntensity", 0.1, 0.0, 1_000_000.0);

  // [network] - topology, tiers and delivery. Tier values live under [network.basicTier], so
  // future tiers add a subcategory instead of new code.
  static {
    BUILDER.pop();
    BUILDER.push("network");
    BUILDER.push("basicTier");
  }

  /** Max link distance between transport nodes (and to hubs). */
  public static final ModConfigSpec.IntValue BASIC_TIER_TRANSPORT_LINK_DISTANCE = BUILDER
      .comment("Max link distance between transport nodes (and to hubs).")
      .defineInRange("transportLinkDistance", 4, 0, 256);

  /** Max link distance from a transport node to any other network member. */
  public static final ModConfigSpec.IntValue BASIC_TIER_MEMBER_LINK_DISTANCE = BUILDER
      .comment("Max link distance from a transport node to any other network member.")
      .defineInRange("memberLinkDistance", 4, 0, 256);

  /** Max number of links per transport node. */
  public static final ModConfigSpec.IntValue BASIC_TIER_MAX_LINKS = BUILDER
      .comment("Max number of links per transport node.")
      .defineInRange("maxLinks", 6, 0, 64);

  /** Range within which a transport node serves a linked machine endpoint. */
  public static final ModConfigSpec.IntValue BASIC_TIER_MAX_SEND_RANGE = BUILDER
      .comment("Range within which a transport node serves a linked machine endpoint.")
      .defineInRange("maxSendRange", 4, 0, 256);

  /** Max FE delivered per FE node per tick; hub upgrades raise it within config bounds. */
  public static final ModConfigSpec.IntValue BASIC_TIER_MAX_FE_PER_TICK_PER_TARGET = BUILDER
      .comment("Max FE delivered per FE node per tick; hub upgrades raise it within config bounds.")
      .defineInRange("maxFePerTickPerTarget", 1_000, 0, 1_000_000);

  static {
    BUILDER.pop();
  }

  /** Emitter beam range in blocks, without attenuation. */
  public static final ModConfigSpec.IntValue EMITTER_RANGE = BUILDER
      .comment("Emitter beam range in blocks, without attenuation.")
      .defineInRange("emitterRange", 4, 0, 256);

  /** Networks processed per tick, smoothing allocation spikes across many active networks. */
  public static final ModConfigSpec.IntValue NETWORKS_PER_TICK_BUDGET = BUILDER
      .comment("Networks processed per tick, smoothing allocation spikes across many active networks.")
      .defineInRange("networksPerTickBudget", 128, 1, 1_000_000);

  /** Whether the Crystal Staff requires line of sight for transport-to-transport links. */
  public static final ModConfigSpec.BooleanValue TRANSPORT_TO_TRANSPORT_LINKS_REQUIRE_LINE_OF_SIGHT = BUILDER
      .comment("Whether the Crystal Staff requires line of sight for transport-to-transport links.")
      .define("transportToTransportLinksRequireLineOfSight", false);

  /** Whether transport-to-member links require line of sight until a hub upgrade lifts it. */
  public static final ModConfigSpec.BooleanValue MACHINE_LINKS_REQUIRE_LINE_OF_SIGHT_WITHOUT_UPGRADE = BUILDER
      .comment("Whether transport-to-member links require line of sight until a hub upgrade lifts it.")
      .define("machineLinksRequireLineOfSightWithoutUpgrade", true);

  static {
    BUILDER.pop();
  }

  static final ModConfigSpec SPEC = BUILDER.build();
}
