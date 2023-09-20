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

import java.awt.Color;

public class ColorUtils {
    /**
     * Get {@link Color#BLACK} or {@link Color#WHITE} contrasting most the given color. (code from:
     * https://stackoverflow.com/questions/4672271/reverse-opposing-colors)
     * 
     * @param color This is the color used to determine the contrasting color
     * @return {@link Color#BLACK} or {@link Color#WHITE}
     */
    public static Color getContrastingColor(Color color) {
        double y = (299 * color.getRed() + 587 * color.getGreen() + 114 * color.getBlue()) / 1000;
        return y >= 164 ? Color.black : Color.white;
    }

    /**
     * This method calculates a color that lies between the start and end color of the given {@link ColorGradient}. And
     * will return a color sample based on the value interpreted in the range [0..scale].
     * 
     * @param colorGradient This ColorGradient defines the startColor and endColor
     * @param scale This is the scale to be applied to the ColorGradient
     * @param value This is the value to be used while applying the scale.
     * @return
     */
    public static Color getColorSample(ColorGradient colorGradient, int scale, int value) {
        int red = getColorSampleComponent(colorGradient.startColor.getRed(), colorGradient.endColor.getRed(), scale,
                value);
        int green = getColorSampleComponent(colorGradient.startColor.getGreen(), colorGradient.endColor.getGreen(),
                scale, value);
        int blue = getColorSampleComponent(colorGradient.startColor.getBlue(), colorGradient.endColor.getBlue(), scale,
                value);
        return new Color(red, green, blue);
    }

    private static int getColorSampleComponent(int startColorComponent, int endColorComponent, int scale, int value) {
        if (value == 0) {
            return startColorComponent;
        }
        if (scale == 1 || value == scale) {
            return endColorComponent;
        }
        return startColorComponent + (((endColorComponent - startColorComponent) / scale) * value);
    }

    public static class ColorGradient {
        public ColorGradient(Color startColor, Color endColor) {
            this.startColor = startColor;
            this.endColor = endColor;
        }

        public final Color startColor;

        public final Color endColor;
    }
}
