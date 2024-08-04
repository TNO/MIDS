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

public class LargeRangeHslColorSchemeTest {
    @Test
    public void test_threeColored_colorSampleCalculator_on_start_middle_and_end_values() {
        int minValue = 0;
        int maxValue = 24;
        int middleValue = (minValue + maxValue) / 2;

        LargeRangeHslColorScheme colorScheme = new LargeRangeHslColorScheme();

        { // spectrum.start
            HSLColor resultColor = colorScheme.sampleColor(minValue, maxValue);
            assertNotNull(resultColor);
            assertEquals(240, resultColor.getHue());
            assertEquals(80, resultColor.getSaturation());
            assertEquals(50, resultColor.getLuminance());
        }
        { // spectrum.end
            HSLColor resultColor = colorScheme.sampleColor(maxValue, maxValue);
            assertNotNull(resultColor);
            assertEquals(301, resultColor.getHue());
            assertEquals(60, resultColor.getSaturation());
            assertEquals(35, resultColor.getLuminance());
        }
        { // spectrum.middle
            HSLColor resultColor = colorScheme.sampleColor(middleValue, maxValue);
            assertNotNull(resultColor);
            assertEquals(28, resultColor.getHue());
            assertEquals(60, resultColor.getSaturation());
            assertEquals(35, resultColor.getLuminance());
        }
    }
}
