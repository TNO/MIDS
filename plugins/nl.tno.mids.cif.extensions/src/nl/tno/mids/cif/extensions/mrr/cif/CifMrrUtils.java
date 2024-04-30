/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2024 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////

package nl.tno.mids.cif.extensions.mrr.cif;

import static org.eclipse.escet.common.java.Sets.setc;

import java.util.Set;

import org.eclipse.escet.cif.metamodel.cif.automata.Automaton;

import com.google.common.base.Preconditions;

import nl.tno.mids.cif.extensions.mrr.data.ConcatenationMRR;
import nl.tno.mids.cif.extensions.mrr.data.LetterMRR;
import nl.tno.mids.cif.extensions.mrr.data.MRR;
import nl.tno.mids.cif.extensions.mrr.data.RepetitionMRR;

public class CifMrrUtils {
    /**
     * Get the CIF automaton for the MRR. Checks that there is a unique one.
     *
     * @param mrr The MRR.
     * @return The CIF automaton.
     */
    public static Automaton getAutomaton(MRR<CifMrrLetter> mrr) {
        Set<Automaton> automata = setc(1);
        getAutomata(mrr, automata);
        Preconditions.checkArgument(automata.size() == 1);
        Automaton aut = automata.iterator().next();
        return aut;
    }

    private static void getAutomata(MRR<CifMrrLetter> mrr, Set<Automaton> automata) {
        if (mrr instanceof ConcatenationMRR<?>) {
            for (MRR<CifMrrLetter> child: ((ConcatenationMRR<CifMrrLetter>)mrr).sequence) {
                getAutomata(child, automata);
            }
        } else if (mrr instanceof RepetitionMRR<?>) {
            getAutomata(((RepetitionMRR<CifMrrLetter>)mrr).getChild(), automata);
        } else if (mrr instanceof LetterMRR<?>) {
            automata.add(((LetterMRR<CifMrrLetter>)mrr).letter.aut);
        } else {
            throw new RuntimeException("Unexpected MRR: " + mrr.getClass().getName());
        }
    }
}
