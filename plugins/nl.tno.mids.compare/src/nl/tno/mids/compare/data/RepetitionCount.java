/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2023 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////

package nl.tno.mids.compare.data;

import java.util.Objects;

import com.github.tno.gltsdiff.operators.printers.HtmlPrinter;

/** A repetition count property for structural comparison. */
public class RepetitionCount {
    /** The number of repetitions. */
    private final int count;

    /**
     * Instantiates a repetition count property.
     * 
     * @param count The number of repetitions.
     */
    public RepetitionCount(int count) {
        this.count = count;
    }

    /** @return The number of repetitions. */
    public int getCount() {
        return count;
    }

    /** @return An HTML printer for repetition count properties. */
    public static HtmlPrinter<RepetitionCount> getHtmlPrinter() {
        return property -> String.format("(repeated %,d times)", property.getCount());
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(count);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof RepetitionCount)) {
            return false;
        }

        final RepetitionCount other = (RepetitionCount)object;

        return Objects.equals(this.count, other.count);
    }
}
