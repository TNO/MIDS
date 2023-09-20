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

import java.util.List;

import org.eclipse.escet.cif.metamodel.cif.automata.Automaton;

import nl.tno.mids.cif.extensions.mrr.data.LetterMRR;

/**
 * Internal data used by {@link MrrToCif} while adapting the CIF specification.
 */
class MrrToCifData {
    /** The CIF automaton to which the MRR applies. */
    public final Automaton automaton;

    /** The original word to which the MRR applies. */
    public final List<LetterMRR<CifMrrLetter>> word;

    public MrrToCifData(Automaton automaton, List<LetterMRR<CifMrrLetter>> word) {
        this.automaton = automaton;
        this.word = word;
    }
}
