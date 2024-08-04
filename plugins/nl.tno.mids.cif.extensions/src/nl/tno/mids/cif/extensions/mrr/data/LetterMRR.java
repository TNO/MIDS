/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2024 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////

package nl.tno.mids.cif.extensions.mrr.data;

import nl.tno.mids.cif.extensions.mrr.MrrConfig;

/** Single letter {@link MRR}. */
public class LetterMRR<T> extends MRR<T> {
    public final T letter;

    public final int letterRepr;

    final int cost;

    final String printText; // Only used for textual representation of MRR.

    public LetterMRR(T letter, MrrConfig<T> config) {
        this.letter = letter;
        this.letterRepr = config.getLetterIntRepresentative(letter);
        this.cost = config.getLetterCost(letter);
        this.printText = config.getPrintLetterText(letter);
    }

    @Override
    public int getCost() {
        return cost;
    }

    @Override
    public int getDomainSize() {
        return 1;
    }

    @Override
    public String toSingleLineString() {
        return printText;
    }

    @Override
    public String toMultiLineString() {
        return printText;
    }
}
