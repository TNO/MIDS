/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2023 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////

package nl.tno.mids.compare.output.color;

import com.google.common.base.Preconditions;

/**
 * A GradientHslColorScheme can be used to get sample colors, as HSLColor, from a color scheme by a hue start value, the
 * available number of colors and the hue direction.
 * <p>
 * After defining the GradientHslColorScheme color samples can be obtained via {@link #sampleColor(int, int)} by passing
 * the relevant value and range maximum.
 * </p>
 */
public abstract class GradientHslColorScheme implements HslColorSampler {
    private static final double MAX_HUE_COLORS = 360;

    /**
     * Get the HSLColor width hue, saturation and luminance based on the value/maxValue. Values will be obtained by the
     * following methods:
     * <ul>
     * <li>hue {@link #getHue(int, int)}</li>
     * <li>saturation {@link #getSaturation(int, int)}</li>
     * <li>luminance {@link #getLuminance(int, int)}</li>
     * </ul>
     * Override these methods to deviate from their defaults.
     */
    public HSLColor sampleColor(int value, int maxValue) {
        return new HSLColor(getHue(value, maxValue), getSaturation(value, maxValue), getLuminance(value, maxValue));
    }

    protected int getHue(int value, int maxValue) {
        Preconditions.checkArgument(value >= 0);
        Preconditions.checkArgument(value <= maxValue);

        // translate the value/maxValue to a factor between [0..1]
        float sampleValue = getSampleValue(value, maxValue);

        // project the sampled value onto the color scale [0..numberOfValues]
        double sampledColor = sampleValue * (getHueNumberOfValues() - 1);

        // shift the sampled color from the color scheme start value in the given direction
        double projectedColorValue = getHueStartValue() + sampledColor * getHueDirection().factor;

        // normalize the color value to wrap around at 0 to 360
        return (int)((MAX_HUE_COLORS + projectedColorValue) % MAX_HUE_COLORS);
    }

    abstract protected int getLuminance(int value, int maxValue);

    abstract protected int getSaturation(int value, int maxValue);

    abstract protected int getHueStartValue();

    abstract protected int getHueNumberOfValues();

    abstract protected ScaleDirection getHueDirection();

    abstract protected float getSampleValue(int value, int maxValue);

    protected enum ScaleDirection {
        ASCENDING(1), DESCENDING(-1);

        final private int factor;

        private ScaleDirection(int factor) {
            this.factor = factor;
        }
    }
}
