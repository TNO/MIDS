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

import static org.eclipse.escet.common.java.Lists.listc;
import static org.eclipse.escet.common.java.Lists.set2list;
import static org.eclipse.escet.common.java.Maps.mapc;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.escet.cif.common.CifScopeUtils;
import org.eclipse.escet.cif.common.CifTextUtils;
import org.eclipse.escet.cif.common.CifValidationUtils;
import org.eclipse.escet.cif.common.CifValueUtils;
import org.eclipse.escet.cif.metamodel.cif.ComplexComponent;
import org.eclipse.escet.cif.metamodel.cif.Group;
import org.eclipse.escet.cif.metamodel.cif.Specification;
import org.eclipse.escet.cif.metamodel.cif.automata.Automaton;
import org.eclipse.escet.cif.metamodel.cif.automata.Edge;
import org.eclipse.escet.cif.metamodel.cif.automata.EdgeEvent;
import org.eclipse.escet.cif.metamodel.cif.automata.Location;
import org.eclipse.escet.cif.metamodel.cif.declarations.Event;
import org.eclipse.escet.cif.metamodel.cif.expressions.Expression;
import org.eclipse.escet.cif.metamodel.java.CifConstructors;
import org.eclipse.escet.common.java.Lists;

import com.google.common.base.Preconditions;

import net.automatalib.automata.UniversalFiniteAlphabetAutomaton;
import net.automatalib.automata.concepts.StateIDs;
import net.automatalib.automata.concepts.StateLocalInput;
import net.automatalib.automata.fsa.DFA;
import net.automatalib.automata.fsa.NFA;

/**
 * Utility methods for converting AutomataLib automata to CIF.
 */
public class AutomataLibToCif {
    /**
     * Convert a Finite State Automaton (FSA), e.g. {@link DFA} or {@link NFA}, to a CIF specification with a single CIF
     * automaton.
     *
     * <p>
     * For details on preconditions and the conversion itself, see {@link #fsasToCifSpecification}.
     * </p>
     * 
     * @param <S> The type of the states of the FSA.
     * @param <T> The type of the transitions of the FSA.
     * @param <TP> The type of the transition properties of the FSA.
     * @param <A> The type of the FSA.
     * @param fsa The FSA to convert.
     * @param absAutName The absolute name of the single CIF automaton to create.
     * @param addMarking Whether to add marked predicates to CIF locations for accepting FSA states ({@code true}) or
     *     not add any marked predicates ({@code false}).
     * @throws ConversionPreconditionException If a precondition is not satisfied.
     * @return The CIF specification.
     */
    public static <S, T, TP,
            A extends UniversalFiniteAlphabetAutomaton<S, String, T, Boolean, TP> & StateLocalInput<S, String> & StateIDs<S>>
            Specification fsaToCifSpecification(A fsa, String absAutName, boolean addMarking)
    {
        Map<String, A> fsas = mapc(1);
        fsas.put(absAutName, fsa);
        return fsasToCifSpecification(fsas, addMarking);
    }

    /**
     * Convert Finite State Automata (FSAs), e.g. {@link DFA}s or {@link NFA}s, to a CIF specification with a CIF
     * automaton per FSA.
     *
     * <p>
     * The following preconditions apply:
     * <ul>
     * <li>The given absolute automata names must be a valid absolute CIF automata names.</li>
     * <li>The inputs from the FSAs input alphabets must be valid absolute CIF event names.</li>
     * <li>For each FSA input from the FSAs input alphabets, any ancestor that is the not the direct parent, must not be
     * one of the automaton to create. This prevents that the direct parent group in which the event is to be declared,
     * can't be placed, as automata can't contain other components.</li>
     * <li>For each FSA, the automaton to create must not be a descendant of an automaton to create for one of the other
     * FSAs.</li>
     * <li>If {@code addMarking} is {@code false}, all FSA states must be accepting states.</li>
     * </ul>
     * </p>
     *
     * <p>
     * Some other notes regarding the conversion:
     * <ul>
     * <li>The inputs from the input alphabets of the FSAs are assumed to be the absolute names of the CIF events. If
     * their parent is the absolute name of a CIF automaton to create, they will be created in that CIF automaton.
     * Otherwise, they will be declared in groups.</li>
     * <li>Locations will be named 'loc1', 'loc2', 'loc3', etc. If such names conflict with event names, underscores are
     * appended until the names are unique, e.g. 'loc2_', 'loc2__', 'loc2___', etc.</li>
     * <li>FSA input 'tau' is considered to be the CIF builtin 'tau' event. FSA input '$tau' is considered to be an
     * explicitly declared event named 'tau' in the root of the CIF specification.</li>
     * <li>If {@code addMarking} is {@code true} marked predicates are added to locations. Such predicates are added for
     * both accepting and non-accepting states. This allows to distinguish models with marking where nothing is marked
     * from models where no marking is generated at all.</li>
     * </ul>
     * </p>
     * 
     * @param <S> The type of the states of the FSAs.
     * @param <T> The type of the transitions of the FSAs.
     * @param <TP> The type of the transition properties of the FSAs.
     * @param <A> The type of the FSAs.
     * @param fsas Mapping from absolute name of CIF automaton to create for an FSAs, to the FSA.
     * @param addMarking Whether to add marked predicates to CIF locations for accepting FSA states ({@code true}) or
     *     not add any marked predicates ({@code false}).
     * @throws ConversionPreconditionException If a precondition is not satisfied.
     * @return The CIF specification.
     */
    public static <S, T, TP,
            A extends UniversalFiniteAlphabetAutomaton<S, String, T, Boolean, TP> & StateLocalInput<S, String> & StateIDs<S>>
            Specification fsasToCifSpecification(Map<String, A> fsas, boolean addMarking)
    {
        // Create CIF specification.
        Map<String, ComplexComponent> componentMap = new HashMap<>();
        Specification specification = CifConstructors.newSpecification();
        specification.setName("specification");
        componentMap.put("", specification);

        // Fill the CIF specification.
        List<Automaton> automata = addAutomata(fsas, specification, componentMap);
        Map<String, Event> eventMap = addEvents(fsas, specification, componentMap);
        addLocationsAndEdges(fsas, automata, eventMap, addMarking);

        // Return CIF specification.
        return specification;
    }

    private static <S, T, TP,
            A extends UniversalFiniteAlphabetAutomaton<S, String, T, Boolean, TP> & StateLocalInput<S, String> & StateIDs<S>>
            List<Automaton>
            addAutomata(Map<String, A> fsas, Specification specification, Map<String, ComplexComponent> componentMap)
    {
        // Check valid absolute automata names.
        for (String absAutName: fsas.keySet()) {
            if (!CifValidationUtils.isValidName(absAutName)) {
                throw new ConversionPreconditionException("Invalid absolute CIF automaton name: " + absAutName);
            }
        }

        // Add groups for ancestors of automata.
        for (String absAutName: fsas.keySet()) {
            List<String> absAutNameParts = Arrays.asList(absAutName.split("\\.", -1));
            Group autParent = specification;
            for (int i = 0; i < absAutNameParts.size() - 1; i++) {
                String absGroupName = String.join(".", absAutNameParts.subList(0, i + 1));
                ComplexComponent component = componentMap.get(absGroupName);
                if (component == null) {
                    Group group = CifConstructors.newGroup();
                    group.setName(absAutNameParts.get(i));
                    autParent.getComponents().add(group);
                    componentMap.put(absGroupName, group);
                    autParent = group;
                } else {
                    autParent = (Group)component;
                }
            }
        }

        // Check that automata are not ancestors of each other.
        for (String absAutName: fsas.keySet()) {
            if (componentMap.containsKey(absAutName)) {
                throw new ConversionPreconditionException(
                        "Automaton '" + absAutName + "' is an ancestor of another automaton.");
            }
        }

        // Add automata.
        List<Automaton> automata = listc(fsas.size());
        for (String absAutName: fsas.keySet()) {
            // Ensure no duplicate absolute automata names.
            if (componentMap.containsKey(absAutName)) {
                throw new ConversionPreconditionException(
                        "Absolute automaton name '" + absAutName + "' is used for multiple FSAs.");
            }

            // Get parent.
            List<String> absAutNameParts = Arrays.asList(absAutName.split("\\.", -1));
            String absParentName = String.join(".", Lists.slice(absAutNameParts, null, -1));
            ComplexComponent parent = componentMap.get(absParentName);
            Preconditions.checkNotNull(parent);
            Preconditions.checkState(parent instanceof Group);
            Group parentGroup = (Group)parent;

            // Add automaton.
            String autName = Lists.last(absAutNameParts);
            Automaton automaton = CifConstructors.newAutomaton();
            automaton.setName(autName);
            parentGroup.getComponents().add(automaton);
            componentMap.put(absAutName, automaton);
            automata.add(automaton);
        }

        // Return automata.
        return automata;
    }

    private static <S, T, TP,
            A extends UniversalFiniteAlphabetAutomaton<S, String, T, Boolean, TP> & StateLocalInput<S, String> & StateIDs<S>>
            Map<String, Event>
            addEvents(Map<String, A> fsas, Specification specification, Map<String, ComplexComponent> componentMap)
    {
        // Collect event names.
        List<String> absEvtNames = fsas.values().stream().flatMap(a -> a.getInputAlphabet().stream()).sorted()
                .distinct().collect(Collectors.toList());

        // Add event declarations.
        Map<String, Event> eventMap = new HashMap<>();
        for (String absEvtName: absEvtNames) {
            // CIF builtin 'tau'.
            if (absEvtName.equals("tau")) {
                continue;
            }

            // CIF event with 'tau' as absolute name.
            String origAbsEvtName = absEvtName;
            if (absEvtName.equals("$tau")) {
                absEvtName = "tau";
            }

            // Check valid absolute event name.
            if (!CifValidationUtils.isValidName(absEvtName)) {
                throw new ConversionPreconditionException(
                        "FSA input is not a valid absolute CIF event name: " + absEvtName);
            }

            // Check events not already a component.
            if (componentMap.containsKey(absEvtName)) {
                ComplexComponent component = componentMap.get(absEvtName);
                if (component instanceof Group) {
                    throw new ConversionPreconditionException("FSA input '" + absEvtName
                            + "' is already a group, due to it being an ancestor of another event or an automaton.");
                } else if (component instanceof Automaton) {
                    throw new ConversionPreconditionException(
                            "FSA input '" + absEvtName + "' can't also be used as absolute automaton name.");
                } else {
                    throw new RuntimeException("Unexpected component: " + component);
                }
            }

            // Get parent for new event.
            List<String> absEvtNameParts = Arrays.asList(absEvtName.split("\\.", -1));
            ComplexComponent evtParent = specification;
            for (int i = 0; i < absEvtNameParts.size() - 1; i++) {
                String absComponentName = String.join(".", absEvtNameParts.subList(0, i + 1));
                ComplexComponent component = componentMap.get(absComponentName);
                if (component != null) {
                    evtParent = component;
                } else {
                    if (evtParent instanceof Automaton) {
                        throw new ConversionPreconditionException("Can't create event declaration '" + absEvtName
                                + "' as '" + absComponentName + "' is an automaton, which can't contain a group.");
                    }

                    Preconditions.checkState(evtParent instanceof Group);
                    Group group = CifConstructors.newGroup();
                    group.setName(absEvtNameParts.get(i));
                    ((Group)evtParent).getComponents().add(group);
                    componentMap.put(absComponentName, group);
                    evtParent = group;
                }
            }

            // Add event.
            String eventName = Lists.last(absEvtNameParts);
            Event event = CifConstructors.newEvent();
            event.setName(eventName);
            evtParent.getDeclarations().add(event);
            eventMap.put(origAbsEvtName, event);
        }

        // Return event map.
        return eventMap;
    }

    private static <S, T, TP,
            A extends UniversalFiniteAlphabetAutomaton<S, String, T, Boolean, TP> & StateLocalInput<S, String> & StateIDs<S>>
            void addLocationsAndEdges(Map<String, A> fsas, List<Automaton> automata, Map<String, Event> eventMap,
                    boolean addMarking)
    {
        // Add locations and edges, for each automaton.
        int entryIdx = 0;
        for (Entry<String, A> entry: fsas.entrySet()) {
            // Get FSA and CIF automaton.
            A fsa = entry.getValue();
            Automaton automaton = automata.get(entryIdx);
            entryIdx++;
            Preconditions.checkState(entry.getKey().equals(CifTextUtils.getAbsName(automaton, false)));

            // Get names declared in automaton.
            Set<String> autScopeNames = CifScopeUtils.getSymbolNamesForScope(automaton, null);

            // Add locations and edges, for the automaton.
            Map<S, Location> locationMap = addLocations(fsa, automaton, autScopeNames, addMarking);
            addEdges(fsa, eventMap, locationMap);
        }
    }

    private static <S, T, TP,
            A extends UniversalFiniteAlphabetAutomaton<S, String, T, Boolean, TP> & StateLocalInput<S, String> & StateIDs<S>>
            Map<S, Location> addLocations(A fsa, Automaton automaton, Set<String> autScopeNames, boolean addMarking)
    {
        // Add locations.
        Set<S> initialStates = fsa.getInitialStates();
        Map<S, Location> locationMap = new HashMap<>();
        int locNr = 0;
        for (S state: fsa.getStates()) {
            // Get location name.
            locNr++;
            String locName = "loc" + locNr;
            while (autScopeNames.contains(locName)) {
                locName += "_";
            }

            // Add location.
            Location loc = CifConstructors.newLocation();
            loc.setName(locName);
            automaton.getLocations().add(loc);
            locationMap.put(state, loc);

            // Initial.
            if (initialStates.contains(state)) {
                loc.getInitials().add(CifValueUtils.makeTrue());
            }

            // Marked.
            boolean accepting = fsa.getStateProperty(state);
            if (addMarking) {
                // Add explicit marking, even if non-accepting.
                // This makes it distinguishable from omitting marked predicates altogether in
                // case of
                // '!addMarking'.
                Expression markedPred = accepting ? CifValueUtils.makeTrue() : CifValueUtils.makeFalse();
                loc.getMarkeds().add(markedPred);
            } else if (!accepting) {
                throw new ConversionPreconditionException(
                        "Since 'addMarking' is 'false', all states must be accepting states: " + state);
            }
        }

        // Return location mapping.
        return locationMap;
    }

    private static <S, T, TP,
            A extends UniversalFiniteAlphabetAutomaton<S, String, T, Boolean, TP> & StateLocalInput<S, String> & StateIDs<S>>
            void addEdges(A fsa, Map<String, Event> eventMap, Map<S, Location> locationMap)
    {
        // Add edges.
        for (S sourceState: fsa.getStates()) {
            Location sourceLoc = locationMap.get(sourceState);
            for (String input: fsa.getLocalInputs(sourceState)) {
                // Get target states in sorted order, for predictable output.
                List<S> targetStates = set2list(fsa.getSuccessors(sourceState, input));
                Collections.sort(targetStates, (s1, s2) -> Integer.compare(fsa.getStateId(s1), fsa.getStateId(s2)));
                for (S targetState: targetStates) {
                    // Add edge.
                    Edge edge = CifConstructors.newEdge();
                    edge.setTarget(locationMap.get(targetState));
                    sourceLoc.getEdges().add(edge);

                    // Add edge event.
                    EdgeEvent edgeEvent = CifConstructors.newEdgeEvent();
                    edge.getEvents().add(edgeEvent);

                    // Add event reference.
                    Expression eventRef;
                    if (input.equals("tau")) {
                        eventRef = CifConstructors.newTauExpression(null, CifConstructors.newBoolType());
                    } else {
                        Event event = eventMap.get(input);
                        Preconditions.checkNotNull(event);
                        eventRef = CifConstructors.newEventExpression(event, null, CifConstructors.newBoolType());
                    }
                    edgeEvent.setEvent(eventRef);
                }
            }
        }
    }
}
