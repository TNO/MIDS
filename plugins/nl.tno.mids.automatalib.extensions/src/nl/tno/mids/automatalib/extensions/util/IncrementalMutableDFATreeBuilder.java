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

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import net.automatalib.automata.fsa.DFA;
import net.automatalib.automata.fsa.MutableDFA;
import net.automatalib.automata.fsa.impl.FastDFA;
import net.automatalib.automata.fsa.impl.FastDFAState;
import net.automatalib.automata.fsa.impl.compact.CompactDFA;
import net.automatalib.incremental.ConflictException;
import net.automatalib.incremental.dfa.Acceptance;
import net.automatalib.incremental.dfa.IncrementalDFABuilder;
import net.automatalib.util.automata.copy.AutomatonCopyMethod;
import net.automatalib.util.automata.copy.AutomatonLowLevelCopy;
import net.automatalib.words.Alphabet;
import net.automatalib.words.Word;
import net.automatalib.words.WordBuilder;
import net.automatalib.words.impl.GrowingMapAlphabet;

/**
 * A wrapper around the FastDFA class to build DFAs incrementally. Insertions of words containing unknown symbols are
 * resolved by automatically modifying the alphabet. Returns both FastDFAs and CompactDFAs.
 * 
 * @param <I> the type used to represent symbols in the alphabet.
 */
public class IncrementalMutableDFATreeBuilder<I> implements IncrementalDFABuilder<I> {
    FastDFA<I> treeDFA;

    // extend DFA by tracking rejectingStates
    Set<FastDFAState> rejectingStates;

    // Caches
    FastDFA<I> cachedFastDFA;

    CompactDFA<I> cachedCompactDFA;

    /**
     * Construct a new {@link IncrementalMutableDFATreeBuilder} with empty initial alphabet.
     */
    public IncrementalMutableDFATreeBuilder() {
        this(new GrowingMapAlphabet<>());
    }

    /**
     * Construct a new {@link IncrementalMutableDFATreeBuilder} with given initial alphabet.
     * 
     * @param inputAlphabet Initial alphabet of the builder.
     */
    public IncrementalMutableDFATreeBuilder(Alphabet<I> inputAlphabet) {
        this.treeDFA = new FastDFA<>(inputAlphabet);
        treeDFA.addInitialState();
        this.rejectingStates = new HashSet<FastDFAState>();
    }

    @Override
    public void addAlphabetSymbol(I symbol) {
        treeDFA.addAlphabetSymbol(symbol);
    }

    /**
     * @return {@link Alphabet} of the current DFA.
     */
    public Alphabet<I> getInputAlphabet() {
        return treeDFA.getInputAlphabet();
    }

    /**
     * @param state State to retrieve acceptance for.
     * @return {@link Acceptance} value for given state in current DFA.
     */
    public Acceptance getAcceptance(FastDFAState state) {
        if (state.isAccepting()) {
            return Acceptance.TRUE;
        } else if (rejectingStates.contains(state)) {
            return Acceptance.FALSE;
        } else {
            return Acceptance.DONT_KNOW;
        }
    }

    @Override
    public Word<I> findSeparatingWord(DFA<?, I> target, Collection<? extends I> inputs, boolean omitUndefined) {
        // allows generalizing the type of state from ? to S.
        return doFindSeperatingWord(target, inputs, omitUndefined);
    }

    protected <S> Word<I> doFindSeperatingWord(DFA<S, I> target, Collection<? extends I> inputs,
            boolean omitUndefined)
    {
        S automatonInit = target.getInitialState();
        if (getAcceptance(treeDFA.getInitialState()).conflicts(target.isAccepting(automatonInit))) {
            return Word.epsilon();
        }

        Deque<Record<S, I>> dfsStack = new ArrayDeque<>();
        dfsStack.push(new Record<>(automatonInit, treeDFA.getInitialState(), null, inputs.iterator()));

        while (!dfsStack.isEmpty()) {
            Record<S, I> rec = dfsStack.peek();
            if (!rec.inputIt.hasNext()) {
                dfsStack.pop();
                continue;
            }
            I input = rec.inputIt.next();

            FastDFAState succ = treeDFA.getSuccessor(rec.treeNode, input);
            if (succ == null) {
                continue;
            }

            S automatonSucc = (rec.automatonState == null) ? null : target.getTransition(rec.automatonState, input);
            if (automatonSucc == null && omitUndefined) {
                continue;
            }

            boolean succAcc = (automatonSucc != null) && target.isAccepting(automatonSucc);

            if (getAcceptance(succ).conflicts(succAcc)) {
                WordBuilder<I> wb = new WordBuilder<>(dfsStack.size());
                wb.append(input);

                dfsStack.pop();
                while (!dfsStack.isEmpty()) {
                    wb.append(rec.incomingInput);
                    rec = dfsStack.pop();
                }
                return wb.reverse().toWord();
            }

            dfsStack.push(new Record<>(automatonSucc, succ, input, inputs.iterator()));
        }

        return null;
    }

    protected static final class Record<S, I> {
        public final S automatonState;

        public final FastDFAState treeNode;

        public final I incomingInput;

        public final Iterator<? extends I> inputIt;

        public Record(S automatonState, FastDFAState treeNode, I incomingInput, Iterator<? extends I> inputIt) {
            this.automatonState = automatonState;
            this.treeNode = treeNode;
            this.incomingInput = incomingInput;
            this.inputIt = inputIt;
        }
    }

    @Override
    public boolean hasDefinitiveInformation(Word<? extends I> word) {
        return lookup(word) != Acceptance.DONT_KNOW;
    }

    @Override
    public Acceptance lookup(Word<? extends I> inputWord) {
        FastDFAState resultState = treeDFA.getState(inputWord);
        if (resultState == null) {
            return Acceptance.DONT_KNOW;
        } else if (resultState.isAccepting())
            return Acceptance.TRUE;
        else if (rejectingStates.contains(resultState)) {
            return Acceptance.FALSE;
        } else {
            return Acceptance.DONT_KNOW;
        }
    }

    // INSERTION

    /**
     * Inserts a new word in the automaton with the given acceptance formula. Contrary to other IncrementalDFABuilder
     * implementations, this implementation modifies the alphabet if need be.
     * 
     * {@inheritDoc}
     */
    @Override
    public void insert(Word<? extends I> word, boolean accepting) throws ConflictException {
        FastDFAState state = treeDFA.getInitialState();
        I symbol;
        // Traverse over word, tracking current state in `state`.
        for (int i = 0; i < word.size(); i++) {
            symbol = word.getSymbol(i);
            treeDFA.addAlphabetSymbol(symbol); // implementation checks for duplicates
            FastDFAState next = treeDFA.getSuccessor(state, symbol);

            if (next == null) {
                emptyCache();
                // Traversal cannot continue as no state can be reached using word[i].
                // Add suffix w[i..] to dfa and break.
                for (int j = i; j < word.size(); j++) {
                    symbol = word.getSymbol(j);
                    treeDFA.addAlphabetSymbol(symbol); // implementation checks for duplicates
                    next = treeDFA.addState();
                    treeDFA.addTransition(state, symbol, next, null);
                    state = next;
                }
                break;
            }
            state = next;
        }
        // Final state must be set accepting or rejecting.
        if (accepting) {
            if (rejectingStates.contains(state)) {
                throw new ConflictException("Cannot accept word " + word.toString() + " as it is already rejected");
            }
            if (!state.isAccepting()) {
                emptyCache();
                state.setAccepting(true);
            }
        } else {
            if (state.isAccepting()) {
                throw new ConflictException("Cannot reject word " + word.toString() + " as it is already accepted");
            }
            if (rejectingStates.add(state)) {
                emptyCache();
            }
        }
    }

    /**
     * Inserts a word in the automaton. Modifies the alphabet if need be.
     * 
     * {@inheritDoc}
     */
    @Override
    public void insert(Word<? extends I> word) throws ConflictException {
        insert(word, true);
    }

    /**
     * Inserts multiple words in the alphabet. Modifies the alphabet if need be.
     * 
     * @param words collection of words to be inserted
     */
    public void insert(Collection<? extends Word<? extends I>> words) {
        for (Word<? extends I> word: words) {
            insert(word, true);
        }
    }

    /** COPY FUNCTIONALITY **/

    /**
     * Returns a copy of the build DFA. This copy has a binary acceptance condition and hence the rejecting and don't
     * know states are merged during this copy. The returned object is cached for subsequent calls, but changes to this
     * copy are not reflected in the builder itself. A clean version can be obtained using {@link #getFastDFA(true)} or
     * {@link #getCompactDFA(true)}.
     * 
     * @return a FastDFA accepting the same language as the incrementalbuilder
     */
    public FastDFA<I> getFastDFA() {
        if (cachedFastDFA != null) {
            return cachedFastDFA;
        }
        cachedFastDFA = new FastDFA<>(getInputAlphabet());
        AutomatonLowLevelCopy.copy(AutomatonCopyMethod.STATE_BY_STATE, this.treeDFA, getInputAlphabet(), cachedFastDFA);
        return cachedFastDFA;
    }

    /**
     * Returns a copy of the build DFA. This copy has a binary acceptance condition and hence the rejecting and don't
     * know states are merged during this copy. The returned object is cached for subsequent calls, but changes to this
     * copy are not reflected in the builder itself. A clean version can be obtained using {@link #getFastDFA(true)} or
     * {@link #getCompactDFA(true)}.
     * 
     * @param clearCache if {@code true}, a new copy of the contained DFA is returned, otherwise the returned DFA may be
     *     the same as one retrieved earlier.
     * 
     * @return a FastDFA accepting the same language as the incrementalbuilder
     */
    public FastDFA<I> getFastDFA(boolean clearCache) {
        if (clearCache) {
            emptyCache();
        }
        return getFastDFA();
    }

    /**
     * Returns a copy of the build DFA. This copy has a binary acceptance condition and hence the rejecting and don't
     * know states are merged during this copy. The returned object is cached for subsequent calls, but changes to this
     * copy are not reflected in the builder itself. A clean version can be obtained using {@link #getFastDFA(true)} or
     * {@link #getCompactDFA(true)}.
     * 
     * @return a CompactDFA accepting the same language as the incrementalbuilder
     */
    public CompactDFA<I> getCompactDFA() {
        if (cachedCompactDFA != null) {
            return cachedCompactDFA;
        }
        cachedCompactDFA = new CompactDFA<>(getInputAlphabet(), treeDFA.size());
        AutomatonLowLevelCopy.copy(AutomatonCopyMethod.STATE_BY_STATE, this.treeDFA, getInputAlphabet(),
                cachedCompactDFA);
        return cachedCompactDFA;
    }

    /**
     * Returns a copy of the build DFA. This copy has a binary acceptance condition and hence the rejecting and don't
     * know states are merged during this copy. The returned object is cached for subsequent calls, but changes to this
     * copy are not reflected in the builder itself. A clean version can be obtained using {@link #getFastDFA(true)} or
     * {@link #getCompactDFA(true)}.
     * 
     * @param clearCache if {@code true}, a new copy of the contained DFA is returned, otherwise the returned DFA may be
     *     the same as one retrieved earlier.
     * 
     * @return a CompactDFA accepting the same language as the incrementalbuilder
     */
    public CompactDFA<I> getCompactDFA(boolean clearCache) {
        if (clearCache) {
            emptyCache();
        }
        return getCompactDFA();
    }

    /**
     * @param <S> Type of state of returned DFA.
     * @param <A> Type of DFA returned.
     * @param dfa Automaton to contain DFA from builder.
     * @return Automaton containing a copy of DFA contained in builder.
     */
    public <S, A extends MutableDFA<S, I>> A getDFA(A dfa) {
        AutomatonLowLevelCopy.copy(AutomatonCopyMethod.STATE_BY_STATE, this.treeDFA, getInputAlphabet(), dfa);
        return dfa;
    }

    /**
     * Clear DFA cache of builder.
     */
    public void emptyCache() {
        this.cachedCompactDFA = null;
        this.cachedFastDFA = null;
    }

    // Views

    @Override
    public GraphView<I, ?, ?> asGraph() {
        throw new UnsupportedOperationException();
    }

    @Override
    public TransitionSystemView<?, I, ?> asTransitionSystem() {
        throw new UnsupportedOperationException();
    }
}
