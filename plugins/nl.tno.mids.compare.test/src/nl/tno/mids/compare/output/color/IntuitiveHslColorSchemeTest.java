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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import nl.tno.mids.compare.options.HslColorScheme;

public class IntuitiveHslColorSchemeTest {
    @Test
    public void test_threeColored_colorSampleCalculator_on_start_middle_and_end_values() {
        int minValue = 1;
        int maxValue = 28;
        int middleValue = (minValue + maxValue) / 2;

        GradientHslColorScheme colorSampler = HslColorScheme.INTUITIVE.getSampler();

        { // spectrum.start
            HSLColor resultColor = colorSampler.sampleColor(minValue, maxValue);
            assertNotNull(resultColor);
            assertEquals(95, resultColor.getHue());
            assertEquals(60, resultColor.getSaturation());
            assertEquals(57, resultColor.getLuminance());
        }
        { // spectrum.end
            HSLColor resultColor = colorSampler.sampleColor(maxValue, maxValue);
            assertNotNull(resultColor);
            assertEquals(0, resultColor.getHue());
            assertEquals(85, resultColor.getSaturation());
            assertEquals(57, resultColor.getLuminance());
        }
        { // spectrum.middle
            HSLColor resultColor = colorSampler.sampleColor(middleValue, maxValue);
            assertNotNull(resultColor);
            assertEquals(31, resultColor.getHue());
            assertEquals(100, resultColor.getSaturation());
            assertEquals(57, resultColor.getLuminance());
        }
    }
}
