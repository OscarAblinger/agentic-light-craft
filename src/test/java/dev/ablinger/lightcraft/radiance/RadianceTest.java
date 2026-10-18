package dev.ablinger.lightcraft.radiance;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Unit tests of the {@link Radiance} value: the signed byte storage of the colour channels,
 * the {@link Radiance#DEFAULT_COLOUR} constant, and the accessor and derivation helpers.
 */
class RadianceTest {

  @Test
  void channelsReadBackAsUnsignedValues() {
    // Java bytes are signed, so the stored 0-255 comes back as a negative byte above 127.
    // & 0xFF shifts every channel back into its actual 0-255 range.
    Radiance radiance = new Radiance((byte) -1, (byte) 128, (byte) 200, 5L);

    assertEquals(255, radiance.red255());
    assertEquals(128, radiance.green255());
    assertEquals(200, radiance.blue255());
  }

  @Test
  void channelsStayWithinZeroTo255() {
    // Every possible byte already lands in 0-255 after & 0xFF; spot-check the extremes.
    Radiance low = new Radiance((byte) 0, (byte) 0, (byte) 0, 0L);
    Radiance high = new Radiance(Byte.MIN_VALUE, Byte.MAX_VALUE, (byte) -1, 0L);

    assertEquals(0, low.red255());
    assertEquals(127, high.green255()); // Byte.MAX_VALUE stays 127
    assertEquals(128, high.red255()); // Byte.MIN_VALUE is -128, stored back as 128
    assertEquals(255, high.blue255());
  }

  @Test
  void negativeIntensityIsClampedToZero() {
    Radiance radiance = Radiance.DEFAULT_COLOUR.withIntensity(-10L);

    assertEquals(0L, radiance.intensity());
  }

  @Test
  void defaultColourIsTheDimAnchor() {
    assertEquals(1, Radiance.DEFAULT_COLOUR.red255());
    assertEquals(1, Radiance.DEFAULT_COLOUR.green255());
    assertEquals(1, Radiance.DEFAULT_COLOUR.blue255());
    assertEquals(0L, Radiance.DEFAULT_COLOUR.intensity());
  }

  @Test
  void withIntensityKeepsTheColour() {
    Radiance radiance = Radiance.DEFAULT_COLOUR.withIntensity(7L);

    assertEquals(new Radiance((byte) 1, (byte) 1, (byte) 1, 7L), radiance);
    assertEquals(7L, radiance.intensity());
  }
}
