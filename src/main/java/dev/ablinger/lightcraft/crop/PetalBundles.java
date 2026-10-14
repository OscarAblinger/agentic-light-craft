package dev.ablinger.lightcraft.crop;

import dev.ablinger.lightcraft.LightCraft;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * The petal bundle tiers of the luxflower, used to keep big suncharge harvests compact.
 *
 * <p>A small petal bundle holds 8 petals, every following tier multiplies that by 64.
 * Because normal crafting cannot take stacks of 64 as input, only the small bundle can
 * be crafted up from loose petals; the four larger tiers drop straight out of a charged
 * crop instead and wait for the pressing machine that will compress them by hand.
 * Every larger tier can be crafted back down one step at a time, ending at the small
 * bundle, which unpacks into its eight loose petals.
 */
public final class PetalBundles {

  /** One compression tier: a bundle item holding the given amount of loose petals. */
  public record Tier(int petals, Supplier<Item> item) {
  }

  /** All tiers ordered by size, the smallest bundle last so drops compress greedily. */
  private static final List<Tier> TIERS = List.of(
      new Tier(16777216, LightCraft.QUADRUPLE_PETAL_BUNDLE),
      new Tier(262144, LightCraft.TRIPLE_PETAL_BUNDLE),
      new Tier(4096, LightCraft.DOUBLE_PETAL_BUNDLE),
      new Tier(64, LightCraft.PETAL_BUNDLE),
      new Tier(8, LightCraft.SMALL_PETAL_BUNDLE));

  private PetalBundles() {
  }

  /** The tiers ordered from the largest bundle down to the smallest. */
  public static List<Tier> tiersDescending() {
    return TIERS;
  }

  /** How many loose petals one of the given items represents, or 0 if it is none. */
  public static int petalsPer(Item item) {
    for (Tier tier : TIERS) {
      if (tier.item().get() == item) {
        return tier.petals();
      }
    }
    return 0;
  }

  /**
   * Splits a petal amount into the largest possible bundles, so a huge harvest drops a
   * handful of bundles instead of thousands of loose petals.
   */
  public static void decompose(int petals, Consumer<ItemStack> drops) {
    int remaining = petals;
    for (Tier tier : TIERS) {
      while (remaining >= tier.petals()) {
        int count = Math.min(remaining / tier.petals(), 64);
        drops.accept(new ItemStack(tier.item().get(), count));
        remaining -= count * tier.petals();
      }
    }
    if (remaining > 0) {
      drops.accept(new ItemStack(LightCraft.LUXFLOWER_PETAL.get(), remaining));
    }
  }
}
