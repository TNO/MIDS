/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2024 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////

package nl.tno.mids.compare.output;

import static nl.tno.mids.compare.output.color.ColorUtils.getColorSample;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.awt.Color;

import org.junit.jupiter.api.Test;

import nl.tno.mids.compare.output.color.ColorUtils.ColorGradient;

public class ColorUtilsTest {
    @Test
    public void test_colorGradient_on_1_scale_2_values() {
        Color startColor = new Color(100, 190, 190);
        Color endColor = new Color(249, 115, 109);

        {
            Color resultColor = getColorSample(new ColorGradient(startColor, endColor), 1, 0);
            assertNotNull(resultColor);
            assertEquals(startColor.getRed(), resultColor.getRed());
            assertEquals(startColor.getGreen(), resultColor.getGreen());
            assertEquals(startColor.getBlue(), resultColor.getBlue());
        }
        {
            Color resultColor = getColorSample(new ColorGradient(startColor, endColor), 1, 1);
            assertNotNull(resultColor);
            assertEquals(endColor.getRed(), resultColor.getRed());
            assertEquals(endColor.getGreen(), resultColor.getGreen());
            assertEquals(endColor.getBlue(), resultColor.getBlue());
        }
    }

    @Test
    public void test_colorGradient_on_2_scale_with_3_values() {
        Color startColor = new Color(100, 190, 190);
        Color endColor = new Color(249, 115, 109);

        {// expect the startColor to be returned
            Color resultColor = getColorSample(new ColorGradient(startColor, endColor), 2, 0);
            assertNotNull(resultColor);
            assertEquals(startColor.getRed(), resultColor.getRed());
            assertEquals(startColor.getGreen(), resultColor.getGreen());
            assertEquals(startColor.getBlue(), resultColor.getBlue());
        }
        {// expect the endColor to be returned
            Color resultColor = getColorSample(new ColorGradient(startColor, endColor), 2, 2);
            assertNotNull(resultColor);
            assertEquals(endColor.getRed(), resultColor.getRed());
            assertEquals(endColor.getGreen(), resultColor.getGreen());
            assertEquals(endColor.getBlue(), resultColor.getBlue());
        }
        {// expect a Color to be returned that is approximately in between the startColor and the endColor
            Color resultColor = getColorSample(new ColorGradient(startColor, endColor), 2, 1);
            assertNotNull(resultColor);
            assertEquals(174, resultColor.getRed());
            assertEquals(153, resultColor.getGreen());
            assertEquals(150, resultColor.getBlue());
        }
    }
}
