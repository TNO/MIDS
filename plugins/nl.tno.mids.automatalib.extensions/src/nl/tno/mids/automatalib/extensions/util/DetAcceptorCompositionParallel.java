/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2024 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////

package nl.tno.mids.automatalib.extensions.util;

import net.automatalib.automata.concepts.InputAlphabetHolder;
import net.automatalib.commons.util.Pair;
import net.automatalib.ts.acceptors.DeterministicAcceptorTS;
import net.automatalib.util.ts.acceptors.AcceptanceCombiner;
import net.automatalib.util.ts.acceptors.DetAcceptorComposition;

/**
 * Represent a parallel composition of two deterministic acceptors with defined input alphabets.
 *
 * @param <S1> Type of the states of the first acceptor.
 * @param <S2> Type of the states of the second acceptor.
 * @param <I> Type of the inputs for both acceptors.
 * @param <A1> Type of first acceptor.
 * @param <A2> Type of second acceptor.
 */
public class DetAcceptorCompositionParallel<S1, S2, I,
        A1 extends DeterministicAcceptorTS<S1, I> & InputAlphabetHolder<I>,
        A2 extends DeterministicAcceptorTS<S2, I> & InputAlphabetHolder<I>>
        extends DetAcceptorComposition<S1, S2, I, A1, A2>
{
    /**
     * Construct a parallel composition of two acceptors.
     * 
     * @param ts1 First acceptor.
     * @param ts2 Second acceptor.
     */
    public DetAcceptorCompositionParallel(A1 ts1, A2 ts2) {
        super(ts1, ts2, AcceptanceCombiner.AND);
    }

    @Override
    public Pair<S1, S2> getTransition(Pair<S1, S2> state, I input) {
        S1 s1 = state.getFirst();

        S1 t1 = (s1 == null) ? null
                : (ts1.getInputAlphabet().containsSymbol(input) ? ts1.getTransition(s1, input) : s1);
        if (t1 == null && !allowPartial) {
            return null;
        }

        S2 s2 = state.getSecond();

        S2 t2 = (s2 == null) ? null
                : (ts2.getInputAlphabet().containsSymbol(input) ? ts2.getTransition(s2, input) : s2);
        if (t2 == null && !allowPartial) {
            return null;
        }

        return t1 == null && t2 == null ? null : Pair.of(t1, t2);
    }
}
