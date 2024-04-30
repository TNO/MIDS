/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2024 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////
package nl.tno.mids.cmi.api.basic

import java.util.Deque
import java.util.List
import java.util.Set
import java.util.stream.Collectors
import nl.tno.mids.cif.extensions.AutomatonExtensions
import nl.tno.mids.cif.extensions.EdgeExtensions
import nl.tno.mids.cif.extensions.LocationExtensions
import nl.tno.mids.cmi.api.general.CmiGeneralComponentQueries
import nl.tno.mids.cmi.api.general.CmiGeneralEventQueries
import nl.tno.mids.cmi.api.general.CmiGeneralQueries
import nl.tno.mids.cmi.api.general.CmiSubset
import nl.tno.mids.cmi.api.protocol.CmiProtocolQueries
import org.eclipse.escet.cif.common.CifCollectUtils
import org.eclipse.escet.cif.common.CifEdgeUtils
import org.eclipse.escet.cif.metamodel.cif.Specification
import org.eclipse.escet.cif.metamodel.cif.automata.Automaton
import org.eclipse.escet.cif.metamodel.cif.automata.Edge
import org.eclipse.escet.cif.metamodel.cif.declarations.Event
import org.eclipse.escet.common.java.Assert
import org.eclipse.escet.common.java.Sets

class CmiBasicServiceFragmentQueries {
    /**
     * Is the given model within the basic subset, i.e., without split service fragments and not a protocol?
     * 
     * <p>This check is implemented by checking that all CIF automata match components with behavior, rather than
     * being service fragment automata, and do not have protocol names.</p>
     * 
     * @return {@code true} if the model is within the basic subset without split service fragments, {@code false}
     *      otherwise.
     * @note Use {@link CmiGeneralQueries#detectSubset} instead, for additional robustness.
     */
    static def boolean isBasicCmiModelWithNoSplitServiceFragments(Specification model) {
        // Check that all automata match components with behavior.
        val List<Automaton> automata = newArrayList
        CifCollectUtils.collectAutomata(model, automata)
        val automataSet = Sets.list2set(automata)
        val componentsWithBehavior = Sets.list2set(CmiBasicComponentQueries.getComponentsWithBehavior(model))
        if (!automataSet.equals(componentsWithBehavior) || CmiProtocolQueries.isProtocolCmiModel(model)) {
            return false
        }

        // Is 'basic' model.
        return true
    }

    /**
     * Does the given model contain only components that can be split into separate service fragments?
     * 
     * @param model The model.
     * @return {@code true} if the model contains only components that can be split, {@code false} otherwise.
     */
    static def boolean canBeSplitIntoServiceFragments(Specification model) {
        // Protocol models cannot be split into service fragments.
        if (CmiGeneralQueries.detectSubset(model) == CmiSubset.PROTOCOL) {
            return false
        }

        // If no behavior components are present, there is nothing to split.
        // E.g. if the split was already done, it can't be done again.
        if (CmiBasicComponentQueries.getComponentsWithBehavior(model).isEmpty) {
            return false
        }

        // Check that all automata match components with behavior, and not already split service fragments.
        if (!isBasicCmiModelWithNoSplitServiceFragments(model)) {
            return false;
        }

        // Find out whether all components in the model can be split into separate service fragment automata.
        val componentsWithBehavior = CmiBasicComponentQueries.getComponentsWithBehavior(model)
        return componentsWithBehavior.forall[canBeSplitIntoServiceFragments]
    }

    /**
     * Can the given component be split into separate service fragments?
     * 
     * @param component The component with behavior.
     * @return {@code true} if the automaton can be split, {@code false} otherwise.
     */
    private static def boolean canBeSplitIntoServiceFragments(Automaton component) {
        // Can only be split if one unique edge (no duplicate edges) to start a service fragment.
        val initialEvents = getServiceFragmentInitialEvents(component)
        if (initialEvents.size != initialEvents.toSet.size) {
            return false
        }

        // The non-tau event names of all edges must comply to the API event naming scheme.
        val allEdges = AutomatonExtensions.getAllEdges(component)
        val allEvents = allEdges.stream.map[edge|EdgeExtensions.getEventDecl(edge, true)].filter [ event |
            event !== null
        ].collect(Collectors.toSet)
        if (allEvents.exists[!CmiGeneralEventQueries.hasValidEventName(it)]) {
            return false
        }

        // From all locations in service fragments, it must be able to reach the initial location again.
        val initialLocation = AutomatonExtensions.initialLocation(component)
        if (!LocationExtensions.getCoReachableLocations(initialLocation).equals(
            LocationExtensions.getReachableLocations(initialLocation))) {
            return false;
        }

        return true
    }

    /**
     * Get the initial events of service fragments in the given component.
     */
    static def getServiceFragmentInitialEvents(Automaton component) {
        return getServiceFragmentInitialEdges(component).map[EdgeExtensions.getEventDecl(it, true)]
    }

    /**
     * Get the initial edges of service fragments in the given component.
     */
    static def getServiceFragmentInitialEdges(Automaton component) {
        return AutomatonExtensions.initialLocation(component).edges
    }

    /**
     * Get the edges that comprise a service fragment in the given component, based on the initial event of the service
     * fragment.
     * 
     * @param component The component containing the service fragment.
     * @param serviceFragmentInitialEvent Initial event of the service fragment.
     * @return The set of edges that together comprise the requested service fragment.
     * 
     * @note The results for different service fragments are not guaranteed to be disjoint.
     * @note The component automaton must contain an outgoing edge with the given event in its initial location.
     * @note The initial edge of the service fragment is guaranteed to be the first element of the resulting set.
     */
    static def Set<Edge> getServiceFragmentEdges(Automaton component, Event serviceFragmentInitialEvent) {
        val initialLocation = AutomatonExtensions.initialLocation(component)
        val initialEdge = LocationExtensions.getEdge(initialLocation, serviceFragmentInitialEvent)

        Assert.notNull(initialEdge,
            "Component " + CmiGeneralComponentQueries.getComponentName(component) +
                " does not contain a service fragment starting with " +
                CmiGeneralEventQueries.getEventName(serviceFragmentInitialEvent) + ".")
        return getServiceFragmentEdges(component, initialEdge)
    }

    /**
     * Get the edges that comprise a service fragment in the given component, based on the initial edge of the service
     * fragment.
     * 
     * @param component The component containing the service fragment.
     * @param serviceFragmentInitialEdge Initial edge of the service fragment.
     * @return The set of edges that together comprise the requested service fragment.
     * 
     * @note The results for different service fragments are not guaranteed to be disjoint.
     * @note The component automaton must contain an outgoing edge with the given event in its initial location.
     * @note The initial edge of the service fragment is guaranteed to be the first element of the resulting set.
     */
    static def Set<Edge> getServiceFragmentEdges(Automaton component, Edge initialEdge) {
        val initialLocation = AutomatonExtensions.initialLocation(component)
        Assert.check(CifEdgeUtils.getSource(initialEdge) == initialLocation,
            "Initial service fragment edge does not belong to component" +
                CmiGeneralComponentQueries.getComponentName(component) + ".")

        val fragmentEdges = newLinkedHashSet(initialEdge)
        val Deque<Edge> edgeQueue = newLinkedList(initialEdge)
        while (!edgeQueue.isEmpty) {
            val edge = edgeQueue.pop
            val edgeTarget = CifEdgeUtils.getTarget(edge)
            if (edgeTarget !== initialLocation) {
                // Avoid revisiting any edges.
                edgeTarget.edges.filter[!fragmentEdges.contains(it)].forEach [
                    fragmentEdges.add(it)
                    edgeQueue.add(it)
                ]
            }
        }

        return fragmentEdges
    }
}
