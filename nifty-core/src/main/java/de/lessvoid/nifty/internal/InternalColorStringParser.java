package de.lessvoid.nifty.internal;

import java.util.logging.Level;
import java.util.logging.Logger;

import de.lessvoid.nifty.api.NiftyColor;

/**
 * Helper class to create Color instances from String representation.
 * @author void
 */
public class InternalColorStringParser {
  private static Logger log = Logger.getLogger(InternalColorStringParser.class.getName());

  /**
   * ColorValidator.
   */
  private static InternalColorValidator colorValidator = new InternalColorValidator();

  /**
   * scale short mode factor (converts 0x5 to 0x55).
   */
  private static final int SCALE_SHORT_MODE = 0x11;

  /**
   * max value for conversion.
   */
  private static final float MAX_INT_VALUE = 255.0f;

  /**
   * hex base to convert numbers.
   */
  private static final int HEX_BASE = 16;

  /**
   * Create a new Color instance from a color String.
   * @param color color String in short mode without alphe ("f09") or in short mode with alpha ("f09a") or in long
   * mode without alpha ("ff0099") or with alpha ("ff0099aa") 
   * @return Color instance
   */
  public NiftyColor fromString(final String color) {
    float red = 1.f;
    float green = 1.f;
    float blue = 1.f;
    float alpha = 1.f;

    if (colorValidator.isShortModeWithoutAlpha(color)) {
      red = getRFromString(color);
      green = getGFromString(color);
      blue = getBFromString(color);
      if (log.isLoggable(Level.FINE)) {
        log.fine("found short mode color [" + color + "] with missing alpha value automatically adjusted with alpha value of [#f]");
      }
    } else if (colorValidator.isLongModeWithoutAlpha(color)) {
      red = getRFromString(color);
      green = getGFromString(color);
      blue = getBFromString(color);
      if (log.isLoggable(Level.FINE)) {
        log.fine("found long mode color [" + color + "] with missing alpha value automatically adjusted with alpha value of [#ff]");
      }
    } else if (colorValidator.isValid(color)) {
      red = getRFromString(color);
      green = getGFromString(color);
      blue = getBFromString(color);
      alpha = getAFromString(color);
    } else {
      log.fine("error parsing color [" + color + "] automatically adjusted to white [#ffffffff]");
      red = green = blue = alpha = 1.0f;
    }
    return new NiftyColor(red, green, blue, alpha);
  }

  /**
   * helper to get red from a string value.
   * @param color color string
   * @return extracted red value
   */
  private float getRFromString(final String color) {
    if (isShortMode(color)) {
      return (Integer.valueOf(color.substring(1, 2), HEX_BASE) * SCALE_SHORT_MODE) / MAX_INT_VALUE;
    } else {
      return Integer.valueOf(color.substring(1, 3), HEX_BASE) / MAX_INT_VALUE;
    }
  }

  /**
   * helper to get green from a string value.
   * @param color color string
   * @return extracted green
   */
  private float getGFromString(final String color) {
    if (isShortMode(color)) {
      return (Integer.valueOf(color.substring(2, 3), HEX_BASE) * SCALE_SHORT_MODE) / MAX_INT_VALUE;
    } else {
      return Integer.valueOf(color.substring(3, 5), HEX_BASE) / MAX_INT_VALUE;
    }
  }

  /**
   * helper to get blue from a string value.
   * @param color color string
   * @return extracted blue
   */
  private float getBFromString(final String color) {
    if (isShortMode(color)) {
      return (Integer.valueOf(color.substring(3, 4), HEX_BASE) * SCALE_SHORT_MODE) / MAX_INT_VALUE;
    } else {
      return Integer.valueOf(color.substring(5, 7), HEX_BASE) / MAX_INT_VALUE;
    }
  }

  /**
   * helper to get alpha from a string value.
   * @param color color string
   * @return alpha value
   */
  private float getAFromString(final String color) {
    if (isShortMode(color)) {
      return (Integer.valueOf(color.substring(4, 5), HEX_BASE) * SCALE_SHORT_MODE) / MAX_INT_VALUE;
    } else {
      return Integer.valueOf(color.substring(7, 9), HEX_BASE) / MAX_INT_VALUE;
    }
  }

  /**
   * Returns true when the given string is from format: #ffff and false when #ffffffff.
   * @param color the color string
   * @return true or false
   */
  private boolean isShortMode(final String color) {
    return colorValidator.isShortMode(color) || colorValidator.isShortModeWithoutAlpha(color);
  }
}