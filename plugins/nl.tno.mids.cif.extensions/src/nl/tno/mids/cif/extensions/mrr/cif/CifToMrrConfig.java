/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2023 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////

package nl.tno.mids.cif.extensions.mrr.cif;

import static org.eclipse.escet.common.java.Maps.map;

import java.util.Map;

import org.eclipse.escet.common.java.Pair;

import nl.tno.mids.cif.extensions.mrr.MrrConfig;

public class CifToMrrConfig implements MrrConfig<CifMrrLetter> {
    private final int letterCost;

    private final int repetitionCost;

    private int nextFreeNumber = 0;

    private Map<Pair<String, Boolean>, Integer> mapping = map();

    public CifToMrrConfig() {
        this(1, 1);
    }

    public CifToMrrConfig(int letterCost, int repetitionCost) {
        this.letterCost = letterCost;
        this.repetitionCost = repetitionCost;
    }

    @Override
    public int getLetterIntRepresentative(CifMrrLetter domainLetter) {
        Pair<String, Boolean> key = new Pair<>(domainLetter.name, domainLetter.isTargetLocMarked());

        // Mapping is based on event names and markings of target locations,
        // matching the print letter text.
        Integer number = mapping.get(key);
        if (number == null) {
            number = nextFreeNumber;
            nextFreeNumber++;
            mapping.put(key, number);
        }
        return number;
    }

    @Override
    public int getLetterCost(CifMrrLetter domainLetter) {
        return letterCost;
    }

    @Override
    public int getRepetitionCost(int repetitionCount) {
        return repetitionCost;
    }

    @Override
    public String getPrintLetterText(CifMrrLetter domainLetter) {
        return domainLetter.name + (domainLetter.isTargetLocMarked() ? "/marked" : "");
    }
}
