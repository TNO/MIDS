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

import java.util.List;

import org.eclipse.escet.common.java.Lists;

import com.google.common.base.Preconditions;

/**
 * A Hsl color scheme that produces {@link HslColors} ranging from green (hsl:134) to red (hsl:0) varying on a given
 * value within a range from 0 to a given maximum value.
 * 
 * @see GradientHslColorScheme
 */
public class IntuitiveHslColorScheme extends GradientHslColorScheme {
    @Override
    protected int getHueStartValue() {
        return 120; // start at green
    }

    @Override
    protected int getHueNumberOfValues() {
        return 120; // end at red
    }

    /**
     * This method returns hue values from a range relative to the given value/maxValue.
     * <ul>
     * <li>the first segment [0%..7%*maxValue] returns a value from the range[134...55]</li>
     * <li>The second segment [7%*maxValue..100%*maxValue] returns a value from the range[55...0]</li>
     * </ul>
     */
    @Override
    public int getHue(int value, int maxValue) {
        float segment1Factor = 0.07f;

        List<RangeSelector<RangeSelector<Integer>>> valueRanges = Lists.list();
        valueRanges.add(new RangeSelector<RangeSelector<Integer>>(0, maxValue * segment1Factor)
                .set(new RangeSelector<Integer>(134, 55)));
        valueRanges.add(new RangeSelector<RangeSelector<Integer>>(maxValue * segment1Factor + 1, maxValue)
                .set(new RangeSelector<Integer>(55, 0)));

        RangeSelector<RangeSelector<Integer>> valueRange = valueRanges.stream()
                .filter(vr -> value >= vr.min && value <= vr.max).findFirst().get();

        int increment = Math.round((value - valueRange.min) * increaseCoefficient(valueRange, valueRange.targetRange));
        return (int)valueRange.targetRange.min + increment;
    }

    /**
     * This method returns saturation values from a range relative to the given value/maxValue.
     * <ul>
     * <li>the first segment [0%.. 7%*maxValue] returns a value from the range[37...83]</li>
     * <li>the second segment [7%*maxValue..46%*maxValue] returns a value from the range[83...100]</li>
     * <li>The thirtd segment [46%*maxValue..100%*maxValue] returns a value from the range[100...85]</li>
     * </ul>
     */
    @Override
    public int getSaturation(int value, int maxValue) {
        float segment1Factor = 0.07f;
        float segment2Factor = 0.46f;

        List<RangeSelector<RangeSelector<Integer>>> valueRanges = Lists.list();
        valueRanges.add(new RangeSelector<RangeSelector<Integer>>(0, maxValue * segment1Factor)
                .set(new RangeSelector<Integer>(37, 83)));
        valueRanges
                .add(new RangeSelector<RangeSelector<Integer>>(maxValue * segment1Factor + 1, maxValue * segment2Factor)
                        .set(new RangeSelector<Integer>(83, 100)));
        valueRanges.add(new RangeSelector<RangeSelector<Integer>>(maxValue * segment2Factor + 1, maxValue)
                .set(new RangeSelector<Integer>(100, 85)));

        RangeSelector<RangeSelector<Integer>> valueRange = valueRanges.stream()
                .filter(vr -> value >= vr.min && value <= vr.max).findFirst().get();

        int increment = Math.round((value - valueRange.min) * increaseCoefficient(valueRange, valueRange.targetRange));
        return (int)valueRange.targetRange.min + increment;
    }

    private float increaseCoefficient(RangeSelector<RangeSelector<Integer>> valueRange,
            RangeSelector<Integer> targetRange)
    {
        return (float)(targetRange.max - targetRange.min) / (valueRange.max - valueRange.min);
    }

    @Override
    public int getLuminance(int value, int maxValue) {
        return 57;
    }

    @Override
    protected ScaleDirection getHueDirection() {
        return ScaleDirection.DESCENDING;
    }

    @Override
    protected float getSampleValue(int value, int maxValue) {
        Preconditions.checkArgument(value >= 0);
        Preconditions.checkArgument(value <= maxValue);
        return (float)value / maxValue;
    }

    private class RangeSelector<S> {
        private final int min;

        private final int max;

        private S targetRange;

        RangeSelector(int min, int max) {
            this.min = min;
            this.max = max;
        }

        public RangeSelector<S> set(S targetRange) {
            this.targetRange = targetRange;
            return this;
        }

        RangeSelector(int min, float max) {
            this(min, Math.round(max));
        }

        RangeSelector(float min, float max) {
            this(Math.round(min), Math.round(max));
        }
    }
}
