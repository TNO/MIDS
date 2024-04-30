/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2024 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////

package nl.tno.mids.automatalib.extensions.cif;

import static org.eclipse.escet.common.java.Lists.list;
import static org.eclipse.escet.common.java.Maps.mapc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.escet.cif.common.CifCollectUtils;
import org.eclipse.escet.cif.common.CifEdgeUtils;
import org.eclipse.escet.cif.common.CifEventUtils;
import org.eclipse.escet.cif.common.CifTextUtils;
import org.eclipse.escet.cif.common.CifValueUtils;
import org.eclipse.escet.cif.metamodel.cif.Specification;
import org.eclipse.escet.cif.metamodel.cif.automata.Automaton;
import org.eclipse.escet.cif.metamodel.cif.automata.Edge;
import org.eclipse.escet.cif.metamodel.cif.automata.EdgeEvent;
import org.eclipse.escet.cif.metamodel.cif.automata.Location;
import org.eclipse.escet.cif.metamodel.cif.declarations.Event;
import org.eclipse.escet.cif.metamodel.cif.expressions.EventExpression;
import org.eclipse.escet.cif.metamodel.cif.expressions.Expression;
import org.eclipse.escet.cif.metamodel.cif.expressions.TauExpression;

import com.google.common.base.Preconditions;

import net.automatalib.SupportsGrowingAlphabet;
import net.automatalib.automata.MutableAutomaton;
import net.automatalib.automata.fsa.DFA;
import net.automatalib.automata.fsa.NFA;
import net.automatalib.automata.fsa.impl.FastDFA;
import net.automatalib.automata.fsa.impl.FastNFA;
import net.automatalib.automata.fsa.impl.compact.CompactDFA;
import net.automatalib.automata.fsa.impl.compact.CompactNFA;
import net.automatalib.words.impl.GrowingMapAlphabet;

/**
 * Utilities to convert CIF automata to AutomataLib automata.
 */
public class CifToAutomataLib {
    /**
     * Convert a CIF automaton to a {@link CompactDFA}.
     * 
     * <p>
     * For details on preconditions and the conversion itself, see {@link #cifAutomatonToFsa}.
     * </p>
     *
     * @param cifAutomaton CIF automaton to convert.
     * @param allStatesAccepting Whether to create all DFA states as accepting states ({@code true}), or to use marking
     *     information from the CIF model to determine which states in the DFA should be accepting states
     *     ({@code false}).
     * @throws ConversionPreconditionException If a precondition is not satisfied.
     * @throws IllegalStateException If a non-deterministic CIF automaton is converted.
     * @throws IllegalStateException If a CIF automaton with multiple initial states is converted.
     * @return The DFA.
     */
    public static CompactDFA<String> cifAutomatonToCompactDfa(Automaton cifAutomaton, boolean allStatesAccepting) {
        CompactDFA<String> dfa = new CompactDFA<>(new GrowingMapAlphabet<>());
        return cifAutomatonToFsa(cifAutomaton, dfa, allStatesAccepting);
    }

    /**
     * Convert a CIF automaton to a {@link CompactNFA}.
     * 
     * <p>
     * For details on preconditions and the conversion itself, see {@link #cifAutomatonToFsa}.
     * </p>
     *
     * @param cifAutomaton CIF automaton to convert.
     * @param allStatesAccepting Whether to create all NFA states as accepting states ({@code true}), or to use marking
     *     information from the CIF model to determine which states in the NFA should be accepting states
     *     ({@code false}).
     * @throws ConversionPreconditionException If a precondition is not satisfied.
     * @throws IllegalStateException If a CIF automaton with multiple initial states is converted.
     * @return The NFA.
     */
    public static CompactNFA<String> cifAutomatonToCompactNfa(Automaton cifAutomaton, boolean allStatesAccepting) {
        CompactNFA<String> nfa = new CompactNFA<>(new GrowingMapAlphabet<>());
        return cifAutomatonToFsa(cifAutomaton, nfa, allStatesAccepting);
    }

    /**
     * Convert a CIF automaton to a {@link FastDFA}.
     * 
     * <p>
     * For details on preconditions and the conversion itself, see {@link #cifAutomatonToFsa}.
     * </p>
     *
     * @param cifAutomaton CIF automaton to convert.
     * @param allStatesAccepting Whether to create all DFA states as accepting states ({@code true}), or to use marking
     *     information from the CIF model to determine which states in the DFA should be accepting states
     *     ({@code false}).
     * @throws ConversionPreconditionException If a precondition is not satisfied.
     * @throws IllegalStateException If a non-deterministic CIF automaton is converted.
     * @throws IllegalStateException If a CIF automaton with multiple initial states is converted.
     * @return The DFA.
     */
    public static FastDFA<String> cifAutomatonToFastDfa(Automaton cifAutomaton, boolean allStatesAccepting) {
        FastDFA<String> dfa = new FastDFA<>(new GrowingMapAlphabet<>());
        return cifAutomatonToFsa(cifAutomaton, dfa, allStatesAccepting);
    }

    /**
     * Convert a CIF automaton to a {@link FastNFA}.
     * 
     * <p>
     * For details on preconditions and the conversion itself, see {@link #cifAutomatonToFsa}.
     * </p>
     *
     * @param cifAutomaton CIF automaton to convert.
     * @param allStatesAccepting Whether to create all NFA states as accepting states ({@code true}), or to use marking
     *     information from the CIF model to determine which states in the NFA should be accepting states
     *     ({@code false}).
     * @throws ConversionPreconditionException If a precondition is not satisfied.
     * @throws IllegalStateException If a CIF automaton with multiple initial states is converted.
     * @return The NFA.
     */
    public static FastNFA<String> cifAutomatonToFastNfa(Automaton cifAutomaton, boolean allStatesAccepting) {
        FastNFA<String> nfa = new FastNFA<>(new GrowingMapAlphabet<>());
        return cifAutomatonToFsa(cifAutomaton, nfa, allStatesAccepting);
    }

    /**
     * Convert the automata of a CIF specification to {@link CompactDFA}s.
     * 
     * <p>
     * For details on preconditions and the conversion itself, see {@link #cifAutomatonToFsa}.
     * </p>
     *
     * @param specification CIF specification containing the CIF automata to convert.
     * @param allStatesAccepting Whether to create all DFA states as accepting states ({@code true}), or to use marking
     *     information from the CIF model to determine which states in the DFA should be accepting states
     *     ({@code false}).
     * @throws ConversionPreconditionException If a precondition is not satisfied.
     * @throws IllegalStateException If a non-deterministic CIF automaton is converted.
     * @throws IllegalStateException If a CIF automaton with multiple initial states is converted.
     * @return Mapping from absolute names of CIF automata to DFAs.
     */
    public static Map<String, CompactDFA<String>> cifSpecificationToCompactDfas(Specification specification,
            boolean allStatesAccepting)
    {
        List<Automaton> automata = list();
        CifCollectUtils.collectAutomata(specification, automata);
        Map<String, CompactDFA<String>> dfas = mapc(automata.size());
        for (Automaton aut: automata) {
            CompactDFA<String> dfa = new CompactDFA<>(new GrowingMapAlphabet<>());
            cifAutomatonToFsa(aut, dfa, allStatesAccepting);
            dfas.put(CifTextUtils.getAbsName(aut, false), dfa);
        }
        return dfas;
    }

    /**
     * Convert the automata of a CIF specification to {@link CompactNFA}s.
     * 
     * <p>
     * For details on preconditions and the conversion itself, see {@link #cifAutomatonToFsa}.
     * </p>
     *
     * @param specification CIF specification containing the CIF automata to convert.
     * @param allStatesAccepting Whether to create all NFA states as accepting states ({@code true}), or to use marking
     *     information from the CIF model to determine which states in the NFA should be accepting states
     *     ({@code false}).
     * @throws ConversionPreconditionException If a precondition is not satisfied.
     * @throws IllegalStateException If a CIF automaton with multiple initial states is converted.
     * @return Mapping from absolute names of CIF automata to NFAs.
     */
    public static Map<String, CompactNFA<String>> cifSpecificationToCompactNfas(Specification specification,
            boolean allStatesAccepting)
    {
        List<Automaton> automata = list();
        CifCollectUtils.collectAutomata(specification, automata);
        Map<String, CompactNFA<String>> nfas = mapc(automata.size());
        for (Automaton aut: automata) {
            CompactNFA<String> nfa = new CompactNFA<>(new GrowingMapAlphabet<>());
            cifAutomatonToFsa(aut, nfa, allStatesAccepting);
            nfas.put(CifTextUtils.getAbsName(aut, false), nfa);
        }
        return nfas;
    }

    /**
     * Convert the automata of a CIF specification to {@link FastDFA}s.
     * 
     * <p>
     * For details on preconditions and the conversion itself, see {@link #cifAutomatonToFsa}.
     * </p>
     *
     * @param specification CIF specification containing the CIF automata to convert.
     * @param allStatesAccepting Whether to create all DFA states as accepting states ({@code true}), or to use marking
     *     information from the CIF model to determine which states in the DFA should be accepting states
     *     ({@code false}).
     * @throws ConversionPreconditionException If a precondition is not satisfied.
     * @throws IllegalStateException If a non-deterministic CIF automaton is converted.
     * @throws IllegalStateException If a CIF automaton with multiple initial states is converted.
     * @return Mapping from absolute names of CIF automata to DFAs.
     */
    public static Map<String, FastDFA<String>> cifSpecificationToFastDfas(Specification specification,
            boolean allStatesAccepting)
    {
        List<Automaton> automata = list();
        CifCollectUtils.collectAutomata(specification, automata);
        Map<String, FastDFA<String>> dfas = mapc(automata.size());
        for (Automaton aut: automata) {
            FastDFA<String> dfa = new FastDFA<>(new GrowingMapAlphabet<>());
            cifAutomatonToFsa(aut, dfa, allStatesAccepting);
            dfas.put(CifTextUtils.getAbsName(aut, false), dfa);
        }
        return dfas;
    }

    /**
     * Convert the automata of a CIF specification to {@link FastNFA}s.
     * 
     * <p>
     * For details on preconditions and the conversion itself, see {@link #cifAutomatonToFsa}.
     * </p>
     *
     * @param specification CIF specification containing the CIF automata to convert.
     * @param allStatesAccepting Whether to create all NFA states as accepting states ({@code true}), or to use marking
     *     information from the CIF model to determine which states in the NFA should be accepting states
     *     ({@code false}).
     * @throws ConversionPreconditionException If a precondition is not satisfied.
     * @throws IllegalStateException If a CIF automaton with multiple initial states is converted.
     * @return Mapping from absolute names of CIF automata to NFAs.
     */
    public static Map<String, FastNFA<String>> cifSpecificationToFastNfas(Specification specification,
            boolean allStatesAccepting)
    {
        List<Automaton> automata = list();
        CifCollectUtils.collectAutomata(specification, automata);
        Map<String, FastNFA<String>> nfas = mapc(automata.size());
        for (Automaton aut: automata) {
            FastNFA<String> nfa = new FastNFA<>(new GrowingMapAlphabet<>());
            cifAutomatonToFsa(aut, nfa, allStatesAccepting);
            nfas.put(CifTextUtils.getAbsName(aut, false), nfa);
        }
        return nfas;
    }

    /**
     * Convert a CIF automaton to a Finite State Automaton (FSA), e.g. {@link DFA} or {@link NFA}.
     * 
     * <p>
     * Not all CIF specifications are supported. The following preconditions apply:
     * <ul>
     * <li>The CIF automaton must not monitor any events.</li>
     * <li>The CIF automaton must not have edges that refer to events other than the builtin 'tau' event or events
     * declared in concrete components. References to events declared in component instantiation are not supported.</li>
     * <li>The CIF automaton must only have locations with initialization predicates, if the predicates are trivially
     * true or false.</li>
     * <li>The CIF automaton must only have locations with marked predicates, if the predicates are trivially true or
     * false. This precondition does not apply if {@code allStatesAccepting} is {@code true}.</li>
     * <li>The CIF automaton must only have trivially true guards for its edges.</li>
     * <li>The CIF automaton must not have an updates on its edges.</li>
     * </ul>
     * </p>
     *
     * <p>
     * Some other notes regarding the conversion:
     * <ul>
     * <li>The absolute names of CIF events will be used for the FSA input alphabet. For the CIF builtin 'tau' event,
     * "tau" will be used. For an explicitly declared CIF event named 'tau', declared in the root of the CIF
     * specification, name "$tau" is used.</li>
     * <li>To determine the initial states of the FSA, only the initialization predicates in the CIF locations are
     * considered. Initialization predicates in CIF components, and initialization predicates of locations of other CIF
     * automata are ignored.</li>
     * <li>To determine the accepting states of the FSA, only the marked predicates in the CIF locations are considered.
     * Marked predicates in CIF components, and marked predicates of locations of other CIF automata are ignored. Marked
     * predicates are ignored entirely if {@code allStatesAccepting} is {@code true}.</li>
     * <li>Invariants in the CIF specification are ignored.</li>
     * </ul>
     * </p>
     *
     * @param <S> The type of the states of the FSA.
     * @param <T> The type of the transitions of the FSA.
     * @param <TP> The type of the transition properties of the FSA.
     * @param <A> The type of the FSA.
     * @param cifAutomaton CIF automaton to convert.
     * @param fsa The empty FSA into which to put the conversion result. Is modified in-place.
     * @param allStatesAccepting Whether to create all FSA states as accepting states ({@code true}), or to use marking
     *     information from the CIF model to determine which states in the FSA should be accepting states
     *     ({@code false}).
     * @throws ConversionPreconditionException If a precondition is not satisfied.
     * @throws IllegalStateException If a non-deterministic CIF automaton is converted into an empty deterministic
     *     AutomataLib FSA.
     * @throws IllegalStateException If a CIF automaton with multiple initial states is converted into an AutomataLib
     *     FSA that does not support multiple initial states.
     * @return The modified FSA.
     */
    private static <S, T, TP, A extends MutableAutomaton<S, String, T, Boolean, TP> & SupportsGrowingAlphabet<String>> A
            cifAutomatonToFsa(Automaton cifAutomaton, A fsa, boolean allStatesAccepting)
    {
        // Check no monitors.
        if (cifAutomaton.getMonitors() != null) {
            throw new ConversionPreconditionException(
                    "Monitors are not supported: " + CifTextUtils.getAbsName(cifAutomaton));
        }

        // Check guards and updates.
        for (Location loc: cifAutomaton.getLocations()) {
            for (Edge edge: loc.getEdges()) {
                // Check trivially true guard.
                if (!CifValueUtils.isTriviallyTrue(edge.getGuards(), true, true)) {
                    throw new ConversionPreconditionException(
                            "Edges with guards that are not trivially true are not supported: "
                                    + CifTextUtils.getAbsName(loc));
                }

                // Check no updates.
                if (!edge.getUpdates().isEmpty()) {
                    throw new ConversionPreconditionException(
                            "Edges with updates are not supported: " + CifTextUtils.getAbsName(loc));
                }
            }
        }

        // Add alphabet.
        for (Event event: CifEventUtils.getAlphabet(cifAutomaton)) {
            String eventName = CifTextUtils.getAbsName(event, false);
            if (eventName.equals("tau")) {
                eventName = "$tau";
            }
            fsa.addAlphabetSymbol(eventName);
        }

        // Add locations.
        Map<Location, S> locationToStateMap = new HashMap<>(cifAutomaton.getLocations().size());
        for (Location loc: cifAutomaton.getLocations()) {
            // Initial.
            boolean initial;
            if (loc.getInitials().isEmpty()) {
                initial = false;
            } else {
                boolean initialTrue = CifValueUtils.isTriviallyTrue(loc.getInitials(), true, true);
                boolean initialFalse = CifValueUtils.isTriviallyFalse(loc.getInitials(), true, true);
                if (!initialTrue && !initialFalse) {
                    throw new ConversionPreconditionException(
                            "Only locations that are trivially initial or trivially non-initial are supported: "
                                    + CifTextUtils.getAbsName(loc));
                }
                initial = initialTrue;
            }

            // Accepting.
            boolean accepting;
            if (allStatesAccepting) {
                accepting = true;
            } else if (loc.getMarkeds().isEmpty()) {
                accepting = false;
            } else {
                boolean markedTrue = CifValueUtils.isTriviallyTrue(loc.getMarkeds(), false, true);
                boolean markedFalse = CifValueUtils.isTriviallyFalse(loc.getMarkeds(), false, true);
                if (!markedTrue && !markedFalse) {
                    throw new ConversionPreconditionException(
                            "Only locations that are trivially marked or trivially non-marked are supported: "
                                    + CifTextUtils.getAbsName(loc));
                }
                accepting = markedTrue;
            }

            // Add state.
            S state;
            if (initial) {
                state = fsa.addInitialState(accepting);
            } else {
                state = fsa.addState(accepting);
            }
            locationToStateMap.put(loc, state);
        }

        // Add edges.
        for (Location sourceLoc: cifAutomaton.getLocations()) {
            for (Edge edge: sourceLoc.getEdges()) {
                Location targetLoc = CifEdgeUtils.getTarget(edge);

                S sourceState = locationToStateMap.get(sourceLoc);
                S targetState = locationToStateMap.get(targetLoc);
                Preconditions.checkNotNull(sourceState);
                Preconditions.checkNotNull(targetState);

                if (edge.getEvents().isEmpty()) {
                    fsa.addAlphabetSymbol("tau");
                    fsa.addTransition(sourceState, "tau", targetState, null);
                } else {
                    for (EdgeEvent edgeEvent: edge.getEvents()) {
                        Expression eventRef = edgeEvent.getEvent();

                        String eventName;
                        if (eventRef instanceof TauExpression) {
                            fsa.addAlphabetSymbol("tau");
                            eventName = "tau";
                        } else {
                            if (!(eventRef instanceof EventExpression)) {
                                throw new ConversionPreconditionException(
                                        "Only references to the 'tau' event and event references to events declared "
                                                + "in concrete components are supported. "
                                                + "Other event references are not supported: " + eventRef);
                            }

                            Event event = ((EventExpression)eventRef).getEvent();
                            eventName = CifTextUtils.getAbsName(event, false);
                            if (eventName.equals("tau")) {
                                eventName = "$tau";
                            }
                        }

                        fsa.addTransition(sourceState, eventName, targetState, null);
                    }
                }
            }
        }

        // Return the FSA.
        return fsa;
    }
}
