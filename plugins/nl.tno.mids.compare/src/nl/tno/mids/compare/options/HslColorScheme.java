/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2024 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////

package nl.tno.mids.compare.options;

import nl.tno.mids.compare.output.color.GradientHslColorScheme;
import nl.tno.mids.compare.output.color.IntuitiveHslColorScheme;
import nl.tno.mids.compare.output.color.LargeRangeHslColorScheme;

/** HSL color scheme used to display range of variations. */
public enum HslColorScheme {
    /** Color scheme based on intuitive colors. */
    INTUITIVE(IntuitiveHslColorScheme.class, "Intuitive color scheme (green -> red)"),

    /** Color scheme based on large color difference between variants. */
    LARGE_RANGE(LargeRangeHslColorScheme.class, "Large range color scheme (blue -> purple)");

    /** {@link Class} implementing the selected color scheme. */
    public final Class<? extends GradientHslColorScheme> colorSchemeClass;

    /** Description of color scheme. */
    public final String description;

    HslColorScheme(Class<? extends GradientHslColorScheme> colorSchemeClass, String description) {
        this.colorSchemeClass = colorSchemeClass;
        this.description = description;
    }

    /** @return {@link GradientHslColorScheme} implementing the chosen color scheme. */
    public GradientHslColorScheme getSampler() {
        try {
            return colorSchemeClass.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Exception while instantiating HslColorScheme.", e);
        }
    }
}
