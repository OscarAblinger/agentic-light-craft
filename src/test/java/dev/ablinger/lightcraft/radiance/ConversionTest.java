package dev.ablinger.lightcraft.radiance;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Unit tests of the Radiance to FE conversion: the two configurable anchors (#FFFFFF at 100 FE
 * per intensity, #010101 at 0.1 FE per intensity), the zero for colourless light, the linear
 * interpolation between the anchors, and the residue accumulator that drains long amounts
 * through the int-sized FE API without losing power to rounding.
 */
class ConversionTest {

  /** The default anchors from the config: white 100 FE/R, dim 0.1 FE/R at channel sum 3. */
  private static final Conversion CONVERSION = new Conversion(100.0, 0.1);

  private static final Radiance WHITE = new Radiance((byte) 255, (byte) 255, (byte) 255, 0L);
  private static final Radiance DIM = Radiance.DEFAULT_COLOUR; // #010101
  private static final Radiance BLACK = new Radiance((byte) 0, (byte) 0, (byte) 0, 0L); // #000000

  private static final double EPS = 1e-9;

  @Test
  void whiteLightIsWorthHundredFePerIntensity() {
    assertEquals(100.0, CONVERSION.fePerIntensity(255, 255, 255), EPS);
    assertEquals(100L, CONVERSION.toFe(1L, WHITE));
  }

  @Test
  void dimAnchorIsWorthTenthFePerIntensity() {
    assertEquals(0.1, CONVERSION.fePerIntensity(1, 1, 1), EPS);
    assertEquals(1L, CONVERSION.toFe(10L, DIM)); // 10 intensity make 1 FE
    assertEquals(0L, CONVERSION.toFe(1L, DIM)); // single rounding still rounds down here
  }

  @Test
  void colourlessLightIsWorthNothing() {
    assertEquals(0.0, CONVERSION.fePerIntensity(0, 0, 0), EPS);
    assertEquals(0L, CONVERSION.toFe(1_000L, BLACK));
  }

  @Test
  void rateInterpolatesLinearlyBetweenTheAnchors() {
    // Channel sum 384 is the midpoint between the anchors 3 and 765, so the rate must be the
    // midpoint of 0.1 and 100 FE per intensity.
    assertEquals(50.05, CONVERSION.fePerIntensity(128, 128, 128), EPS);
  }

  @Test
  void ratesBelowTheDimAnchorClampToZero() {
    // #000002 lies below the dim anchor; the interpolation goes negative and clamps to zero.
    assertEquals(0.0, CONVERSION.fePerIntensity(0, 0, 2), EPS);
    assertEquals(0L, CONVERSION.toFe(1_000L, new Radiance((byte) 0, (byte) 0, (byte) 2, 0L)));
  }

  @Test
  void negativeIntensityIsWorthNothing() {
    assertEquals(0L, CONVERSION.toFe(-5L, WHITE));
  }

  @Test
  void residueAccumulatesUntilWholeFeIsPending() {
    Conversion.Residue residue = CONVERSION.residue();

    residue.add(5L, DIM); // 0.5 FE, not drainable yet
    assertEquals(0, residue.pending());

    residue.add(5L, DIM); // another 0.5 FE make a whole one
    assertEquals(1, residue.pending());
    assertEquals(1, residue.drain(Integer.MAX_VALUE));
    assertEquals(0, residue.pending());
  }

  @Test
  void residueDrainsLongAmountsThroughTheIntApi() {
    Conversion.Residue residue = CONVERSION.residue();

    residue.add(25L, WHITE); // 2500 FE at 100 FE per intensity

    assertEquals(1_000, residue.drain(1_000));
    assertEquals(1_000, residue.drain(1_000));
    assertEquals(500, residue.drain(1_000));
    assertEquals(0, residue.drain(1_000));
  }

  @Test
  void residueNeverLosesPowerAcrossTicks() {
    Conversion.Residue residue = CONVERSION.residue();

    // 10,000 ticks of 5 intensity each at the dim anchor: 0.5 FE per tick, always drained
    // with a per-target cap of 1000 FE. The exact total of 5,000 FE must come out whole -
    // per-tick rounding would dribble or lose it entirely.
    long drained = 0;
    for (int tick = 0; tick < 10_000; tick++) {
      residue.add(5L, DIM);
      drained += residue.drain(1_000);
    }

    assertEquals(5_000L, drained);
    assertEquals(0, residue.pending());
  }

  @Test
  void residueCarriesTheFractionLeftByAnEarlyDrain() {
    Conversion.Residue residue = CONVERSION.residue();

    residue.add(5L, DIM);
    residue.add(5L, DIM);
    residue.add(5L, DIM); // 1.5 FE
    assertEquals(1, residue.drain(Integer.MAX_VALUE)); // 0.5 FE stays behind

    residue.add(5L, DIM); // the carried fraction completes the next whole FE
    assertEquals(1, residue.pending());
    assertEquals(1, residue.drain(Integer.MAX_VALUE));
  }

  @Test
  void residueIgnoresNonPositiveIntensity() {
    Conversion.Residue residue = CONVERSION.residue();

    residue.add(-5L, WHITE);
    residue.add(0L, WHITE);

    assertEquals(0, residue.pending());
  }

  @Test
  void residueUsesTheRateOfEachIndividualAdd() {
    // Colours are live: every add converts at the rate of its own colour, not of the first.
    Conversion.Residue residue = CONVERSION.residue();

    residue.add(1L, WHITE); // 100 FE
    residue.add(10L, DIM); // 1 FE

    assertEquals(101, residue.pending());
  }
}
