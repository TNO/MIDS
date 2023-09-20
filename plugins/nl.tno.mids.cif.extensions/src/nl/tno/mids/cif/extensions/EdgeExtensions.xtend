/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2023 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////
package nl.tno.mids.cif.extensions

import java.util.Collection
import org.eclipse.escet.cif.common.CifEdgeUtils
import org.eclipse.escet.cif.common.CifEventUtils
import org.eclipse.escet.cif.metamodel.cif.ComplexComponent
import org.eclipse.escet.cif.metamodel.cif.automata.Edge
import org.eclipse.escet.cif.metamodel.cif.automata.EdgeReceive
import org.eclipse.escet.cif.metamodel.cif.automata.EdgeSend
import org.eclipse.escet.cif.metamodel.cif.declarations.Event
import org.eclipse.escet.cif.metamodel.cif.expressions.EventExpression
import org.eclipse.escet.cif.metamodel.cif.expressions.TauExpression

import static extension org.eclipse.escet.cif.common.CifEdgeUtils.*
import static extension nl.tno.mids.cif.extensions.CifExtensions.*
import org.eclipse.escet.cif.common.CifTextUtils
import org.eclipse.escet.cif.metamodel.cif.declarations.DiscVariable

class EdgeExtensions {

    /**
     * Returns the single event on this edge. Throws a exception if multiple events are declared on this edge. 
     * If {@code allowTau} is false, an exception is thrown as well if the edge has no event, or the event is represented 
     * by a TauExpression.
     * 
     * <p>If {@code allowTau} is true, the edge might return null if tau is on the edge. In order to check for multiple 
     * event, tau is considered an event, though it will not be returned as such.</p> 
     */
    def static getEventDecl(Edge edge, boolean allowTau) {
        val list = edge.getEventDecls(allowTau)
        if (list.size == 1) {
            return list.get(0)
        } else {
            // no events is not possible: in such case getEventDecls adds null to represent a tau edge. 
            throw new RuntimeException(
                '''Called getEvent while multiple events exist for edge («edge.source.name»->«edge.destination.name»), namely ''' +
                    list.join("", ", ", ".", [Event e|if(e === null) "tau" else e.name])
            )
        }
    }

    /**
     * Returns the list of all events declared on this edge.
     * 
     * <p>If {@code allowTau} is false, an exception is thrown as well if the edge has no event, or an event is represented 
     * by a TauExpression.</p>
     * 
     * <p>If {@code allowTau} is true, null will be added to the list when no edgeEvent is found, or for each event that is 
     * represented by a TauExpression.</p>
     */
    def static getEventDecls(Edge edge, boolean allowTau) {
        val evts = edge.events.map[event].filter(EventExpression).map[it.event].toList
        if (allowTau) {
            edge.events.map[event].filter(TauExpression).forEach[evts.add(null)]
            if (edge.events.empty) {
                evts.add(null)
            }
        } else {
            if (!edge.events.map[event].filter(TauExpression).empty || edge.events.empty) {
                throw new RuntimeException("Found tau event on edge, while this is not allowed by allowTau")
            }
        }
        return evts
    }

    /**
     * Returns the target location of this Edge
     * 
     * <p>This is a convenience method, to allow for the use of CifLocationUtils::getTarget as extension, and avoid conflict 
     * with Edge::getTarget</p>
     */
    def static getDestination(Edge edge) {
        return CifEdgeUtils.getTarget(edge)
    }

    /**
     * Returns whether the edge is communicating within {@code context}, that is, whether it synchronizes across two
     * automata in {@code context}, or participates in either sending or receiving in channel communication. 
     * Whether there is actually another party participating in this channel communication is not checked. 
     * Assumes alphabets in automata are up to date when present. For more information, see {@link #isSynchronizedIn}.
     * 
     */
    def static boolean isCommunicatingIn(Edge edge, Collection<? extends ComplexComponent> context) {

        for (edgeEvent : edge.events) {
            if ((edgeEvent instanceof EdgeSend) || (edgeEvent instanceof EdgeReceive)) {
                return true
            }
        }
        return edge.isSynchronizedIn(context)
    }

    /**
     * Returns whether the edge has an event that synchronizes with an event in another automaton, that is, whether 
     * there are two automata in {@code context} that have the event in its alphabet. 
     * 
     * <p>This function makes heavy use of automata alphabets, so in order 
     * to speed this up, a call to {@link automaton#updateAlphabet} might be worthwhile, to force explicit storing of
     * alphabets. If an alphabet is present in the automaton, ensure it is up to date. </p>
     */
    private def static boolean isSynchronizedIn(Edge edge, Collection<? extends ComplexComponent> context) {
        return edge.getEventDecls(true).filterNull.filter[isSynchronizedIn(context)].size > 0
    }

    /**
     * Returns whether the the event synchronizes across two automata in {@code context}, that is, whether 
     * there are two automata in {@code context} that have the event in its alphabet. 
     * 
     * <p>This function makes heavy use of automata alphabets, so in order 
     * to speed this up, a call to {@link automaton#updateAlphabet} might be worthwhile, to force explicit storing of
     * alphabets. If an alphabet is present in the automaton, ensure it is up to date. </p>
     */
    private def static boolean isSynchronizedIn(Event event, Collection<? extends ComplexComponent> context) {
        val alphabets = CifEventUtils.getAlphabets(newLinkedList(context.flatMap[allAutomata]))
        return alphabets.filter[exists[CifTextUtils.getAbsName(it, false) == CifTextUtils.getAbsName(event, false)]].
            size > 1
    }
    
    /**
     * Retrieve the set of {@link DiscVariable} variables referenced from a given edge.
     */
    def static getReferencedDiscVars(Edge edge) {
        val referencedVariables = edge.guards.flatMap[ExpressionExtensions.getReferencedDiscVars(it)].toSet
        referencedVariables.addAll(edge.updates.flatMap[ExpressionExtensions.getReferencedDiscVars(it)])
        return referencedVariables
    }

}
