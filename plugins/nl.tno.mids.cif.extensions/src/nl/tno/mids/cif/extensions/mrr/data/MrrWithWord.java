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

import java.util.List;

public class MrrWithWord<T> {
    public final List<LetterMRR<T>> word;

    public final MRR<T> mrr;

    public MrrWithWord(List<LetterMRR<T>> word, MRR<T> mrr) {
        this.word = word;
        this.mrr = mrr;
    }
}
