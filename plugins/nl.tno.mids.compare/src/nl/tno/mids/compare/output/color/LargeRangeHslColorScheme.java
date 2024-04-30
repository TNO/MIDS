/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2024 TNO and Contributors to the GitHub community
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
 * A Hsl color scheme that produces {@link HslColors} ranging from blue (hsl:240) to purple (hsl:300) varying on a given
 * value within a range from 0 to a given maximum value.
 * <p>
 * Hsl values are produced in descending order by an increasing input value. Given values of 0 will result in colors
 * with increased saturation and luminance.
 * </p>
 * 
 * @see GradientHslColorScheme
 */
public class LargeRangeHslColorScheme extends GradientHslColorScheme {
    @Override
    protected int getHueStartValue() {
        return 240; // start at blue
    }

    @Override
    protected int getHueNumberOfValues() {
        return 300; // Ends at purple
    }

    @Override
    public int getSaturation(int value, int maxValue) {
        // We want extra saturation in case of 0
        return value == 0 ? 80 : 60;
    }

    @Override
    public int getLuminance(int value, int maxValue) {
        // We want extra luminance in case of 0
        return value == 0 ? 50 : 35;
    }

    @Override
    protected ScaleDirection getHueDirection() {
        return ScaleDirection.DESCENDING;
    }

    @Override
    protected float getSampleValue(int value, int maxValue) {
        Preconditions.checkArgument(value >= 0);
        Preconditions.checkArgument(value <= maxValue);
        return (float)(maxValue == 0 ? 0 : Math.sqrt((value / (float)maxValue)));
    }
}
