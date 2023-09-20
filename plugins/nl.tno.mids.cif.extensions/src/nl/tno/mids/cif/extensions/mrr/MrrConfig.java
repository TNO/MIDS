/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2023 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////

package nl.tno.mids.cif.extensions.mrr;

import nl.tno.mids.cif.extensions.mrr.data.MRR;

/** {@link MRR} configuration. */
public interface MrrConfig<T> {
    public int getLetterIntRepresentative(T domainLetter);

    public int getLetterCost(T domainLetter);

    public int getRepetitionCost(int repetitionCount);

    public String getPrintLetterText(T domainLetter);
}
