/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2023 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////

package nl.tno.mids.automatalib.extensions.util;

import net.automatalib.automata.AutomatonCreator;
import net.automatalib.automata.fsa.impl.FastDFA;
import net.automatalib.words.Alphabet;

/**
 * Wrapper for creating FastDFA instances.
 * 
 * @param <I> Type of automaton states.
 */
public class FastDfaCreator<I> implements AutomatonCreator<FastDFA<I>, I> {
    @Override
    public FastDFA<I> createAutomaton(Alphabet<I> alphabet, int numStates) {
        return new FastDFA<>(alphabet);
    }

    @Override
    public FastDFA<I> createAutomaton(Alphabet<I> alphabet) {
        return new FastDFA<>(alphabet);
    }
}
