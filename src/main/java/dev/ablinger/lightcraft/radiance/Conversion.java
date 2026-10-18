package dev.ablinger.lightcraft.radiance;

/**
 * Conversion of Radiance intensity into FE (Forge Energy), for the output-only FE
 * interoperability.
 *
 * <p>The rate is linear in the channel sum {@code S = r+g+b} (0..765), anchored at two
 * configurable calibration points: white light (#FFFFFF, S=765) is worth
 * {@code whiteFePerIntensity} FE per intensity and the dim anchor (#010101, S=3) is worth
 * {@code dimFePerIntensity} FE per intensity. The dim anchor sits at {@link Radiance#DEFAULT_COLOUR},
 * so its channel sum is fixed together with that constant. Without any colour (S=0) no FE is
 * sent at all, and rates interpolated below the dim anchor are clamped to zero.
 *
 * <p>Intensity is a {@code long} while the FE {@code insert} API takes an {@code int}, and
 * {@code int} is also far too small for amounts like 100 FE per intensity at high intensity.
 * {@link #toFe} is the stateless one-shot conversion of a single amount; {@link Residue} is the
 * stateful accumulator for delivering FE over time, draining long amounts through the int API in
 * whole-FE portions and carrying the fractional remainder over to the next tick, so per-tick
 * rounding never destroys power.
 *
 * <p>Instances are immutable snapshots of the conversion anchors; see {@code Config} for the
 * config keys providing them.
 */
public record Conversion(double whiteFePerIntensity, double dimFePerIntensity) {

  /**
   * Channel sum of the dim anchor #010101. Fixed rather than configured because the dim anchor
   * is {@link Radiance#DEFAULT_COLOUR}, which lives in code.
   */
  private static final int DIM_SUM = 3;

  /** Channel sum of white light (#FFFFFF), the upper anchor of the conversion. */
  private static final int WHITE_SUM = 765;

  /** FE per intensity for the given colour channels (their actual 0-255 values). */
  public double fePerIntensity(int red, int green, int blue) {
    int sum = red + green + blue; // 0..765
    if (sum <= 0) {
      return 0.0; // no colour, no FE
    }
    double rate = dimFePerIntensity
        + (sum - DIM_SUM) * (whiteFePerIntensity - dimFePerIntensity) / (WHITE_SUM - DIM_SUM);
    return Math.max(0.0, rate);
  }

  /** The FE worth of an intensity amount at the given colour, rounded to whole FE. */
  public long toFe(long intensity, Radiance colour) {
    if (intensity <= 0L) {
      return 0L;
    }
    return Math.round(intensity * fePerIntensity(colour.red255(), colour.green255(), colour.blue255()));
  }

  /**
   * A new residue accumulator tuned to this conversion. Use it to deliver FE over several
   * ticks; use {@link #toFe} for a single, non-repeated conversion.
   */
  public Residue residue() {
    return new Residue();
  }

  /**
   * Accumulator draining long Radiance or FE amounts through the int-sized FE insert API.
   *
   * <p>Each {@link #add} contributes the exact, possibly fractional FE worth of a granted
   * intensity at the colour it was produced with; colours (and therefore rates) may change
   * from tick to tick. {@link #drain} hands out whole FE only and keeps the fraction, so over
   * time nothing is lost to rounding - unlike per-tick {@link Conversion#toFe}, which would
   * round the 0.3 FE of a dim 3 intensity grant down to zero on every single tick.
   */
  public final class Residue {

    /** Exact FE accumulated so far; {@link #pending()} exposes only its whole part. */
    private double fe;

    private Residue() {
    }

    /** Adds the exact FE worth of the given intensity produced at the given colour. */
    public void add(long intensity, Radiance colour) {
      if (intensity <= 0L) {
        return;
      }
      fe += intensity * fePerIntensity(colour.red255(), colour.green255(), colour.blue255());
    }

    /** Whole FE available to move right now; the carried fraction stays pending. */
    public int pending() {
      return (int) Math.min(Math.floor(fe), Integer.MAX_VALUE);
    }

    /** Removes at most {@code max} whole FE and returns how many were taken. */
    public int drain(int max) {
      int drained = Math.min(pending(), Math.max(0, max));
      fe -= drained;
      return drained;
    }
  }
}
