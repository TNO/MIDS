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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.eclipse.xtext.xbase.lib.Pair;
import org.junit.jupiter.api.Test;

import net.automatalib.automata.fsa.impl.compact.CompactDFA;
import net.automatalib.words.Alphabet;
import net.automatalib.words.Word;
import net.automatalib.words.impl.GrowingMapAlphabet;

/** Tests for AutomataLib utilities. */
class AutomataLibUtilTest {
    /**
     * Regression test for using renaming to change the input type of a DFA from {@link String} to
     * {@code Pair<String, Boolean>}.
     */
    @Test
    public void testThatGeneralizedRenamingWorksAsExpected() {
        CompactDFA<String> dfa = exampleAutomatonB();
        CompactDFA<Pair<String, Boolean>> renamedDfa = AutomataLibUtil.rename(dfa, s -> Pair.of(s, s.equals("c")));

        assertEquals(dfa.size(), renamedDfa.size());
        int s0 = renamedDfa.getInitialState();
        assertTrue(renamedDfa.getTransition(s0, Pair.of("a", false)) != null);
        int s1 = renamedDfa.getTransition(s0, Pair.of("a", false));
        assertTrue(renamedDfa.getTransition(s1, Pair.of("c", true)) == s0);
    }

    /**
     * Regression test for the usage of {@link AutomataLibUtil#synchronizeAlphabets} in combination with
     * {@link AutomataLibUtil#normalizeWeakTrace}.
     */
    @Test
    public void testSynchronizeAlphabetAndNormalizeWeakTrace() {
        CompactDFA<String> dfaLeft = exampleAutomatonAWithTau();
        CompactDFA<String> dfaRight = exampleAutomatonB();

        final CompactDFA<String> lhs = AutomataLibUtil.normalizeWeakTrace(dfaLeft);
        final CompactDFA<String> rhs = AutomataLibUtil.normalizeWeakTrace(dfaRight);

        assertDoesNotThrow(() -> { AutomataLibUtil.synchronizeAlphabets(lhs, rhs); });
    }

    /**
     * Regression test for minimizing an automaton with only a non-accepting initial state with some self loops.
     */
    @Test
    public void testMinimizeDFAWithInitialSelfLoops() {
        CompactDFA<String> dfa = exampleAutomatonEmptyWithLoops();

        CompactDFA<String> minimizedDfa = AutomataLibUtil.minimizeDFA(dfa);

        assertEquals(1, minimizedDfa.getStates().size());
        Integer initial = minimizedDfa.getInitialState();
        assertFalse(minimizedDfa.isAccepting(initial));
        assertEquals(0, minimizedDfa.getLocalInputs(initial).size());
    }

    /** Regression test for union computation for two automata with different alphabets. */
    @Test
    public void testUnionDifferentAlphabet() {
        CompactDFA<String> dfaLeft = exampleAutomatonA();
        CompactDFA<String> dfaRight = exampleAutomatonB();

        final CompactDFA<String> dfaResult = AutomataLibUtil.unionMinimized(dfaLeft, dfaRight);

        assertTrue(dfaResult.accepts(Word.fromSymbols("a", "b")));
        assertTrue(dfaResult.accepts(Word.fromSymbols("a", "c")));
    }

    /** Regression test for intersection computation for two automata with different alphabets. */
    @Test
    public void testIntersectionDifferentAlphabet() {
        CompactDFA<String> dfaLeft = exampleAutomatonAB();
        CompactDFA<String> dfaRight = exampleAutomatonAC();

        final CompactDFA<String> dfaResult = AutomataLibUtil.intersectionMinimized(dfaLeft, dfaRight);

        assertTrue(dfaResult.accepts(Word.fromSymbols("a", "b")));
        assertFalse(dfaResult.accepts(Word.fromSymbols("a", "c")));
        assertFalse(dfaResult.accepts(Word.fromSymbols("a", "d")));
    }

    /** Regression test for xor computation for two automata with different alphabets. */
    @Test
    public void testXorDifferentAlphabet() {
        CompactDFA<String> dfaLeft = exampleAutomatonAB();
        CompactDFA<String> dfaRight = exampleAutomatonAC();

        final CompactDFA<String> dfaResult = AutomataLibUtil.xorMinimized(dfaLeft, dfaRight);

        assertFalse(dfaResult.accepts(Word.fromSymbols("a", "b")));
        assertTrue(dfaResult.accepts(Word.fromSymbols("a", "c")));
        assertTrue(dfaResult.accepts(Word.fromSymbols("a", "d")));
    }

    /** Regression test for parallel composition computation for two automata with different alphabets. */
    @Test
    public void testParallelDifferentAlphabet() {
        CompactDFA<String> dfaLeft = exampleAutomatonAB();
        CompactDFA<String> dfaRight = exampleAutomatonAC();

        final CompactDFA<String> dfaResult = AutomataLibUtil.parallelCompositionMinimized(dfaLeft, dfaRight);

        assertTrue(dfaResult.accepts(Word.fromSymbols("a", "b")));
        assertTrue(dfaResult.accepts(Word.fromSymbols("a", "c", "d")));
        assertTrue(dfaResult.accepts(Word.fromSymbols("a", "d", "c")));
    }

    /**
     * @return An example automaton that is an 'a, b' cycle.
     */
    private CompactDFA<String> exampleAutomatonA() {
        Alphabet<String> alphabet = new GrowingMapAlphabet<String>(Arrays.asList("a", "b"));
        CompactDFA<String> dfa = new CompactDFA<>(alphabet);

        int q0 = dfa.addIntInitialState(true);
        int q1 = dfa.addIntState(false);

        dfa.setTransition(q0, alphabet.getSymbolIndex("a"), q1);
        dfa.setTransition(q1, alphabet.getSymbolIndex("b"), q0);

        return dfa;
    }

    /**
     * @return An example automaton that is an 'a, tau, b' cycle.
     */
    private CompactDFA<String> exampleAutomatonAWithTau() {
        Alphabet<String> alphabet = new GrowingMapAlphabet<String>(Arrays.asList("a", "b", "tau"));
        CompactDFA<String> dfa = new CompactDFA<>(alphabet);

        int q0 = dfa.addIntInitialState(true);
        int q1 = dfa.addIntState(false);
        int q2 = dfa.addIntState(false);

        dfa.setTransition(q0, alphabet.getSymbolIndex("a"), q1);
        dfa.setTransition(q1, alphabet.getSymbolIndex("tau"), q2);
        dfa.setTransition(q2, alphabet.getSymbolIndex("b"), q0);

        return dfa;
    }

    /**
     * @return An example automaton that is an 'a, c' cycle.
     */
    private CompactDFA<String> exampleAutomatonB() {
        Alphabet<String> alphabet = new GrowingMapAlphabet<String>(Arrays.asList("a", "c"));
        CompactDFA<String> dfa = new CompactDFA<>(alphabet);

        int q0 = dfa.addIntInitialState(true);
        int q1 = dfa.addIntState(false);

        dfa.setTransition(q0, alphabet.getSymbolIndex("a"), q1);
        dfa.setTransition(q1, alphabet.getSymbolIndex("c"), q0);

        return dfa;
    }

    /**
     * @return An example automaton that has an empty language with self loops in the initial state.
     */
    private CompactDFA<String> exampleAutomatonEmptyWithLoops() {
        Alphabet<String> alphabet = new GrowingMapAlphabet<String>(Arrays.asList("a", "b", "tau"));
        CompactDFA<String> dfa = new CompactDFA<>(alphabet);

        int q0 = dfa.addIntInitialState(false);

        dfa.setTransition(q0, alphabet.getSymbolIndex("a"), q0);
        dfa.setTransition(q0, alphabet.getSymbolIndex("tau"), q0);
        dfa.setTransition(q0, alphabet.getSymbolIndex("b"), q0);

        return dfa;
    }

    /**
     * @return An example automaton that is an 'a, (b|c)' cycle.
     */
    private CompactDFA<String> exampleAutomatonAB() {
        Alphabet<String> alphabet = new GrowingMapAlphabet<String>(Arrays.asList("a", "b", "c"));
        CompactDFA<String> dfa = new CompactDFA<>(alphabet);

        int q0 = dfa.addIntInitialState(true);
        int q1 = dfa.addIntState(false);

        dfa.setTransition(q0, alphabet.getSymbolIndex("a"), q1);
        dfa.setTransition(q1, alphabet.getSymbolIndex("b"), q0);
        dfa.setTransition(q1, alphabet.getSymbolIndex("c"), q0);

        return dfa;
    }

    /**
     * @return An example automaton that is an 'a, (b|d)' cycle.
     */
    private CompactDFA<String> exampleAutomatonAC() {
        Alphabet<String> alphabet = new GrowingMapAlphabet<String>(Arrays.asList("a", "b", "d"));
        CompactDFA<String> dfa = new CompactDFA<>(alphabet);

        int q0 = dfa.addIntInitialState(true);
        int q1 = dfa.addIntState(false);

        dfa.setTransition(q0, alphabet.getSymbolIndex("a"), q1);
        dfa.setTransition(q1, alphabet.getSymbolIndex("b"), q0);
        dfa.setTransition(q1, alphabet.getSymbolIndex("d"), q0);

        return dfa;
    }
}
