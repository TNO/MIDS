/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2024 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////

package nl.tno.mids.compare.input;

import java.util.List;

import org.eclipse.escet.cif.common.CifCollectUtils;
import org.eclipse.escet.cif.metamodel.cif.Specification;
import org.eclipse.escet.cif.metamodel.cif.automata.Automaton;
import org.eclipse.escet.common.java.Lists;

import com.google.common.base.Preconditions;

import net.automatalib.automata.fsa.impl.compact.CompactDFA;
import net.automatalib.automata.fsa.impl.compact.CompactNFA;
import net.automatalib.util.automata.fsa.NFAs;
import net.automatalib.words.Alphabet;
import nl.tno.mids.automatalib.extensions.cif.CifToAutomataLib;
import nl.tno.mids.automatalib.extensions.util.AutomataLibUtil;
import nl.tno.mids.cif.extensions.CIFOperations;

/**
 * Conversion functions and other utilities used during model set creation.
 */
public class ModelSetBuilderUtils {
    /**
     * Convert CIF specification to an NFA.
     * 
     * <p>
     * Only specifications with at least one automaton are supported.
     * </p>
     * 
     * <p>
     * This conversion eliminates the CIF specification to a single state space automaton, to ensure a single NFA can be
     * returned. State space generation also eliminates data and other concepts, as these can not be represented in
     * AutomataLib NFAs.
     * </p>
     * 
     * <p>
     * CIF specifications should first be converted to an NFA, to ensure NFAs are accepted as input. Subsequently,
     * {@link #convertNfaToDfa} can be used to obtain a DFA.
     * </p>
     * 
     * @param cifSpec The CIF specification to convert.
     * @return The NFA.
     */
    public static CompactNFA<String> convertCifSpecToNfa(Specification cifSpec) {
        // Ensure non-empty specification.
        List<Automaton> automata = Lists.list();
        CifCollectUtils.collectAutomata(cifSpec, automata);
        Preconditions.checkArgument(!automata.isEmpty());

        // Convert to statespace. Eliminates data and other concepts to ensure an NFA remains.
        Specification statespaceSpec = CIFOperations.convertToStateSpace(cifSpec);

        // Convert single state space automaton to an AutomataLib NFA.
        Automaton statespaceAutomaton = CIFOperations.getComponentByName(statespaceSpec, "statespace", Automaton.class);
        return CifToAutomataLib.cifAutomatonToCompactNfa(statespaceAutomaton, false);
    }

    /**
     * Converts an NFA to a minimal weak language equivalent DFA.
     * 
     * @param nfa The NFA.
     * @return The DFA.
     */
    public static CompactDFA<String> convertNfaToDfa(CompactNFA<String> nfa) {
        // Convert NFA to DFA. Copy the alphabet to ensure the NFA and the DFA don't share an alphabet instance.
        Alphabet<String> dfaAlphabet = AutomataLibUtil.copyAlphabet(nfa.getInputAlphabet());
        CompactDFA<String> dfa = NFAs.determinize(nfa, dfaAlphabet, true, false);

        // Eliminate 'tau' and ensure a minimal DFA.
        return AutomataLibUtil.normalizeWeakTrace(dfa);
    }
}
