/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2024 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////
package nl.tno.mids.cmi.api.general

import java.util.HashMap
import java.util.List
import nl.tno.mids.cif.extensions.EdgeExtensions
import nl.tno.mids.cmi.api.info.EventFunctionExecutionSide
import nl.tno.mids.cmi.api.info.EventInfo
import nl.tno.mids.pps.extensions.info.EventFunctionExecutionType
import org.eclipse.escet.cif.common.CifCollectUtils
import org.eclipse.escet.cif.metamodel.cif.Group
import org.eclipse.escet.cif.metamodel.cif.automata.Automaton
import org.eclipse.escet.cif.metamodel.cif.automata.Edge
import org.eclipse.escet.cif.metamodel.cif.declarations.Event

class CmiGeneralAsyncPatternQueries {
    /**
     * Is the given edge the start of an asynchronous pattern:
     * 
     * <ul>
     * <li>Asynchronous handler (only if receiving)</li>
     * <li>FCN call (only if sending)</li>
     * <li>Request call of request/wait pattern (only if sending)</li>
     * </ul>
     * 
     * @param edge The possible start edge.
     * @return {@code true} if the event is the start of an asynchronous pattern, {@code false} otherwise.
     */
    static def isAsyncPatternStart(Edge edge) {
        val event = EdgeExtensions.getEventDecl(edge, true)

        if (event === null) {
            return false
        }

        val eventInfo = CmiGeneralEventQueries.getEventInfo(event)
        if (isReceivingEdge(edge)) {
            // As a receiving edge, it must be an asynchronous handler.
            return eventInfo.otherType == EventFunctionExecutionType.ASYNCHRONOUS_HANDLER &&
                eventInfo.otherSide == EventFunctionExecutionSide.START
        } else if (isSendingEdge(edge)) {
            // As a sending edge, it must be either an FCN call or a request call. 
            return (eventInfo.declType == EventFunctionExecutionType.FCN_CALL &&
                eventInfo.declSide == EventFunctionExecutionSide.START) ||
                (eventInfo.declType == EventFunctionExecutionType.REQUEST_CALL &&
                    eventInfo.declSide == EventFunctionExecutionSide.START)
        } else {
            // Otherwise, it must be an internal edge, which is never part of an asynchronous pattern.
            return false
        }
    }

    /**
     * Is the given edge the end of an asynchronous pattern:
     * 
     * <ul>
     * <li>FCN callback</li>
     * <li>Wait call of request/wait pattern</li>
     * <li>Asynchronous reply for an asynchronous handler</li>
     * </ul>
     * 
     * @param edge The possible end edge.
     * @return {@code true} if the edge is the end of an asynchronous pattern, {@code false} otherwise.
     */
    static def isAsyncPatternEnd(Edge edge) {
        val event = EdgeExtensions.getEventDecl(edge, true)

        if (event === null) {
            return false
        }

        val eventInfo = CmiGeneralEventQueries.getEventInfo(event)
        return (eventInfo.otherType == EventFunctionExecutionType.FCN_CALLBACK &&
            eventInfo.otherSide == EventFunctionExecutionSide.START) ||
            (eventInfo.declType == EventFunctionExecutionType.WAIT_CALL &&
                eventInfo.declSide == EventFunctionExecutionSide.START) ||
            (eventInfo.declType == EventFunctionExecutionType.ASYNCHRONOUS_RESULT &&
                eventInfo.declType == EventFunctionExecutionSide.START)
    }

    /**
     * Do the two given edges form an asynchronous pattern?
     * 
     * @param cache Cache of event info for events.
     * @param startEdge The potential start edge of the pattern.
     * @param endEdge The potential end edge of the pattern.
     * @return {@code true} if the two given edges form an asynchronous pattern, {@code false} otherwise.
     */
    static def isAsyncPatternPair(HashMap<Event, EventInfo> cache, Edge startEdge, Edge endEdge) {
        val startEvent = EdgeExtensions.getEventDecl(startEdge, true)
        val endEvent = EdgeExtensions.getEventDecl(endEdge, true)

        if (startEvent === null || endEvent === null) {
            return false
        }

        val startInfo = cache.computeIfAbsent(startEvent, [event|CmiGeneralEventQueries.getEventInfo(event)])
        val endInfo = cache.computeIfAbsent(endEvent, [event|CmiGeneralEventQueries.getEventInfo(event)])
        val isReceiving = isReceivingEdge(startEdge)
        val isSending = isSendingEdge(startEdge)

        // A pattern must concern the same function.
        if (startInfo.interfaceName != endInfo.interfaceName) {
            return false
        }
        if (startInfo.functionName != endInfo.functionName) {
            return false
        }

        // Check pattern: FCN call/callback (only if sending).
        if (startInfo.declCompInfo == endInfo.otherCompInfo &&
            startInfo.declType == EventFunctionExecutionType.FCN_CALL &&
            startInfo.declSide == EventFunctionExecutionSide.START && isSending &&
            endInfo.otherType == EventFunctionExecutionType.FCN_CALLBACK &&
            endInfo.otherSide == EventFunctionExecutionSide.START) {
            return true
        }

        // Check pattern: request/wait calls (only if sending).
        if (startInfo.declCompInfo == endInfo.declCompInfo &&
            startInfo.declType == EventFunctionExecutionType.REQUEST_CALL &&
            startInfo.declSide == EventFunctionExecutionSide.START && isSending &&
            endInfo.declType == EventFunctionExecutionType.WAIT_CALL &&
            endInfo.declSide == EventFunctionExecutionSide.START) {
            return true
        }

        // Check pattern: asynchronous handler/reply (only if receiving).
        if (startInfo.otherCompInfo == endInfo.declCompInfo &&
            startInfo.otherType == EventFunctionExecutionType.ASYNCHRONOUS_HANDLER &&
            startInfo.otherSide == EventFunctionExecutionSide.START && isReceiving &&
            endInfo.declType == EventFunctionExecutionType.ASYNCHRONOUS_RESULT &&
            endInfo.declSide == EventFunctionExecutionSide.START) {
            return true
        }

        // Not a pattern pair.
        return false
    }

    /**
     * Collect all other asynchronous pattern start edges that form a valid asynchronous pattern with the given edge.
     * 
     * @param edge The potential asynchronous pattern end edge.
     * @return All other asynchronous pattern start edges, if the given edge is an asynchronous pattern end edge, or
     *      no edges otherwise.
     */
    static def getAsyncPatternStarts(Edge edge) {
        if (isAsyncPatternEnd(edge)) {
            return getAsyncPatternOtherEdges(edge)
        }
        return newHashSet
    }

    /**
     * Collect all other asynchronous pattern end edges that form a valid asynchronous pattern with the given edge.
     * 
     * @param edge The potential asynchronous pattern start edge.
     * @return All other asynchronous pattern end edges, if the given edge is an asynchronous pattern start edge, or
     *      no edges otherwise.
     */
    static def getAsyncPatternEnds(Edge edge) {
        if (isAsyncPatternStart(edge)) {
            return getAsyncPatternOtherEdges(edge)
        }
        return newHashSet
    }

    /**
     * Collect all other asynchronous pattern edges that form a valid asynchronous pattern with the given edge.
     * 
     * @param edge The potential asynchronous pattern edge.
     * @return All other asynchronous pattern edges with which the given edge forms a pattern, if the given edge is an
     *      asynchronous pattern edge, or no edges otherwise. Note that two edges can only form a pattern if they are 
     *      part of the same component.
     */
    private static def getAsyncPatternOtherEdges(Edge edge) {
        val result = newLinkedHashSet

        // Get event info for the edge. No other edges for 'tau' events.
        val event = EdgeExtensions.getEventDecl(edge, true)
        if (event === null) {
            return result
        }

        // Get all automata in the same component.
        val component = CmiGeneralComponentQueries.getComponent(edge)
        val List<Automaton> automata = newArrayList
        if (component instanceof Group) { // 'Split' subset.
            CifCollectUtils.collectAutomata(component, automata)
        } else { // 'Basic' subset.
            automata.add(component as Automaton)
        }

        val cache = newHashMap
        // Consider all other edges in this component.
        automata.forEach [ automaton |
            automaton.locations.forEach [ location |
                location.edges.forEach [ otherEdge |
                    // Check for pattern match. Never a match for 'tau' events.
                    val otherEvent = EdgeExtensions.getEventDecl(otherEdge, true)
                    if (otherEvent !== null) {
                        if (isAsyncPatternPair(cache, edge, otherEdge) || isAsyncPatternPair(cache, otherEdge, edge)) {
                            result += otherEdge
                        }
                    }
                ]
            ]
        ]
        return result
    }

    private static def isReceivingEdge(Edge edge) {
        val edgeComponentInfo = CmiGeneralComponentQueries.getComponentInfo(
            CmiGeneralComponentQueries.getComponent(edge))
        val event = EdgeExtensions.getEventDecl(edge, true)

        if (event === null) {
            return false
        }

        val eventInfo = CmiGeneralEventQueries.getEventInfo(event)
        return edgeComponentInfo == eventInfo.otherCompInfo
    }

    private static def isSendingEdge(Edge edge) {
        val edgeComponentInfo = CmiGeneralComponentQueries.getComponentInfo(
            CmiGeneralComponentQueries.getComponent(edge))
        val event = EdgeExtensions.getEventDecl(edge, true)

        if (event === null) {
            return false
        }

        val eventInfo = CmiGeneralEventQueries.getEventInfo(event)
        return edgeComponentInfo == eventInfo.declCompInfo && eventInfo.otherCompInfo !== null
    }
}
