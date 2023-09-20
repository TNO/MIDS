/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2023 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////

package nl.tno.mids.compare.compute;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.github.tno.gltsdiff.glts.State;
import com.github.tno.gltsdiff.glts.Transition;
import com.github.tno.gltsdiff.glts.lts.BaseLTS;
import com.github.tno.gltsdiff.glts.lts.LTS;
import com.github.tno.gltsdiff.glts.lts.LTSStateProperty;
import com.github.tno.gltsdiff.glts.lts.automaton.Automaton;
import com.github.tno.gltsdiff.glts.lts.automaton.AutomatonStateProperty;
import com.github.tno.gltsdiff.glts.lts.automaton.BaseAutomaton;
import com.github.tno.gltsdiff.glts.lts.automaton.diff.DiffAutomaton;
import com.github.tno.gltsdiff.glts.lts.automaton.diff.DiffAutomatonStateProperty;
import com.github.tno.gltsdiff.glts.lts.automaton.diff.DiffKind;
import com.github.tno.gltsdiff.glts.lts.automaton.diff.DiffProperty;
import com.github.tno.gltsdiff.operators.projectors.Projector;
import com.google.common.base.Preconditions;

import net.automatalib.automata.concepts.StateLocalInput;
import net.automatalib.automata.fsa.NFA;
import net.automatalib.automata.fsa.impl.compact.CompactDFA;
import net.automatalib.automata.fsa.impl.compact.CompactNFA;
import net.automatalib.util.automata.fsa.NFAs;
import net.automatalib.words.impl.GrowingMapAlphabet;
import nl.tno.mids.automatalib.extensions.util.AutomataLibUtil;
import nl.tno.mids.gltsdiff.extensions.DiffAutomatonUtils;

/**
 * Functionality for converting AutomataLib automata to structural comparison LTSs and vice versa, and for performing
 * language equivalence checks.
 */
public class StructuralComparisonUtils {
    private StructuralComparisonUtils() {
    }

    /**
     * Converts an AutomataLib {@link NFA} to an {@link LTS} as used by structural comparison.
     * 
     * @param <S> The source type of NFA states.
     * @param <T> The source type of NFA input symbols.
     * @param <N> The source type of AutomataLib NFAs.
     * @param <U> The target type of LTS state properties.
     * @param <V> The target type of LTS transition properties.
     * @param <L> The target type of LTSs.
     * @param nfa The non-{@code null} AutomataLib NFA to convert.
     * @param instantiator A supplier that instantiates new LTSs.
     * @param symbolMapper A function for mapping NFA input symbols to LTS transition properties. Any input symbol that
     *     is mapped to {@code null} will not be included in the converted LTS.
     * @param stateMapper A function for mapping NFA states to their LTS state properties. Any state that is mapped to
     *     {@code null} will not be included in the converted LTS.
     * @return A non-{@code null} LTS representation of {@code nfa}.
     */
    public static <S, T, N extends NFA<S, T> & StateLocalInput<S, T>, U extends LTSStateProperty, V,
            L extends BaseLTS<U, V>> L
            convertToLTS(N nfa, Supplier<L> instantiator, Function<T, V> symbolMapper, Function<S, U> stateMapper)
    {
        Preconditions.checkNotNull(nfa, "Expected a non-null automaton.");

        // Instantiate a fresh LTS.
        L lts = instantiator.get();

        // Convert all states.
        Map<S, State<U>> stateMapping = new HashMap<>(nfa.size());

        for (S state: nfa.getStates()) {
            U mappedStateProperty = stateMapper.apply(state);

            if (mappedStateProperty != null) {
                State<U> mappedState = lts.addState(mappedStateProperty);
                stateMapping.put(state, mappedState);
            }
        }

        // Convert all transitions.
        for (S source: nfa.getStates()) {
            for (T property: nfa.getLocalInputs(source)) {
                for (S target: nfa.getTransitions(source, property)) {
                    State<U> mappedSource = stateMapping.get(source);
                    V mappedProperty = symbolMapper.apply(property);
                    State<U> mappedTarget = stateMapping.get(target);

                    if (mappedSource != null && mappedProperty != null && mappedTarget != null) {
                        lts.tryAddTransition(mappedSource, mappedProperty, mappedTarget);
                    }
                }
            }
        }

        return lts;
    }

    /**
     * Converts a structural comparison {@link Automaton} to an AutomataLib {@link NFA}.
     * 
     * @param <S> The source type of state properties.
     * @param <T> The source type of transition properties.
     * @param <U> The target type of NFA input symbols.
     * @param automaton The non-{@code null} structural comparison automaton to convert.
     * @param transitionPropertyMapper A function for mapping transition properties to NFA input symbols. Any transition
     *     property that is mapped to {@code null} will not be included in the converted NFA.
     * @return A non-{@code null} NFA representation of {@code automaton}.
     */
    public static <S extends AutomatonStateProperty, T, U> CompactNFA<U> convertToNFA(BaseAutomaton<S, T> automaton,
            Function<T, U> transitionPropertyMapper)
    {
        Preconditions.checkNotNull(automaton, "Expected a non-null automaton.");

        // Instantiate a fresh NFA.
        Set<U> symbols = automaton.getStates().stream()
                .flatMap(state -> automaton.getOutgoingTransitionProperties(state).stream())
                .map(transitionPropertyMapper).filter(symbol -> symbol != null).collect(Collectors.toSet());
        CompactNFA<U> nfa = new CompactNFA<>(new GrowingMapAlphabet<>(symbols));

        // Convert all states.
        Map<State<S>, Integer> stateMapping = new HashMap<>(automaton.size());

        for (State<S> state: automaton.getStates()) {
            int newState = nfa.addState(state.getProperty().isAccepting());
            stateMapping.put(state, newState);
        }

        // Mark all initial states.
        for (State<S> initState: automaton.getInitialStates()) {
            nfa.setInitial(stateMapping.get(initState), true);
        }

        // Convert all transitions.
        for (State<S> state: automaton.getStates()) {
            for (Transition<S, T> transition: automaton.getOutgoingTransitions(state)) {
                U mappedSymbol = transitionPropertyMapper.apply(transition.getProperty());

                if (mappedSymbol != null) {
                    int mappedSource = stateMapping.get(transition.getSource());
                    int mappedTarget = stateMapping.get(transition.getTarget());
                    nfa.addTransition(mappedSource, mappedSymbol, mappedTarget);
                }
            }
        }

        return nfa;
    }

    /**
     * Converts a structural comparison {@link Automaton} to an AutomataLib {@link NFA}, thereby reusing all transition
     * properties as input symbols.
     * 
     * @param <S> The type of state properties.
     * @param <T> The type of transition properties and input symbols.
     * @param automaton The non-{@code null} structural comparison automaton to convert.
     * @return A non-{@code null} NFA representation of {@code automaton}.
     */
    public static <S extends AutomatonStateProperty, T> CompactNFA<T> convertToNFA(BaseAutomaton<S, T> automaton) {
        return convertToNFA(automaton, property -> property);
    }

    /**
     * Converts an AutomataLib {@link NFA} to a structural comparison {@link Automaton}.
     * 
     * @param <S> The source type of NFA states.
     * @param <T> The source type of NFA input symbols.
     * @param <U> The target type of transition properties.
     * @param <N> The type of AutomataLib NFAs.
     * @param nfa The non-{@code null} AutomataLib NFA to convert.
     * @param symbolMapper A function for mapping NFA input symbols to transition properties. Any input symbol that is
     *     mapped to {@code null} will not be included in the converted automaton.
     * @return A non-{@code null} automaton representation of {@code nfa}.
     */
    public static <S, T, U, N extends NFA<S, T> & StateLocalInput<S, T>> Automaton<U> convertToAutomaton(N nfa,
            Function<T, U> symbolMapper)
    {
        return convertToLTS(nfa, Automaton::new, symbolMapper,
                state -> new AutomatonStateProperty(nfa.getInitialStates().contains(state), nfa.isAccepting(state)));
    }

    /**
     * Converts an AutomataLib {@link NFA} to a structural comparison {@link Automaton}, thereby reusing all input
     * symbols as transition properties.
     * 
     * @param <S> The source type of NFA states.
     * @param <T> The type of transition properties and input symbols.
     * @param <N> The type of AutomataLib NFAs.
     * @param nfa The non-{@code null} AutomataLib NFA to convert.
     * @return A non-{@code null} {@link Automaton} representation of {@code nfa}.
     */
    public static <S, T, N extends NFA<S, T> & StateLocalInput<S, T>> Automaton<T> convertToAutomaton(N nfa) {
        return convertToAutomaton(nfa, property -> property);
    }

    /**
     * Converts an AutomataLib {@link NFA} to a structural comparison {@link DiffAutomaton}.
     * 
     * @param <S> The source type of NFA states.
     * @param <T> The source type of NFA input symbols.
     * @param <U> The target type of transition properties.
     * @param <N> The type of AutomataLib NFAs.
     * @param nfa The non-{@code null} AutomataLib NFA to convert.
     * @param diffKind The difference kind to be associated to every converted state, initial state arrow and
     *     transition.
     * @param symbolMapper A function for mapping NFA input symbols to transition properties. Any input symbol that is
     *     mapped to {@code null} will not be included in the converted automaton.
     * @return A non-{@code null} difference automaton representation of {@code nfa}.
     */
    public static <S, T, U, N extends NFA<S, T> & StateLocalInput<S, T>> DiffAutomaton<U> convertToDiffAutomaton(N nfa,
            DiffKind diffKind, Function<T, U> symbolMapper)
    {
        Function<T, DiffProperty<U>> diffPropertyMapper = symbol -> {
            U property = symbolMapper.apply(symbol);
            return property == null ? null : new DiffProperty<>(property, diffKind);
        };

        return convertToLTS(nfa, DiffAutomaton::new, diffPropertyMapper,
                state -> new DiffAutomatonStateProperty(nfa.isAccepting(state), diffKind,
                        nfa.getInitialStates().contains(state) ? Optional.of(diffKind) : Optional.empty()));
    }

    /**
     * Determines whether {@code left} and {@code right} are language equivalent.
     * 
     * @param <T> The type of input symbols.
     * @param left The non-{@code null} left NFA.
     * @param right The non-{@code null} right NFA.
     * @return {@code true} if {@code left} and {@code right} are language equivalent, {@code false} otherwise.
     */
    public static <T> boolean areLanguageEquivalent(CompactNFA<T> left, CompactNFA<T> right) {
        return ComputeVariants.areLanguageEquivalent(NFAs.determinize(left, true, false),
                NFAs.determinize(right, true, false));
    }

    /**
     * Determines whether {@code left} and {@code right} are language equivalent.
     * 
     * @param <S> The type of state properties.
     * @param <T> The type of transition properties.
     * @param left The non-{@code null} left automaton.
     * @param right The non-{@code null} right automaton.
     * @return {@code true} if {@code left} and {@code right} are language equivalent, {@code false} otherwise.
     */
    public static <S extends AutomatonStateProperty, T> boolean areLanguageEquivalent(BaseAutomaton<S, T> left,
            BaseAutomaton<S, T> right)
    {
        return areLanguageEquivalent(convertToNFA(left), convertToNFA(right));
    }

    /**
     * Determines whether two given {@link CompactDFA}s are weak language equivalent, that is, language equivalent
     * modulo tau.
     * 
     * @param <T> The type of input symbols.
     * @param left The non-{@code null} left DFA.
     * @param right The non-{@code null} right DFA.
     * @param tau The non-{@code null} symbol that represents tau.
     * @return {@code true} if {@code left} and {@code right} are weak language equivalent, {@code false} otherwise.
     */
    public static <T> boolean areWeakLanguageEquivalent(CompactDFA<T> left, CompactDFA<T> right, T tau) {
        return ComputeVariants.areLanguageEquivalent(AutomataLibUtil.normalizeWeakTrace(left, tau),
                AutomataLibUtil.normalizeWeakTrace(right, tau));
    }

    /**
     * Determines whether two given {@link CompactNFA}s are weak language equivalent, that is, language equivalent
     * modulo tau.
     * 
     * @param <T> The type of input symbols.
     * @param left The non-{@code null} left NFA.
     * @param right The non-{@code null} right NFA.
     * @param tau The non-{@code null} symbol that represents tau.
     * @return {@code true} if {@code left} and {@code right} are weak language equivalent, {@code false} otherwise.
     */
    public static <T> boolean areWeakLanguageEquivalent(CompactNFA<T> left, CompactNFA<T> right, T tau) {
        return areWeakLanguageEquivalent(NFAs.determinize(left, true, false), NFAs.determinize(right, true, false),
                tau);
    }

    /**
     * Determines whether two given structural comparison automata are weak language equivalent, that is, language
     * equivalent modulo tau.
     * 
     * @param <S> The type of state properties.
     * @param <T> The type of transition properties.
     * @param left The non-{@code null} left automaton.
     * @param right The non-{@code null} right automaton.
     * @param tau The non-{@code null} symbol that represents tau.
     * @return {@code true} if {@code left} and {@code right} are weak language equivalent, {@code false} otherwise.
     */
    public static <S extends AutomatonStateProperty, T> boolean areWeakLanguageEquivalent(BaseAutomaton<S, T> left,
            BaseAutomaton<S, T> right, T tau)
    {
        return areWeakLanguageEquivalent(convertToNFA(left), convertToNFA(right), tau);
    }

    /**
     * Determines whether (the projections of) two given structural comparison difference automata are weak language
     * equivalent, that is, language equivalent modulo tau.
     * 
     * @param <T> The type of transition properties.
     * @param left The non-{@code null} left difference automaton.
     * @param right The non-{@code null} right difference automaton.
     * @param projector The non-{@code null} projector for projecting transition properties.
     * @param tau The non-{@code null} symbol that represents tau.
     * @return {@code true} if {@code left} and {@code right} are weak language equivalent, {@code false} otherwise.
     */
    public static <T> boolean areWeakLanguageEquivalent(DiffAutomaton<T> left, DiffAutomaton<T> right,
            Projector<T, DiffKind> projector, T tau)
    {
        Automaton<T> leftLeft = DiffAutomatonUtils.toAutomaton(DiffAutomatonUtils.projectLeft(left, projector),
                property -> property);
        Automaton<T> rightLeft = DiffAutomatonUtils.toAutomaton(DiffAutomatonUtils.projectLeft(right, projector),
                property -> property);
        Automaton<T> leftRight = DiffAutomatonUtils.toAutomaton(DiffAutomatonUtils.projectRight(left, projector),
                property -> property);
        Automaton<T> rightRight = DiffAutomatonUtils.toAutomaton(DiffAutomatonUtils.projectRight(right, projector),
                property -> property);
        return areWeakLanguageEquivalent(leftLeft, rightLeft, tau)
                && areWeakLanguageEquivalent(leftRight, rightRight, tau);
    }
}
