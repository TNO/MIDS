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

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import net.automatalib.SupportsGrowingAlphabet;
import net.automatalib.automata.Automaton;
import net.automatalib.automata.DeterministicAutomaton;
import net.automatalib.automata.MutableAutomaton;
import net.automatalib.automata.ShrinkableAutomaton;
import net.automatalib.automata.UniversalAutomaton;
import net.automatalib.automata.UniversalFiniteAlphabetAutomaton;
import net.automatalib.automata.concepts.InputAlphabetHolder;
import net.automatalib.automata.concepts.StateLocalInput;
import net.automatalib.automata.fsa.DFA;
import net.automatalib.automata.fsa.MutableDFA;
import net.automatalib.automata.fsa.NFA;
import net.automatalib.automata.fsa.impl.FastDFA;
import net.automatalib.automata.fsa.impl.FastDFAState;
import net.automatalib.automata.fsa.impl.FastNFA;
import net.automatalib.automata.fsa.impl.compact.CompactDFA;
import net.automatalib.automata.fsa.impl.compact.CompactNFA;
import net.automatalib.automata.graphs.TransitionEdge;
import net.automatalib.automata.simple.SimpleAutomaton;
import net.automatalib.graphs.Graph;
import net.automatalib.graphs.concepts.NodeIDs;
import net.automatalib.ts.DeterministicTransitionSystem;
import net.automatalib.ts.acceptors.AcceptorTS;
import net.automatalib.ts.acceptors.DeterministicAcceptorTS;
import net.automatalib.util.automata.copy.AutomatonCopyMethod;
import net.automatalib.util.automata.copy.AutomatonLowLevelCopy;
import net.automatalib.util.automata.fsa.DFAs;
import net.automatalib.util.automata.fsa.NFAs;
import net.automatalib.util.automata.minimizer.paigetarjan.PaigeTarjanMinimization;
import net.automatalib.util.ts.acceptors.AcceptanceCombiner;
import net.automatalib.util.ts.copy.TSCopy;
import net.automatalib.util.ts.traversal.TSTraversal;
import net.automatalib.util.ts.traversal.TSTraversalMethod;
import net.automatalib.words.Alphabet;
import net.automatalib.words.impl.GrowingMapAlphabet;

/**
 * Utility functions for AutomataLib automata.
 */
public class AutomataLibUtil {
    // ----------
    // mCRL2 file format
    // See https://www.mcrl2.org/web/user_manual/language_reference/index.html
    // ----------
    private static final Pattern MCRL2_IDENTIFIER_PATTERN = Pattern.compile("[A-Za-z_][A-Za-z_0-9']*");

    /**
     * Write an mCRL2 specification.
     *
     * <p>
     * <strong>WARNING</strong>: The mCRL2 format doesn't distinguish between accepting and non-accepting states.
     * Therefore, be careful when writing a finite state automaton (FSA), e.g. a DFA or an NFA. The FSA should only have
     * accepting states. This is <strong>NOT</strong> checked by this method!
     * </p>
     *
     * @param automaton The automaton. For instance a {@link CompactDFA} or {@link FastDFA}.
     * @param stream Stream to write. No need to provide a buffered stream.
     * @throws IllegalArgumentException If the automaton does not have exactly one initial state.
     * @throws IllegalArgumentException If a transition label is not a valid mCRL2 identifier.
     * @throws IOException In case of an I/O error.
     */
    public static <S, T, A extends Automaton<S, String, T> & InputAlphabetHolder<String>> void writeMcrl2(A automaton,
            OutputStream stream) throws IOException
    {
        // Get graph view of automaton.
        Graph<S, TransitionEdge<String, T>> graph = automaton.transitionGraphView(automaton.getInputAlphabet());

        // Get node ID mapper. Expensive operation for certain automata representation,
        // so reuse this.
        NodeIDs<S> nodeIds = graph.nodeIDs();

        // Get single initial state.
        if (automaton.getInitialStates().size() != 1) {
            throw new IllegalArgumentException("Expected automaton with one initial state, found "
                    + automaton.getInitialStates().size() + " initial states.");
        }
        S initialState = automaton.getInitialStates().iterator().next();
        int initialStateId = nodeIds.getNodeId(initialState);
        if (initialStateId < 0) {
            throw new IllegalArgumentException("Provided automaton does not have an initial state.");
        }

        // Check alphabet.
        Alphabet<String> alphabet = automaton.getInputAlphabet();
        for (String action: alphabet) {
            Matcher matcher = MCRL2_IDENTIFIER_PATTERN.matcher(action);
            if (!matcher.matches()) {
                throw new IllegalArgumentException("Action name \"" + action + "\" is not a valid mCRL2 identifier.");
            }
        }

        // Write output.
        try (Writer writer = new BufferedWriter(new OutputStreamWriter(stream))) {
            // Action declarations for input alphabet.
            for (String action: alphabet) {
                writer.write("act ");
                writer.write(action);
                writer.write(";\n");
            }

            // Process declarations for nodes.
            for (S node: graph.getNodes()) {
                // Declaration.
                writer.write("proc s");
                writer.write(Integer.toString(nodeIds.getNodeId(node)));
                writer.write(" = ");

                // Edges.
                Collection<TransitionEdge<String, T>> outgoingEdges = graph.getOutgoingEdges(node);
                if (outgoingEdges == null || outgoingEdges.isEmpty()) {
                    // No edges.
                    writer.write("delta;");
                } else {
                    // Has edges.
                    Iterator<TransitionEdge<String, T>> it = outgoingEdges.iterator();
                    while (it.hasNext()) {
                        TransitionEdge<String, T> edge = it.next();
                        writer.write(edge.getInput());
                        writer.write(" . ");
                        S target = graph.getTarget(edge);
                        writer.write("s" + Integer.toString(nodeIds.getNodeId(target)));
                        if (it.hasNext()) {
                            writer.write(" + ");
                        } else {
                            writer.write(";");
                        }
                    }
                }
                writer.write("\n");
            }

            // Initial process for initial state.
            writer.write("init s");
            writer.write(Integer.toString(initialStateId));
            writer.write(";\n");
        }
    }

    // ----------
    // Rename
    // ----------

    /**
     * Renames the input symbols of a DFA.
     * <p>
     * The specified {@code renameFunc} function must be <u>injective</u>.
     * </p>
     *
     * @param dfa The original DFA.
     * @param renameFunc The renaming function that maps original input symbols to new input symbols.
     * @return The new DFA, with renamed inputs.
     */
    public static <T, U> CompactDFA<U> rename(CompactDFA<T> dfa, Function<T, U> renameFunc) {
        return rename(dfa, a -> new CompactDFA<>(a), dfa.getInputAlphabet(), renameFunc);
    }

    /**
     * Renames the input symbols of an NFA.
     *
     * @param nfa The original NFA.
     * @param renameFunc The renaming function that maps original input symbols to new input symbols.
     * @return The new NFA, with renamed inputs.
     */
    public static <T, U> CompactNFA<U> rename(CompactNFA<T> nfa, Function<T, U> renameFunc) {
        return rename(nfa, a -> new CompactNFA<>(a), nfa.getInputAlphabet(), renameFunc);
    }

    /**
     * Renames the input symbols of a DFA.
     * <p>
     * The specified {@code renameFunc} function must be <u>injective</u>.
     * </p>
     *
     * @param dfa The original DFA.
     * @param renameFunc The renaming function that maps original input symbols to new input symbols.
     * @return The new DFA, with renamed inputs.
     */
    public static <T, U> FastDFA<U> rename(FastDFA<T> dfa, Function<T, U> renameFunc) {
        return rename(dfa, a -> new FastDFA<>(a), dfa.getInputAlphabet(), renameFunc);
    }

    /**
     * Renames the input symbols of an NFA.
     *
     * @param nfa The original NFA.
     * @param renameFunc The renaming function that maps original input symbols to new input symbols.
     * @return The new NFA, with renamed inputs.
     */
    public static <T, U> FastNFA<U> rename(FastNFA<T> nfa, Function<T, U> renameFunc) {
        return rename(nfa, a -> new FastNFA<>(a), nfa.getInputAlphabet(), renameFunc);
    }

    /**
     * Renames the input symbols of a finite state automaton (FSA), e.g. {@link DFA} or {@link NFA}.
     * <p>
     * If this function is used over deterministic automata then {@code renameFunc} must be <u>injective</u>.
     * </p>
     * 
     * @param originalFsa The original FSA.
     * @param newFsaCreator Function to create a new FSA to contains the result of renaming.
     * @param originalAlphabet The input alphabet of the original FSA.
     * @param renameFunc The renaming function that maps original input symbols to new input symbols.
     * @return The new FSA, with renamed inputs.
     */
    public static <SI, SO, TI, TO, I extends UniversalAutomaton<SI, TI, SI, Boolean, Void>,
            O extends MutableAutomaton<SO, TO, SO, Boolean, Void>> O rename(I originalFsa,
                    Function<Alphabet<TO>, O> newFsaCreator, Alphabet<TI> originalAlphabet, Function<TI, TO> renameFunc)
    {
        // Get new alphabet.
        Set<TO> newAlphabet = new LinkedHashSet<>(originalAlphabet.size());
        for (TI input: originalAlphabet) {
            newAlphabet.add(renameFunc.apply(input));
        }

        // Create new FSA.
        O newFsa = newFsaCreator.apply(new GrowingMapAlphabet<>(newAlphabet));

        // Ensure that 'renameFunc' is injective whenever 'newFsa' is deterministic.
        if (newFsa instanceof DeterministicAutomaton) {
            Preconditions.checkArgument(originalAlphabet.size() == newAlphabet.size(),
                    "Expected the rename function to be injective.");
        }

        // State property (acceptance) is simply copied. There are no transition properties.
        Function<SI, Boolean> spMapping = s -> originalFsa.getStateProperty(s);
        Function<SI, Void> tpMapping = t -> null;

        // Copy.
        AutomatonLowLevelCopy.rawCopy(AutomatonCopyMethod.STATE_BY_STATE, originalFsa, originalAlphabet, newFsa,
                renameFunc, spMapping, tpMapping);
        return newFsa;
    }

    /**
     * Copies the given alphabet to a new {@link GrowingMapAlphabet}.
     * 
     * <p>
     * Copying alphabets can be useful when creating new automata, as then they don't share the same alphabet instance
     * as some other automaton from which the alphabet was obtained. If the alphabets are then modified, the other
     * automaton is not affected, preventing hard to debug issues.
     * </p>
     * 
     * @param <I> The type of the alphabet symbols.
     * @param alphabet The alphabet to copy.
     * @return The copy of the alphabet.
     */
    public static <I> Alphabet<I> copyAlphabet(Alphabet<I> alphabet) {
        return new GrowingMapAlphabet<I>(alphabet);
    }

    /**
     * Copy {@link FastDFA} to {@link CompactDFA} using a new input alphabet.
     * 
     * @param source Source {@link FastDFA} to copy.
     * @param newInputs {@link Alphabet} to use for new automaton.
     * @return {@link CompactDFA} with states identical to source.
     */
    public static <I> CompactDFA<I> copy(FastDFA<I> source, Alphabet<I> newInputs) {
        HashMap<FastDFAState, Integer> stateMapping = new HashMap<>();

        return copy(source, newInputs, stateMapping);
    }

    /**
     * Copy {@link FastDFA} to {@link CompactDFA} using a new input alphabet and returning a mapping of source states to
     * target states.
     * 
     * @param source Source {@link FastDFA} to copy.
     * @param newInputs {@link Alphabet} to use for new automaton.
     * @param stateMapping {@link Map} to contain mapping from source to target states.
     * @return {@link CompactDFA} with states identical to source.
     */
    private static <I> CompactDFA<I> copy(FastDFA<I> source, Alphabet<I> newInputs,
            Map<FastDFAState, Integer> stateMapping)
    {
        CompactDFA<I> newDFA = new CompactDFA<>(copyAlphabet(newInputs));
        for (FastDFAState state: source.getStates()) {
            Integer newState = newDFA.addState(source.isAccepting(state));
            stateMapping.put(state, newState);
        }

        newDFA.setInitialState(stateMapping.get(source.getInitialState()));

        for (FastDFAState state: source.getStates()) {
            for (I input: newInputs) {
                if (!source.getInputAlphabet().contains(input)) {
                    throw new IllegalArgumentException(
                            "Input: " + input + " not in alphabet " + source.getInputAlphabet());
                }
                source.getSuccessor(state, input);
                Integer target = stateMapping.get(source.getSuccessor(state, input));
                Integer sourcestate = stateMapping.get(state);
                newDFA.addTransition(sourcestate, input, target);
            }
        }
        return newDFA;
    }

    /**
     * Copy {@link CompactDFA} to {@link FastDFA} using a new input alphabet.
     * 
     * @param source Source {@link CompactDFA} to copy.
     * @param newInputs {@link Alphabet} to use for new automaton.
     * @return {@link FastDFA} with states identical to source.
     */
    public static <I> FastDFA<I> copy(CompactDFA<I> source, Alphabet<I> newInputs) {
        HashMap<Integer, FastDFAState> stateMapping = new HashMap<>();

        return copy(source, newInputs, stateMapping);
    }

    /**
     * Copy {@link CompactDFA} to {@link FastDFA} using a new input alphabet and returning a mapping of source states to
     * target states.
     * 
     * @param sourceSource {@link CompactDFA} to copy.
     * @param newInputs {@link Alphabet} to use for new automaton.
     * @param stateMapping {@link Map} to contain mapping from source to target states.
     * @return {@link FastDFA} with states identical to source.
     */
    private static <I> FastDFA<I> copy(CompactDFA<I> source, Alphabet<I> newInputs,
            Map<Integer, FastDFAState> stateMapping)
    {
        FastDFA<I> newDFA = new FastDFA<>(copyAlphabet(newInputs));
        for (Integer state: source.getStates()) {
            FastDFAState newState = newDFA.addState(source.isAccepting(state));
            stateMapping.put(state, newState);
        }

        newDFA.setInitialState(stateMapping.get(source.getInitialState()));

        for (Integer state: source.getStates()) {
            for (I input: newInputs) {
                if (!source.getInputAlphabet().contains(input)) {
                    throw new IllegalArgumentException(
                            "Input: " + input + " not in alphabet " + source.getInputAlphabet());
                }
                source.getSuccessor(state, input);
                FastDFAState target = stateMapping.get(source.getSuccessor(state, input));
                FastDFAState sourcestate = stateMapping.get(state);
                newDFA.addTransition(sourcestate, input, target);
            }
        }
        return newDFA;
    }

    /**
     * Convert a DFA to an NFA.
     * 
     * @param <I> The type of the input alphabet symbols.
     * @param dfa The DFA.
     * @return The NFA.
     */
    public static <I> CompactNFA<I> dfaToNfa(CompactDFA<I> dfa) {
        CompactNFA<I> nfa = new CompactNFA<>(copyAlphabet(dfa.getInputAlphabet()));
        AutomatonLowLevelCopy.copy(AutomatonCopyMethod.STATE_BY_STATE, dfa, dfa.getInputAlphabet(), nfa, b -> b,
                t -> null);
        return nfa;
    }

    /**
     * Convert a DFA to an NFA.
     * 
     * @param <I> The type of the input alphabet symbols.
     * @param dfa The DFA.
     * @return The NFA.
     */
    public static <I> FastNFA<I> dfaToNfa(FastDFA<I> dfa) {
        FastNFA<I> nfa = new FastNFA<>(copyAlphabet(dfa.getInputAlphabet()));
        AutomatonLowLevelCopy.copy(AutomatonCopyMethod.STATE_BY_STATE, dfa, dfa.getInputAlphabet(), nfa, b -> b,
                t -> null);
        return nfa;
    }

    /**
     * Counts the number of initial state arrows and transitions within the given automaton.
     * 
     * @param <S> The type of automaton states.
     * @param <T> The type of the input alphabet symbols.
     * @param <U> The type of automaton.
     * @param automaton The input automaton.
     * @return The number of initial state arrows and transitions in the specified automaton.
     */
    public static <S, T, U extends SimpleAutomaton<S, T> & StateLocalInput<S, T>> int countTransitions(U automaton) {
        int count = automaton.getInitialStates().size();

        for (S state: automaton.getStates()) {
            // This is to be absolutely sure that inputs are not counted more than once.
            Set<T> inputs = new HashSet<>(automaton.getLocalInputs(state));

            for (T input: inputs) {
                count += automaton.getSuccessors(state, input).size();
            }
        }

        return count;
    }

    /**
     * Minimize a {@link CompactDFA}.
     * 
     * @param <I> The type of automaton states.
     * @param dfa DFA to be minimized.
     * @return The minimized DFA.
     */
    public static <I> CompactDFA<I> minimizeDFA(CompactDFA<I> dfa) {
        // If there are no accepting states in the automaton, the automaton has an empty language, so we can return a
        // representation of that instead of doing explicit minimization.
        if (dfa.getStates().stream().noneMatch(s -> dfa.isAccepting(s))) {
            return createEmptyLanguageCompactDfa();
        }

        // Perform the actual minimization.
        final FastDFA<I> minimizedDFA = PaigeTarjanMinimization.minimizeDFA(dfa, dfa.getInputAlphabet(),
                new FastDfaCreator<I>());
        // Because AutomataLib is focused on complete automata, the minimized automaton can contain a sink state, as
        // described in https://github.com/LearnLib/automatalib/issues/48. To obtain a truly minimal partial automaton,
        // the sink state has to be removed to obtain a truly minimal partial DFA.

        // If there are no accepting states after minimization, the automaton has an empty language, but there may be
        // sink states and transitions remaining after minimization. Instead, we return a minimal representation of the
        // empty language.
        if (minimizedDFA.getStates().stream().noneMatch(s -> minimizedDFA.isAccepting(s))) {
            return createEmptyLanguageCompactDfa();
        }

        // If there are accepting states, there may still be a sink state in the automaton, which should be removed.
        List<FastDFAState> sinkStates = AutomataLibUtil.findSinks(minimizedDFA, minimizedDFA.getInputAlphabet());
        Preconditions.checkArgument(sinkStates.size() <= 1, "Minimization result contains too many sink states.");
        if (sinkStates.size() == 1) {
            // Because the initial state cannot be a sink state, the automaton must have at least two states. Therefore,
            // the sink state can be removed without the automaton becoming empty.
            minimizedDFA.removeState(sinkStates.get(0));
        }

        return copy(minimizedDFA, minimizedDFA.getInputAlphabet());
    }

    /**
     * Minimize a {@link FastDFA}.
     * 
     * @param <I> The type of automaton states.
     * @param dfa DFA to be minimized.
     * @return The minimized DFA.
     */
    public static <I> FastDFA<I> minimizeDFA(FastDFA<I> dfa) {
        // If there are no accepting states in the automaton, the automaton has an empty language, so we can return a
        // representation of that instead of doing explicit minimization.
        if (dfa.getStates().stream().noneMatch(s -> dfa.isAccepting(s))) {
            return createEmptyLanguageFastDfa();
        }

        // Perform the actual minimization.
        final FastDFA<I> minimizedDFA = PaigeTarjanMinimization.minimizeDFA(dfa, dfa.getInputAlphabet(),
                new FastDfaCreator<I>());
        // Because AutomataLib is focused on complete automata, the minimized automaton can contain a sink state, as
        // described in https://github.com/LearnLib/automatalib/issues/48. To obtain a truly minimal partial automaton,
        // the sink state has to be removed to obtain a truly minimal partial DFA.

        // If there are no accepting states after minimization, the automaton has an empty language, but there may be
        // sink states and transitions remaining after minimization. Instead, we return a minimal representation of the
        // empty language.
        if (minimizedDFA.getStates().stream().noneMatch(s -> minimizedDFA.isAccepting(s))) {
            return createEmptyLanguageFastDfa();
        }

        // If there are accepting states, there may still be a sink state in the automaton, which should be removed.
        List<FastDFAState> sinkStates = AutomataLibUtil.findSinks(minimizedDFA, minimizedDFA.getInputAlphabet());
        Preconditions.checkArgument(sinkStates.size() <= 1, "Minimization result contains too many sink states.");
        if (sinkStates.size() == 1) {
            // Because the initial state cannot be a sink state, the automaton must have at least two states. Therefore,
            // the sink state can be removed without the automaton becoming empty.
            minimizedDFA.removeState(sinkStates.get(0));
        }

        return minimizedDFA;
    }

    /**
     * Create a minimal weak-trace equivalent DFA, eliminating 'tau' transitions, where the symbol "tau" represents
     * 'tau'.
     * 
     * @param dfa The input DFA.
     * @return The output DFA.
     */
    public static CompactDFA<String> normalizeWeakTrace(CompactDFA<String> dfa) {
        return normalizeWeakTrace(dfa, "tau");
    }

    /**
     * Create a minimal weak-trace equivalent DFA, eliminating 'tau' transitions.
     * 
     * @param <T> The type of the input alphabet symbols.
     * @param dfa The input DFA.
     * @param tau The input symbol representing 'tau'.
     * @return The output DFA.
     */
    public static <T> CompactDFA<T> normalizeWeakTrace(CompactDFA<T> dfa, T tau) {
        if (dfa.getInputAlphabet().contains(tau)) {
            CompactNFA<T> nfaWithoutTau = removeTauTransitions(dfaToNfa(dfa), tau);
            dfa = NFAs.determinize(nfaWithoutTau, true, false);
        }

        return minimizeDFA(dfa);
    }

    /**
     * Constructs a weak-trace equivalent NFA, eliminating 'tau' transitions.
     * <p>
     * A standard algorithm is used for removing tau-transitions. See for example:
     * http://web.cecs.pdx.edu/~sheard/course/CS311/Fall2013/ppt/NfaEpsilonDefined.pdf, slide 8. The algorithm seems to
     * be taken from the book "Introduction to the Theory of Computation" by Michael Sipser (ISBN-13 978-0-534-95097-2).
     * One remark: in automata theory, epsilon is used instead of tau for denoting transitions that are empty.
     * </p>
     * <p>
     * This is the algorithm. From {@code nfa} construct and return a new NFA {@code nfaWithoutTau} such that:
     * <ul>
     * <li>{@code nfaWithoutTau} has the same set of states as {@code nfa}.</li>
     * <li>{@code nfaWithoutTau} has the same set of initial states as {@code nfa}.</li>
     * <li>Any state 's' in {@code nfaWithoutTau} is accepting iff there is a state in the tau-closure of 's' that is
     * accepting in {@code nfa}. (See {@link AutomataLibUtil#getTauClosure} for the definition of tau-closures.)</li>
     * <li>A transition 's1 --t--> s2' is defined in {@code nfaWithoutTau} iff there exists a transition 's3 --t--> s2'
     * in {@code nfa} with 's3' a state in the tau-closure of 's1' and 't' unequal to {@code tau}.</li>
     * </ul>
     * </p>
     * 
     * @param <T> The type of the input alphabet symbols.
     * @param nfa The input DFA.
     * @param tau The input symbol representing 'tau'.
     * @return The output NFA.
     */
    private static <T> CompactNFA<T> removeTauTransitions(CompactNFA<T> nfa, T tau) {
        // Initialize a new NFA. We will construct it to be language equivalent to 'nfa', modulo tau-transitions.
        Alphabet<T> alphabetWithoutTau = new GrowingMapAlphabet<>(
                nfa.getInputAlphabet().stream().filter(s -> !s.equals(tau)).collect(Collectors.toList()));
        CompactNFA<T> nfaWithoutTau = new CompactNFA<>(alphabetWithoutTau);

        // Obtain the tau-closure of every state in 'nfa'. This is needed for constructing 'nfaWithoutTau'.
        Map<Integer, Set<Integer>> tauClosures = getTauClosures(nfa, tau);

        // Define all states of 'nfaWithoutTau'. A new state is added for every state in 'nfa'.
        for (int state: nfa.getStates()) {
            int newState = nfaWithoutTau.addState();
            Preconditions.checkArgument(state == newState, "State construction out of sync");

            // The new state is initial if 'state' is initial.
            nfaWithoutTau.setInitial(newState, nfa.getInitialStates().contains(state));

            // The new state is accepting if the tau-closure of 'state' contains an accepting state.
            nfaWithoutTau.setAccepting(newState, tauClosures.get(state).stream().anyMatch(p -> nfa.isAccepting(p)));
        }

        // Define all transitions of 'nfaWithoutTau'.
        for (int state: nfa.getStates()) {
            for (int enclosedState: tauClosures.get(state)) {
                for (T symbol: nfa.getLocalInputs(enclosedState)) {
                    if (!symbol.equals(tau)) {
                        for (int succState: nfa.getTransitions(enclosedState, symbol)) {
                            nfaWithoutTau.addTransition(state, symbol, succState);
                        }
                    }
                }
            }
        }

        return nfaWithoutTau;
    }

    /**
     * Determines the tau-closure of every state in the given automaton.
     * 
     * @param <S> The type of states.
     * @param <T> The type of input symbols.
     * @param automaton The input automaton.
     * @param tau The input symbol that represents tau.
     * @return A mapping from all states of the input automaton to their tau-closures.
     */
    private static <S, T> Map<S, Set<S>> getTauClosures(SimpleAutomaton<S, T> automaton, T tau) {
        Map<S, Set<S>> closureMap = new HashMap<>(automaton.size());

        for (S state: automaton.getStates()) {
            closureMap.put(state, getTauClosure(automaton, state, tau));
        }

        return closureMap;
    }

    /**
     * Determines the set of states that is reachable by the given 'state' by taking zero or more 'tau' transitions.
     * 
     * @param <S> The type of states.
     * @param <T> The type of input symbols.
     * @param automaton The input automaton.
     * @param state The input state.
     * @param tau The input symbol that represents tau.
     * @return The tau-closure of the given input state.
     */
    private static <S, T> Set<S> getTauClosure(SimpleAutomaton<S, T> automaton, S state, T tau) {
        Set<S> visitedStates = new HashSet<>();
        Deque<S> queue = new LinkedList<>();
        queue.add(state);

        while (!queue.isEmpty()) {
            S currentState = queue.pop();
            visitedStates.add(currentState);

            for (S nextState: automaton.getSuccessors(currentState, tau)) {
                if (!visitedStates.contains(nextState)) {
                    queue.add(nextState);
                }
            }
        }

        return visitedStates;
    }

    /**
     * Retrieve all sink states from an automaton.
     * <p>
     * For the purpose of this method, a sink state is a non-initial, non-accepting state with no outgoing edges to
     * other states (so it may have self loops).
     * </p>
     * 
     * @param automaton Automaton containing zero or more sink states.
     * @param inputs Input alphabet of automaton.
     * @return Sink states retrieved from automaton.
     */
    private static <S, I, A extends AcceptorTS<S, I> & UniversalFiniteAlphabetAutomaton<S, I, S, Boolean, Void>> List<S>
            findSinks(A automaton, Alphabet<I> inputs)
    {
        List<S> sinkStates = new ArrayList<>();
        for (S state: automaton.getStates()) {
            if (automaton.isAccepting(state) || automaton.getInitialStates().contains(state)) {
                continue;
            }

            boolean hasSuccessor = false;

            for (I input: inputs) {
                Set<S> successors = new HashSet<>(automaton.getSuccessors(state, input));

                // Add self to simplify test
                successors.add(state);

                // If multiple successors, there must be an outgoing edge, so not a sink state.
                if (successors.size() > 1) {
                    hasSuccessor = true;
                    break;
                }
            }

            if (!hasSuccessor) {
                sinkStates.add(state);
            }
        }
        return sinkStates;
    }

    /**
     * Determine if the automaton contains states not reachable from any initial state.
     * 
     * @param automaton Automaton potentially containing unreachable states.
     * @return {code true} if the automaton contains unreachable states, {@code false} otherwise.
     */
    public static <S, I, A extends AcceptorTS<S, I> & UniversalFiniteAlphabetAutomaton<S, I, S, Boolean, Void>> boolean
            hasUnreachableStates(A automaton)
    {
        Set<S> reachableStates = new HashSet<>();
        for (S initialState: automaton.getInitialStates()) {
            reachableStates.addAll(getReachableStates(automaton, initialState));
        }
        return automaton.getStates().size() != reachableStates.size();
    }

    /**
     * Collect the states reachable from a given state using any sequence of inputs.
     * 
     * @param automaton Automaton containing potentially reachable states.
     * @param state State to start reachability computation.
     * @return {@link Set} of states reachable from {@link state}.
     */
    private static <S, I, A extends AcceptorTS<S, I> & UniversalFiniteAlphabetAutomaton<S, I, S, Boolean, Void>> Set<S>
            getReachableStates(A automaton, S state)
    {
        Set<S> visitedLocations = new HashSet<>();
        visitedLocations.add(state);
        Deque<S> locationQueue = new LinkedList<>();
        locationQueue.add(state);

        while (!locationQueue.isEmpty()) {
            S currentLoc = locationQueue.pop();

            for (I input: automaton.getInputAlphabet()) {
                for (S successor: automaton.getSuccessors(currentLoc, input)) {
                    if (!visitedLocations.contains(successor)) {
                        visitedLocations.add(successor);
                        locationQueue.add(successor);
                    }
                }
            }
        }

        return visitedLocations;
    }

    /**
     * Copy an automaton. Allows to use a different (e.g. bigger) alphabet for the new automaton.
     *
     * @param <IA> Input automaton class.
     * @param <OA> Output automaton class.
     * @param originalAutomaton The automaton to copy.
     * @param newAutomatonCreator Function to create the automaton, given the new alphabet.
     * @param originalAlphabet The alphabet of the original automaton.
     * @param newAlphabet The alphabet of the new automaton.
     * @return Newly created automaton.
     */
    public static <I, S1, T1, S2, T2, SP, TP, IA extends UniversalAutomaton<S1, I, T1, SP, TP>,
            OA extends MutableAutomaton<S2, I, T2, SP, TP>> OA copy(IA originalAutomaton,
                    Function<Alphabet<I>, OA> newAutomatonCreator, Alphabet<I> originalAlphabet,
                    Alphabet<I> newAlphabet)
    {
        // Create new automaton.
        OA newAutomaton = newAutomatonCreator.apply(copyAlphabet(newAlphabet));

        // State and transition properties are simply copied.
        Function<S1, SP> spMapping = s -> originalAutomaton.getStateProperty(s);
        Function<T1, TP> tpMapping = t -> originalAutomaton.getTransitionProperty(t);
        Function<I, I> inputMapping = i -> i;

        // Copy.
        AutomatonLowLevelCopy.rawCopy(AutomatonCopyMethod.STATE_BY_STATE, originalAutomaton, originalAlphabet,
                newAutomaton, inputMapping, spMapping, tpMapping);
        return newAutomaton;
    }

    // ----------
    // Difference / subtract / '\'
    // ----------

    /**
     * Compute the minimized difference of two potentially partial automata, i.e.
     * {@code firstAutomaton \ secondAutomaton}.
     * 
     * <p>
     * As a side effect, the alphabets of the automata are {@link #synchronizeAlphabets synchronized}.
     * </p>
     * 
     * @param firstAutomaton The first potentially partial automaton.
     * @param secondAutomaton The second potentially partial automaton.
     * @return The minimized difference automaton, i.e. {@code firstAutomaton \ secondAutomaton}.
     */
    public static <S, I, A extends DFA<S, I> & InputAlphabetHolder<I> & SupportsGrowingAlphabet<I>> CompactDFA<I>
            differenceMinimized(A firstAutomaton, A secondAutomaton)
    {
        synchronizeAlphabets(firstAutomaton, secondAutomaton);
        CompactDFA<I> combinedDFA = DFAs.and(firstAutomaton,
                DFAs.complement(secondAutomaton, copyAlphabet(secondAutomaton.getInputAlphabet())),
                copyAlphabet(firstAutomaton.getInputAlphabet()));
        return minimizeDFA(combinedDFA);
    }

    // ----------
    // Synchronize alphabets
    // ----------

    /**
     * Synchronize the alphabets of two automata. Adds the symbols from the alphabet of each automaton to the alphabet
     * of the other automaton, such that they each have an alphabet that is the union of the alphabet of both automata.
     * 
     * @param firstAutomaton The first automaton.
     * @param secondAutomaton The second automaton.
     */
    public static <S, I, T, SP, TP,
            A extends UniversalAutomaton<S, I, T, SP, TP> & InputAlphabetHolder<I> & SupportsGrowingAlphabet<I>> void
            synchronizeAlphabets(A firstAutomaton, A secondAutomaton)
    {
        for (I symbol: secondAutomaton.getInputAlphabet()) {
            firstAutomaton.addAlphabetSymbol(symbol);
        }
        for (I symbol: firstAutomaton.getInputAlphabet()) {
            secondAutomaton.addAlphabetSymbol(symbol);
        }
    }

    // ----------
    // Intersection / and
    // ----------

    /**
     * Compute the minimized intersection of two potentially partial automata.
     * 
     * @param firstAutomaton The first potentially partial automaton.
     * @param secondAutomaton The second potentially partial automaton.
     * @return The minimized intersection automaton.
     */
    public static <S, I, A extends DFA<S, I> & InputAlphabetHolder<I> & SupportsGrowingAlphabet<I>> CompactDFA<I>
            intersectionMinimized(A firstAutomaton, A secondAutomaton)
    {
        CompactDFA<I> combinedDFA = combine(firstAutomaton, secondAutomaton, AcceptanceCombiner.AND);
        return minimizeDFA(combinedDFA);
    }

    // ----------
    // Union / or
    // ----------

    /**
     * Compute the minimized union of two potentially partial automata.
     * 
     * @param firstAutomaton The first potentially partial automaton.
     * @param secondAutomaton The second potentially partial automaton.
     * @return The minimized union automaton.
     */
    public static <S, I, A extends DFA<S, I> & InputAlphabetHolder<I> & SupportsGrowingAlphabet<I>> CompactDFA<I>
            unionMinimized(A firstAutomaton, A secondAutomaton)
    {
        CompactDFA<I> combinedDFA = combine(firstAutomaton, secondAutomaton, AcceptanceCombiner.OR);
        return minimizeDFA(combinedDFA);
    }

    // ----------
    // Exclusive or / xor
    // ----------

    /**
     * Compute the minimized xor (exclusive or) of two potentially partial automata.
     * 
     * @param firstAutomaton The first potentially partial automaton.
     * @param secondAutomaton The second potentially partial automaton.
     * @return The minimized xor automaton.
     */
    public static <S, I, A extends DFA<S, I> & InputAlphabetHolder<I> & SupportsGrowingAlphabet<I>> CompactDFA<I>
            xorMinimized(A firstAutomaton, A secondAutomaton)
    {
        CompactDFA<I> combinedDFA = combine(firstAutomaton, secondAutomaton, AcceptanceCombiner.XOR);
        return minimizeDFA(combinedDFA);
    }

    // ----------
    // Parallel composition / '||'
    // ----------

    /**
     * Compute the minimized parallel composition of two potentially partial automata.
     * 
     * @param firstAutomaton The first potentially partial automaton.
     * @param secondAutomaton The second potentially partial automaton.
     * @return The minimized parallel composition automaton.
     */
    public static <S, I, A extends MutableDFA<S, I> & InputAlphabetHolder<I> & SupportsGrowingAlphabet<I>> CompactDFA<I>
            parallelCompositionMinimized(A firstAutomaton, A secondAutomaton)
    {
        CompactDFA<I> combinedDFA = combineParallel(firstAutomaton, secondAutomaton);
        return minimizeDFA(combinedDFA);
    }

    /**
     * Combine two DFAs containing input alphabets. The {@link AcceptanceCombiner} specified via the {@code combiner}
     * parameter specifies how acceptance values of the DFAs will be combined to an acceptance value in the result DFA.
     *
     * @param dfa1 The first DFA.
     * @param dfa2 The second DFA.
     * @param combiner Combination method for acceptance values.
     * @return The combined automaton.
     */
    private static <I, S, A extends DFA<S, I> & InputAlphabetHolder<I>> CompactDFA<I> combine(A dfa1, A dfa2,
            AcceptanceCombiner combiner)
    {
        DeterministicAcceptorTS<?, I> acc = new DetAcceptorCompositionWithInput<>(dfa1, dfa2, combiner);

        return copyAcceptorToDFA(acc, combineAlphabets(dfa1.getInputAlphabet(), dfa2.getInputAlphabet()));
    }

    /**
     * Combine two DFAs containing input alphabets. The {@link AcceptanceCombiner} specified via the {@code combiner}
     * parameter specifies how acceptance values of the DFAs will be combined to an acceptance value in the result DFA.
     *
     * @param dfa1 The first DFA.
     * @param dfa2 The second DFA.
     * @return The parallel composition automaton.
     */
    private static <I, S, A extends DFA<S, I> & InputAlphabetHolder<I>> CompactDFA<I> combineParallel(A dfa1, A dfa2) {
        DeterministicAcceptorTS<?, I> acc = new DetAcceptorCompositionParallel<>(dfa1, dfa2);

        return copyAcceptorToDFA(acc, combineAlphabets(dfa1.getInputAlphabet(), dfa2.getInputAlphabet()));
    }

    /**
     * Copy an acceptor to a DFA.
     * 
     * @param acc Acceptor to copy.
     * @param inputs The input symbols to consider.
     * @return The DFA describing the same language as the acceptor.
     */
    private static <I> CompactDFA<I> copyAcceptorToDFA(DeterministicAcceptorTS<?, I> acc, Alphabet<I> inputs) {
        CompactDFA<I> out = new CompactDFA<>(inputs);
        TSCopy.copy(TSTraversalMethod.DEPTH_FIRST, acc, TSTraversal.NO_LIMIT, inputs, out);
        return out;
    }

    /**
     * Combine two alphabets into a single alphabet.
     * 
     * @param a1 First alphabet.
     * @param a2 Second alphabet.
     * @return The alphabet containing all symbols from the first and second alphabets.
     */
    private static <I> Alphabet<I> combineAlphabets(Alphabet<I> a1, Alphabet<I> a2) {
        Alphabet<I> combinedAlphabet = copyAlphabet(a1);
        combinedAlphabet.addAll(a2);

        return combinedAlphabet;
    }

    // ----------
    // Prefix closure
    // ----------

    /**
     * Modifies the given DFA by applying a prefix closure, to allow all prefixes of accepted words to be accepted as
     * well.
     *
     * @param dfa The DFA. Is modified in-place.
     */
    public static void prefixClose(CompactDFA<String> dfa) {
        Multimap<Integer, Integer> statePredecessors = getStatePredecessorsMap(dfa);

        Deque<Integer> stateQueue = new LinkedList<>();
        Set<Integer> visitedStates = new HashSet<>();

        stateQueue.addAll(dfa.getStates().stream().filter(s -> dfa.isAccepting(s)).collect(Collectors.toList()));

        while (!stateQueue.isEmpty()) {
            Integer state = stateQueue.pollFirst();
            visitedStates.add(state);

            dfa.setAccepting(state, true);

            for (Integer predecessor: statePredecessors.get(state)) {
                if (!visitedStates.contains(predecessor)) {
                    stateQueue.add(predecessor);
                }
            }
        }
    }

    /**
     * Give a mapping from states to their predecessors.
     * 
     * @param dfa The DFA.
     * @return The mapping.
     */
    private static Multimap<Integer, Integer> getStatePredecessorsMap(CompactDFA<String> dfa) {
        Multimap<Integer, Integer> result = HashMultimap.create();

        for (Integer state: dfa.getStates()) {
            for (String input: dfa.getLocalInputs(state)) {
                result.put(dfa.getSuccessor(state, input), state);
            }
        }

        return result;
    }

    /**
     * Merges state1 and state2 in the automaton. Automaton is modified in place. State2 is removed, and all incoming
     * and outgoing transitions are added to state1. Duplicate transitions can arise depending on the implementation of
     * generic A.
     * 
     * @param automaton automaton in which states are merged. Method assumes state1 and state2 are part of automaton.
     * @param state1 the state which is preserved.
     * @param state2 the state which is removed. All transitions are added to state1.
     * @param checkConsistency if true, the merging algorithm will check for transitions from state1 and state2 for the
     *     same symbol to be equal according to {@link T#equals}. If set to false, the callee is responsible to ensure
     *     that e.g. both target states are in the same equivalence partition, or will be merged subsequently.
     * 
     */
    @SuppressWarnings("unchecked")
    public static <I, S, T, SP, TP, A extends ShrinkableAutomaton<S, I, T, SP, TP> & InputAlphabetHolder<I>> void
            merge(A automaton, S state1, S state2, boolean checkConsistency)
    {
        if (state1.equals(state2)) {
            return;
        }
        // Only for deterministic automata, we need to ensure that we do not have
        // conflicting transitions being merged.
        if (automaton instanceof DeterministicAutomaton<?, ?, ?>) {
            for (I input: automaton.getInputAlphabet()) {
                DeterministicTransitionSystem<S, I, T> dtsAutomaton = (DeterministicTransitionSystem<S, I, T>)automaton;
                // We assume state1 is leading
                if (dtsAutomaton.getTransition(state1, input) == null) {
                    automaton.addTransition(state1, input, dtsAutomaton.getTransition(state2, input));
                } else {
                    // if checkConsistency, we check whether state2 is in accordance with state1.
                    // otherwise, we take the target of state1.
                    if (checkConsistency) {
                        if (dtsAutomaton.getTransition(state2, input) != null && !dtsAutomaton
                                .getTransition(state1, input).equals(dtsAutomaton.getTransition(state2, input)))
                        {
                            throw new RuntimeException("Cannot merge states because transitions are not consistent for "
                                    + input.toString());
                        }
                    }
                }
            }
        } else {
            for (I input: automaton.getInputAlphabet()) {
                automaton.addTransitions(state1, input, automaton.getTransitions(state2, input));
            }
        }
        // Merging incoming transitions is done by removeState (rather, replaceState)
        automaton.removeState(state2, state1);
    }

    /**
     * Returns a {@link CompactDFA} that accepts the empty language.
     * 
     * @param <I> The type of input symbols.
     * @return The newly created DFA.
     */
    public static <I> CompactDFA<I> createEmptyLanguageCompactDfa() {
        Alphabet<I> alphabet = new GrowingMapAlphabet<>();
        CompactDFA<I> dfa = new CompactDFA<>(alphabet);
        dfa.addInitialState(false);
        return dfa;
    }

    /**
     * Returns a {@link FastDFA} that accepts the empty language.
     * 
     * @param <I> The type of input symbols.
     * @return The newly created DFA.
     */
    public static <I> FastDFA<I> createEmptyLanguageFastDfa() {
        Alphabet<I> alphabet = new GrowingMapAlphabet<>();
        FastDFA<I> dfa = new FastDFA<>(alphabet);
        dfa.addInitialState(false);
        return dfa;
    }

    /**
     * Returns a {@link CompactNFA} that accepts the empty language.
     * 
     * @param <I> The type of input symbols.
     * @return The newly created NFA.
     */
    public static <I> CompactNFA<I> createEmptyLanguageCompactNfa() {
        Alphabet<I> alphabet = new GrowingMapAlphabet<>();
        CompactNFA<I> nfa = new CompactNFA<>(alphabet);
        nfa.addInitialState(false);
        return nfa;
    }

    /**
     * Returns a {@link FastNFA} that accepts the empty language.
     * 
     * @param <I> The type of input symbols.
     * @return The newly created NFA.
     */
    public static <I> FastNFA<I> createEmptyLanguageFastNfa() {
        Alphabet<I> alphabet = new GrowingMapAlphabet<>();
        FastNFA<I> nfa = new FastNFA<>(alphabet);
        nfa.addInitialState(false);
        return nfa;
    }
}
