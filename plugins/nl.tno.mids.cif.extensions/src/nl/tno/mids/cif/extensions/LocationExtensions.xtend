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

import java.util.Deque
import java.util.Set
import org.eclipse.escet.cif.common.CifEdgeUtils
import org.eclipse.escet.cif.metamodel.cif.automata.Edge
import org.eclipse.escet.cif.metamodel.cif.automata.Location
import org.eclipse.escet.cif.metamodel.cif.declarations.Event

import static extension org.eclipse.escet.cif.common.CifLocationUtils.*
import static extension org.eclipse.escet.cif.common.CifValueUtils.*

class LocationExtensions {
    private new() {
        // static class
    }

    /**
     * Assuming a deterministic automaton, returns the outgoing edge from location {@code state}
     * with event {@code event}. Result is {@code null} if no such edge exists. For non-deterministic automata,
     * use {@code getEdges} instead. 
     */
    def static Edge getEdge(Location state, Event event) {
        // name is consistent with non-deterministic getEdges
        val edges = state?.getEdges(event)
        if (edges !== null && edges.size > 1)
            throw new NonDeterministicChoiceException(state, event)
        else if(edges !== null && edges.empty) return null
        return edges?.get(0)
    }

    /**
     * Determines whether location is a possible initial location:
     * @param location The location to check for.
     * @return Boolean indicating whether location is possibly an initial location
     */
    def static boolean isInitialLocation(Location location) {
        return location.initials.size > 0 && location.initials.isTriviallyTrue(true, true)
    }

    /**
     * Get the set of locations reachable from a given location.
     * 
     * <p>Does not take guards into account when computing reachability, which may result in over-approximation of reachable 
     * locations. Thus, the result may contain locations that according to the full semantics are unreachable.</p>
     */
    static def Set<Location> getReachableLocations(Location location) {
        val visitedLocations = newLinkedHashSet(location)
        val Deque<Location> locationQueue = newLinkedList(location)

        while (!locationQueue.empty) {
            val currentLoc = locationQueue.pop

            for (edge : currentLoc.edges) {
                val target = CifEdgeUtils.getTarget(edge)
                if (!visitedLocations.contains(target)) {
                    visitedLocations += target
                    locationQueue += target
                }
            }
        }
        return visitedLocations
    }

    /**
     * Get the set of locations co-reachable from a given location, i.e the locations from which the given location is reachable.
     * 
     * <p>Does not take guards into account when computing reachability, which may result in over-approximation of reachable 
     * locations. Thus, the result may contain locations where according to the full semantics the initial state is unreachable.</p>
     */
    static def Set<Location> getCoReachableLocations(Location location) {
        val visitedLocations = newLinkedHashSet(location)
        val Deque<Location> locationQueue = newLinkedList(location)

        val incomingEdges = AutomatonExtensions.getIncomingEdgeMap(location.automaton)

        while (!locationQueue.empty) {
            val currentLoc = locationQueue.pop

            for (edge : incomingEdges.get(currentLoc)) {
                val source = CifEdgeUtils.getSource(edge)
                if (!visitedLocations.contains(source)) {
                    visitedLocations += source
                    locationQueue += source
                }
            }
        }
        return visitedLocations
    }
}

class NonDeterministicChoiceException extends Exception {
    new(Location state, Event event) {
        super(
            "State " + state.name + " in automaton " + state.automaton.name +
                " has multiple outgoing transitions for event " + event.name
        )
    }
}
