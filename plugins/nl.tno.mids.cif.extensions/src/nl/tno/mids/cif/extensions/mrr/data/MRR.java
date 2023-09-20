/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2023 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////

package nl.tno.mids.cif.extensions.mrr.data;

/**
 * Minimal repetition representation for a string/word.
 *
 * @param <T> The type of the letters composing the string/word.
 */
public abstract class MRR<T> {
    @Override
    public String toString() {
        return toMultiLineString();
    }

    public abstract String toSingleLineString();

    public abstract String toMultiLineString();

    /** Returns the cost of the MRR. */
    public abstract int getCost();

    /**
     * Returns the size of the MRR, as number of domain letters represented by it. Domain letters that are merged
     * together are counted separately.
     */
    public abstract int getDomainSize();
}
