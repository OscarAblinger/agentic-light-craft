package dev.ablinger.lightcraft.radiance;

import net.minecraft.util.Mth;

/**
 * Immutable light value of the power system. Colour describes the kind of light, intensity the
 * amount; intensity is displayed as {@code 1R}, {@code 2R}, ... with the colour shown separately
 * as a swatch or hex value.
 *
 * <p>Colour is a network property, not a node property: it is uniform per network but derived
 * live from current production, so it can rise and fall from tick to tick.
 * {@link #DEFAULT_COLOUR} is the dim anchor (#010101) a network falls back to when it has no
 * other colour source - minimal, but never zero output.
 *
 * <p>Java bytes are signed, so a channel stored as 0-255 reads back as a negative {@code byte}
 * for values above 127. Always read channels through {@link #red255()}, {@link #green255()} and
 * {@link #blue255()}, which shift the stored byte back into the 0-255 range.
 */
public record Radiance(byte red, byte green, byte blue, long intensity) {

  /** The dim anchor #010101, used by networks without any other colour source. */
  public static final Radiance DEFAULT_COLOUR = new Radiance((byte) 1, (byte) 1, (byte) 1, 0L);

  public Radiance {
    // Bytes are signed; a stored 0-255 is read back via & 0xFF (see red255() etc.).
    red = (byte) Mth.clamp(red & 0xFF, 0, 255);
    green = (byte) Mth.clamp(green & 0xFF, 0, 255);
    blue = (byte) Mth.clamp(blue & 0xFF, 0, 255);
    intensity = Math.max(0L, intensity);
  }

  /** The red channel as its actual 0-255 value instead of the signed byte representation. */
  public int red255() {
    return red & 0xFF;
  }

  /** The green channel as its actual 0-255 value instead of the signed byte representation. */
  public int green255() {
    return green & 0xFF;
  }

  /** The blue channel as its actual 0-255 value instead of the signed byte representation. */
  public int blue255() {
    return blue & 0xFF;
  }

  /** This value's colour with a different intensity. */
  public Radiance withIntensity(long intensity) {
    return new Radiance(red, green, blue, intensity);
  }
}
