/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2024 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////
package nl.tno.mids.cif.extensions

import com.google.common.collect.MultimapBuilder
import com.google.common.collect.SetMultimap
import java.util.HashMap
import java.util.LinkedHashSet
import java.util.Map
import org.eclipse.emf.common.util.ECollections
import org.eclipse.escet.cif.common.CifCollectUtils
import org.eclipse.escet.cif.common.CifEdgeUtils
import org.eclipse.escet.cif.common.CifEventUtils
import org.eclipse.escet.cif.common.CifLocationUtils
import org.eclipse.escet.cif.common.CifTextUtils
import org.eclipse.escet.cif.metamodel.cif.Specification
import org.eclipse.escet.cif.metamodel.cif.automata.Automaton
import org.eclipse.escet.cif.metamodel.cif.automata.Edge
import org.eclipse.escet.cif.metamodel.cif.automata.Location
import org.eclipse.escet.cif.metamodel.cif.declarations.DiscVariable
import org.eclipse.escet.cif.metamodel.cif.declarations.Event
import org.eclipse.escet.cif.metamodel.java.CifConstructors
import org.eclipse.escet.common.java.Lists
import org.eclipse.xtend.lib.annotations.Data

import static org.eclipse.escet.cif.metamodel.java.CifConstructors.*

import static extension nl.tno.mids.cif.extensions.EdgeExtensions.*
import static extension nl.tno.mids.cif.extensions.LocationExtensions.*

class AutomatonExtensions {
    private new() {
        // static class
    }

    /**
     * Returns the single initial location of this automaton. Throws an exception if multiple exist. Only considers
     * locations to be trivially initial (i.e. not dependent on some condition which is not trivially true). 
     */
    def static initialLocation(Automaton aut) {
        val set = aut.initialLocations
        if (set.size == 1) {
            return set.get(0)
        } else if (set.empty) {
            throw new RuntimeException("Requested single initial location, while non exists.")
        } else {
            throw new RuntimeException(
                "Requested single initial location, while multiple exist. Namely " + set.join("", ", ", ".", [
                    CifLocationUtils.getName(it)
                ]) + " Check whether automaton " + aut.name + " is deterministic")
        }
    }

    /**
     * Returns the set of initial locations of this automaton. Only considers locations to be trivially initial 
     * (i.e. not dependent on some condition which is not trivially true). 
     */
    def static initialLocations(Automaton aut) {
        return aut.locations.filter[initialLocation].toSet
    }

    /** 
     * Change the names of locations in the automaton to loc1, loc2, loc3, ..., in the order they are contained 
     * in the automaton.
     */
    def static renumberLocations(Automaton aut) {
        aut.locations.forEach [ loc, index |
            loc.name = 'loc' + (index + 1)
        ]
    }

    /**
     * Ensures that the single initial location in {@code automaton} is also the first location occurring in
     * {@code automaton.getLocations()}. Also applies location renumbering to get consistent location names.
     * 
     * @param automaton The input automaton.
     */
    def static ensureInitialLocationIsFirstLocation(Automaton automaton) {
        val initialState = initialLocation(automaton)
        if (automaton.locations.head !== initialState) {
            automaton.locations.remove(initialState)
            automaton.locations.add(0, initialState)
        }

        renumberLocations(automaton)
    }

    /**
     * Re-orders the locations in the {@code automaton}, starting from each initial location in depth-first order.
     * Additionally, for each location, outgoing edges are sorted based on event name. Any locations unreachable from 
     * any initial location are removed. If there are no initial locations, all locations are replaced by a single 
     * non-initial, unmarked location with no outgoing edges. Also applies location renumbering to get consistent location
     * names.
     * 
     * <p>
     * If the automaton has a single initial location and no locations with multiple edges with the same event, the 
     * result will be fully normalized. If any location contains multiple edges with the same event, those will have the 
     * same order in the output as in the input. If there are multiple initial locations, they are initially sorted 
     * based on their name, and then renamed afterwards.
     * </p>
     * 
     * @param automaton The input automaton.
     */
    def static normalizeLocations(Automaton automaton) {
        val initialLocations = initialLocations(automaton)

        if (!initialLocations.empty) {
            val visitedLocations = new LinkedHashSet
            val stackContents = newHashSet
            automaton.locations.clear
            val stack = newLinkedList
            stack.addAll(initialLocations.sortBy[name])
            stackContents.addAll(stack)
            while (!stack.empty) {
                val nextLoc = stack.pop
                stackContents.remove(nextLoc)
                visitedLocations.add(nextLoc)
                ECollections.sort(nextLoc.edges, [l, r|compareEdges(l, r)])
                for (edge : nextLoc.edges.reverseView) {
                    val newLoc = CifEdgeUtils.getTarget(edge)
                    if (!visitedLocations.contains(newLoc) && !stackContents.contains(newLoc)) {
                        stack.push(newLoc)
                        stackContents.add(newLoc)
                    }
                }
            }
            automaton.locations.addAll(visitedLocations)
        } else {
            automaton.locations.clear
            automaton.locations.add(CifConstructors.newLocation)
        }

        renumberLocations(automaton)
    }

    /**
     * Compares two edges based on the absolute non-escaped names of their associated events. Edges with tau events
     * (explicitly or implicitly) are considered 'smaller' than ones with non-tau events.
     * 
     * <p>This method does not support multiple events on a single edge.</p>
     * 
     * @param left Left edge to compare.
     * @param right Right edge to compare.
     * @return A negative integer if {@code left} precedes {@code right}, zero if they are equal, and a positive integer
     *         if {@code right} precedes {@code left}.
     */
    private def static int compareEdges(Edge left, Edge right) {
        val eventLeft = left.getEventDecl(true)
        val eventRight = right.getEventDecl(true)

        // Tau is equal to itself.
        if (eventLeft === null && eventRight === null) {
            return 0;
        }

        // Tau before other events.
        if (eventLeft === null) {
            return -1;
        }
        if (eventRight === null) {
            return 1;
        }

        // Non-tau events ordered based on name.
        return CifTextUtils.getAbsName(eventLeft, false).compareTo(CifTextUtils.getAbsName(eventRight, false))
    }

    /** Retrieve all edges of a given automaton. */
    def static getAllEdges(Automaton automaton) {
        automaton.locations.flatMap[loc|loc.edges].toSet
    }

    /**
     * Generates a new alphabet and stores this in the automaton. Does not consider channel communication!
     * Note that this procedure generates a new alphabet, with new event expressions. 
     */
    def static void updateAlphabet(Automaton automaton) {
        automaton.removeAlphabet // clear old alphabet to ensure that next call generates a new one. 
        val eventAlphabet = CifEventUtils.getAlphabet(automaton)
        automaton.alphabet = newAlphabet()
        eventAlphabet.forEach [ evt |
            automaton.alphabet.events.add(newEventExpression => [event = evt; type = newBoolType])
        ]
    }

    /** 
     * Removes the alphabet from the automaton.
     */
    private def static void removeAlphabet(Automaton automaton) {
        automaton.alphabet = null
    }

    /**
     * Remove unreachable locations, i.e. locations not reachable from an initial location.
     * 
     * <p>Does not take guards into account when computing reachability, which may result in over-approximation of reachable 
     * locations. Thus, the result may contain locations that according to the full semantics are unreachable.</p>
     */
    def static void removeUnreachableLocations(Automaton automaton) {
        val reachableLocations = newHashSet
        for (initialLocation : automaton.initialLocations) {
            reachableLocations.addAll(getReachableLocations(initialLocation))
        }
        automaton.locations.removeIf[!reachableLocations.contains(it)]
    }

    /** 
     * Construct a multi-map linking locations to incoming edges.
     * 
     * <p>The resulting multi-map does not explicitly include locations with no incoming edges. For those locations,
     * the {@link SetMultimap#get} method will return an empty set rather than {@code null}.</p>
     */
    def static SetMultimap<Location, Edge> getIncomingEdgeMap(Automaton automaton) {
        val resultMap = MultimapBuilder.linkedHashKeys().linkedHashSetValues.build

        automaton.getAllEdges().forEach[edge|resultMap.put(EdgeExtensions.getDestination(edge), edge)]

        return resultMap
    }

    /**
     * Does the given model contain edges with data references?
     * 
     * @param model The model.
     * @return {@code true} if the model contains at least one automaton with at least one edge containing an update or
     *      a guard referencing data, {@code false} otherwise.
     */
    static def hasData(Specification model) {
        val automata = Lists.list
        CifCollectUtils.collectAutomata(model, automata)
        automata.exists[hasData]
    }

    /**
     * Does the given CIF automaton contain edges with data references?
     * 
     * @param automaton The CIF automaton.
     * @return {@code true} if the automaton contains at least one edge containing an update or a guard referencing
     *      data, {@code false} otherwise.
     */
    private static def hasData(Automaton automaton) {
        return automaton.locations.exists[edges.exists[hasData]]
    }

    /**
     * Does the given edge reference data?
     * 
     * @param edge The edge.
     * @return {@code true} if the edge has an update or a guard referencing data, {@code false} otherwise.
     */
    private static def hasData(Edge edge) {
        return !edge.updates.empty || edge.guards.exists[ExpressionExtensions.getReferencedDiscVars(it).size > 0]
    }

    /**
     * Retrieve the set of {@link DiscVariable} variables referenced from any edge in the given automaton.
     */
    static def getReferencedDiscVars(Automaton automaton) {
        return automaton.locations.flatMap[edges.flatMap[EdgeExtensions.getReferencedDiscVars(it)]].toSet
    }

    /**
     * Does the given model contain 'tau' edges?
     * 
     * @param model The model.
     * @return {@code true} if the model contains at least one automaton with at least one 'tau' edge, {@code false}
     *      otherwise.
     */
    static def hasTau(Specification model) {
        val automata = Lists.list
        CifCollectUtils.collectAutomata(model, automata)
        automata.exists[hasTau]
    }

    /**
     * Does the given automaton contain 'tau' edges?
     * 
     * @param automaton The CIF automaton.
     * @return {@code true} if the automaton contains at least one tau edge, {@code false} otherwise.
     */
    private static def hasTau(Automaton automaton) {
        return automaton.locations.exists[edges.exists[edge|EdgeExtensions.getEventDecls(edge, true).contains(null)]]
    }
}

@Data class Signature {
    Map<Event, Location> outgoingTransitions
    boolean accepts

    new(Location q) {
        outgoingTransitions = new HashMap()
        for (edge : q.edges) {
            val oldTrans = outgoingTransitions.put(edge.getEventDecl(true), edge.destination)
            if (oldTrans !== null) {
                throw new NonDeterministicChoiceException(q, edge.getEventDecl(true))
            }
        }
        accepts = if(q.edges.empty) true else false
    }
}
